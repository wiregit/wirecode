/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package com.limegroup.mojito;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.db.KeyValuePublisher;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.event.FindNodeEvent;
import com.limegroup.mojito.event.FindNodeListener;
import com.limegroup.mojito.event.FindValueEvent;
import com.limegroup.mojito.event.FindValueListener;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.event.StoreEvent;
import com.limegroup.mojito.event.StoreListener;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.io.MessageDispatcherImpl;
import com.limegroup.mojito.manager.BootstrapManager;
import com.limegroup.mojito.manager.FindNodeManager;
import com.limegroup.mojito.manager.FindValueManager;
import com.limegroup.mojito.manager.PingManager;
import com.limegroup.mojito.manager.StoreManager;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.messages.MessageHelper;
import com.limegroup.mojito.routing.RandomBucketRefresher;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.RouteTableImpl;
import com.limegroup.mojito.security.CryptoHelper;
import com.limegroup.mojito.settings.ContextSettings;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.DHTNodeStat;
import com.limegroup.mojito.statistics.DHTStats;
import com.limegroup.mojito.statistics.DatabaseStatisticContainer;
import com.limegroup.mojito.statistics.GlobalLookupStatisticContainer;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;

/**
 * The Context is the heart of Mojito where everything comes 
 * together. 
 */
public class Context {
    
    private static final Log LOG = LogFactory.getLog(Context.class);
    
    private KeyPair masterKeyPair;
    
    private String name;
    private Contact localNode;
    private SocketAddress localAddress;
    private SocketAddress tmpExternalAddress;

    private final Object keyPairLock = new Object();
    private volatile KeyPair keyPair;
    
    private Database database;
    private RouteTable routeTable;
    private MessageDispatcher messageDispatcher;
    private MessageHelper messageHelper;
    private KeyValuePublisher publisher;
    private RandomBucketRefresher bucketRefresher;
    
    private PingManager pingManager;
    private FindNodeManager findNodeManager;
    private FindValueManager findValueManager;
    private StoreManager storeManager;
    private BootstrapManager bootstrapManager;
    
    private boolean running = false;
    
    private DHTStats dhtStats = null;
    
    private NetworkStatisticContainer networkStats;
    private GlobalLookupStatisticContainer globalLookupStats;
    private DatabaseStatisticContainer databaseStats;
    
    private long lastEstimateTime = 0L;
    private int estimatedSize = 0;
    
    private LinkedList<Integer> localSizeHistory = new LinkedList<Integer>();
    private LinkedList<Integer> remoteSizeHistory = new LinkedList<Integer>();
    
    private ThreadFactory threadFactory = new DefaultThreadFactory();
    
    private ScheduledExecutorService scheduledExecutor;
    private ExecutorService contextExecutor;
    
    public Context(String name, Contact localNode, KeyPair keyPair) {
        this.name = name;
        this.localNode = localNode;
        this.keyPair = keyPair;
        
        PublicKey masterKey = null;
        try {
            File file = new File(ContextSettings.MASTER_KEY.getValue());
            if (file.exists() && file.isFile()) {
                masterKey = CryptoHelper.loadMasterKey(file);
            }
        } catch (Exception err) {
            LOG.fatal("Loading the MasterKey failed!", err);
        }
        masterKeyPair = new KeyPair(masterKey, null);
        
        dhtStats = new DHTNodeStat(this);
        
        networkStats = new NetworkStatisticContainer(this);
        globalLookupStats = new GlobalLookupStatisticContainer(this);
        databaseStats = new DatabaseStatisticContainer(this);
        
        database = new Database(this);
        routeTable = new RouteTableImpl(this);
        
        messageDispatcher = new MessageDispatcherImpl(this);
        messageHelper = new MessageHelper(this);
        publisher = new KeyValuePublisher(this);

        bucketRefresher = new RandomBucketRefresher(this);
        
        pingManager = new PingManager(this);
        findNodeManager = new FindNodeManager(this);
        findValueManager = new FindValueManager(this);
        storeManager = new StoreManager(this);
        bootstrapManager = new BootstrapManager(this);
        
        // Add the local to the RouteTable
        localNode.setTimeStamp(Long.MAX_VALUE);
        routeTable.add(localNode);
    }
    
    private void initContextTimer() {
        ThreadFactory factory = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread thread = getThreadFactory().newThread(r);
                thread.setName(getName() + "-ContextTimer");
                thread.setDaemon(true);
                return thread;
            }
        };
        
        scheduledExecutor = Executors.newScheduledThreadPool(1, factory);
    }
    
    private void initContextExecutor() {
        ThreadFactory factory = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread thread = getThreadFactory().newThread(r);
                thread.setName(getName() + "-ContextExecutor");
                thread.setDaemon(true);
                return thread;
            }
        };
        
        contextExecutor = Executors.newCachedThreadPool(factory);
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Installs a custom MessageDispatcher implementation. The
     * passed Class must be a subclass of MessageDispatcher.
     */
    public synchronized void setMessageDispatcher(Class<? extends MessageDispatcher> clazz) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MessageDispatcher while DHT is running");
        }

        if (clazz == null) {
            clazz = MessageDispatcherImpl.class;
        }
        
        try {
            Constructor c = clazz.getConstructor(Context.class);
            messageDispatcher = (MessageDispatcher)c.newInstance(this);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
    
    /**
     * Installs a custom RouteTable implementation. The
     * passed Class must be a subclass of <tt>RouteTable</tt>.
     */
    public synchronized RouteTable setRoutingTable(Class<? extends RouteTable> clazz) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch RouteTable while DHT is running");
        }

        if (clazz == null) {
            clazz = RouteTableImpl.class;
        }
        
        try {
            Constructor c = clazz.getConstructor(Context.class);
            routeTable = (RouteTable)c.newInstance(this);
            routeTable.add(localNode);
            return routeTable;
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
    
    public DHTStats getDHTStats() {
        return dhtStats;
    }
    
    public int getVendor() {
        return ContextSettings.VENDOR.getValue();
    }
    
    public int getVersion() {
        return ContextSettings.VERSION.getValue();
    }
    
    public PublicKey getMasterKey() {
        if (masterKeyPair != null) {
            return masterKeyPair.getPublic();
        }
        return null;
    }
    
    public KeyPair getMasterKeyPair() {
        return masterKeyPair;
    }
    
    public void setMasterKeyPair(KeyPair masterKeyPair) {
        this.masterKeyPair = masterKeyPair;
    }
    
    public void createNewKeyPair() {
        setKeyPair(null);
    }
    
    public void setKeyPair(KeyPair keyPair) {
        synchronized (keyPairLock) {
            if (keyPair == null) {
                this.keyPair = CryptoHelper.createKeyPair();
            } else {
                this.keyPair = keyPair;
            }
        }
    }
    
    public KeyPair getKeyPair() {
        return keyPair;
    }
    
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
    
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
    
    public Contact getLocalNode() {
        return localNode;
    }
    
    public boolean isLocalNode(Contact node) {
        return isLocalNodeID(node.getNodeID());
    }
    
    public boolean isLocalNodeID(KUID nodeId) {
        return nodeId != null && nodeId.equals(localNode.getNodeID());
    }
    
    public boolean isLocalAddress(SocketAddress address) {
        return getSocketAddress().equals(address);
    }
    
    public KUID getLocalNodeID() {
        return localNode.getNodeID();
    }
    
    public SocketAddress getSocketAddress() {
        return localNode.getSocketAddress();
    }
    
    public SocketAddress getLocalSocketAddress() {
        return localAddress;
    }
    
    public void setExternalSocketAddress(SocketAddress newExternalAddress)
            throws IOException {
        if (newExternalAddress != null) {
            if (tmpExternalAddress == null) {
                localNode.setSocketAddress(newExternalAddress);
                tmpExternalAddress = newExternalAddress;
            } else if (!newExternalAddress.equals(localNode.getSocketAddress())) {
                if (tmpExternalAddress.equals(newExternalAddress)) {
                    localNode.setSocketAddress(newExternalAddress);
                }
                tmpExternalAddress = newExternalAddress;
            }
        }
    }
    
    public boolean isFirewalled() {
        return localNode.isFirewalled();
    }
    
    public void setFirewalled(boolean firewalled) {
        localNode.setFirewalled(firewalled);
    }
    
    public Database getDatabase() {
        return database;
    }
    
    public RouteTable getRouteTable() {
        return routeTable;
    }
    
    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }
    
    public KeyValuePublisher getPublisher() {
        return publisher;
    }
    
    public synchronized void setMessageHelper(MessageHelper messageHelper) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MessageHelper while DHT is running");
        }
        
        this.messageHelper = messageHelper;
    }
    
    public MessageHelper getMessageHelper() {
        return messageHelper;
    }
    
    public synchronized void setMessageFactory(MessageFactory messageFactory) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MessageFactory while DHT is running");
        }
        
        messageHelper.setMessageFactory(messageFactory);
    }
    
    public MessageFactory getMessageFactory() {
        return messageHelper.getMessageFactory();
    }
    
    public synchronized boolean isRunning() {
        return running;
    }

    public boolean isOpen() {
        return messageDispatcher.isOpen();
    }
    
    public boolean isBootstrapping() {
        return (isRunning() && bootstrapManager.isBootstrapping());
    }
    
    public int getReceivedMessagesCount() {
        return messageDispatcher.getReceivedMessagesCount();
    }
    
    public long getReceivedMessagesSize() {
        return messageDispatcher.getReceivedMessagesSize();
    }
    
    public int getSentMessagesCount() {
        return messageDispatcher.getSentMessagesCount();
    }
    
    public long getSentMessagesSize() {
        return messageDispatcher.getSentMessagesSize();
    }
    
    /**
     * Sets the ThreadFactory. A null value will re-set the
     * current ThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            threadFactory = new DefaultThreadFactory();
        }
        
        this.threadFactory = threadFactory;
    }
    
    /**
     * Returns the current ThreadFactory
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }
    
    /**
     * Binds the DatagramChannel to a specific address & port.
     * 
     * @param address
     * @throws IOException
     */
    public synchronized void bind(SocketAddress localAddress) throws IOException {
        if (isOpen()) {
            throw new IOException("DHT is already bound");
        }
        
        this.localAddress = localAddress;
        localNode.setSocketAddress(localAddress);
        localNode.nextInstanceID();
        
        messageDispatcher.bind(localAddress);
    }
    
    /**
     * Starts the DHT
     */
    public synchronized void start() {
        if (!isOpen()) {
            throw new IllegalStateException("DHT is not bound");
        }
        
        if (isRunning()) {
            LOG.error("DHT is already running!");
            return;
        }
        
        initContextTimer();
        initContextExecutor();
        
        pingManager.init();
        findNodeManager.init();
        findValueManager.init();
        
        running = true;
        
        messageDispatcher.start();
        bucketRefresher.start();
        publisher.start();
    }
    
    /**
     * Stops the DHT
     */
    public synchronized void stop() {
        if (!isRunning()) {
            return;
        }
        
        running = false;
        
        bucketRefresher.stop();
        publisher.stop();
        
        scheduledExecutor.shutdownNow();
        contextExecutor.shutdownNow();
        messageDispatcher.stop();
        
        lastEstimateTime = 0L;
        estimatedSize = 0;
        
        synchronized (localSizeHistory) {
            localSizeHistory.clear();
        }
        
        synchronized (remoteSizeHistory) {
            remoteSizeHistory.clear();
        }
    }
    
    /**
     * Schedules a Runnable
     * 
     * @param runnable
     * @param delay
     * @param period
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long delay, long period) {
        return scheduledExecutor.scheduleAtFixedRate(r, delay, period, TimeUnit.MILLISECONDS);
    }
    
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay) {
        return scheduledExecutor.schedule(task, delay, TimeUnit.MILLISECONDS);
    }
    
    public <V> Future<V> schedule(Callable<V> task) {
        return contextExecutor.submit(task);
    }
    
    public void execute(Runnable command) {
        contextExecutor.execute(command);
    }
    
    /** Adds a global PingListener */
    public void addPingListener(PingListener listener) {
        pingManager.addPingListener(listener);
    }
    
    /** Removes a global PingListener */
    public void removePingListener(PingListener listener) {
        pingManager.removePingListener(listener);
    }
    
    /** Returns all global PingListeners */
    public PingListener[] getPingListeners() {
        return pingManager.getPingListeners();
    }
    
    /**
     * Pings the DHT node with the given SocketAddress. 
     * Warning: This method should not be used to ping contacts from the routing table
     * 
     * @param address The address of the node
     * @param l the PingListener for incoming pongs
     * @throws IOException
     */
    public Future<Contact> ping(SocketAddress address) {
        return pingManager.ping(address);
    }
    
    /** Pings the given Node */
    public Future<Contact> ping(SocketAddress address, PingListener listener) {
        return pingManager.ping(address, listener);
    }
    
    /** Pings the given Node */
    public Future<Contact> ping(Contact node) {
        return pingManager.ping(node);
    }
    
    /** Pings the given Node */
    public Future<Contact> ping(Contact node, PingListener listener) {
        return pingManager.ping(node, listener);
    }
    
    /** Starts a value for the given KUID */
    public Future<FindValueEvent> get(KUID key, FindValueListener listener) throws IOException {
        return findValueManager.lookup(key, listener);
    }
    
    /** Starts a lookup for the given KUID */
    public Future<FindNodeEvent> lookup(KUID lookupId) {
        return findNodeManager.lookup(lookupId);
    }
    
    /** Starts a lookup for the given KUID */
    public Future<FindNodeEvent> lookup(KUID lookupId, FindNodeListener listener) {
        return findNodeManager.lookup(lookupId, listener);
    }
    
    /**
     * Tries to bootstrap from the local Route Table.
     */
    public Future<BootstrapEvent> bootstrap(BootstrapListener listener) {
        return bootstrapManager.bootstrap(listener);
    }
    
    /**
     * Tries to bootstrap from a List of Hosts. Depending on the 
     * size of the list it may fall back to bootstrapping from
     * the local Route Table!
     */
    public Future<BootstrapEvent> bootstrap(List<? extends SocketAddress> hostList, 
                BootstrapListener listener) {
        return bootstrapManager.bootstrap(hostList, listener);
    }
    
    /** 
     * Stores the given KeyValue 
     */
    public Future<StoreEvent> store(KeyValue keyValue) {
        return storeManager.store(keyValue);
    }
    
    /** 
     * Stores the given KeyValue 
     */
    public Future<StoreEvent> store(KeyValue keyValue, StoreListener listener) {
        return storeManager.store(keyValue, listener);
    }
    
    /** 
     * Stores the given KeyValue 
     */
    public Future<StoreEvent> store(Contact node, QueryKey queryKey, KeyValue keyValue) {
        return storeManager.store(node, queryKey, keyValue);
    }
    
    /**
     * Returns the approximate DHT size
     */
    public int size() {
        if (!isRunning()) {
            return 0;
        }
        
        if ((System.currentTimeMillis() - lastEstimateTime) 
                >= ContextSettings.ESTIMATE_NETWORK_SIZE_EVERY.getValue()) {
            
            estimatedSize = getEstimatedSize();
            lastEstimateTime = System.currentTimeMillis();
            networkStats.ESTIMATE_SIZE.addData(estimatedSize);
        }
        
        return estimatedSize;
    }
    
    /**
     * Adds the approximate DHT size as returned by a remote Node.
     * The average of the remote DHT sizes is incorporated into into
     * our local computation.
     */
    public void addEstimatedRemoteSize(int remoteSize) {
        if (remoteSize <= 0 || !ContextSettings.COUNT_REMOTE_SIZE.getValue()) {
            return;
        }
        
        synchronized (remoteSizeHistory) {
            remoteSizeHistory.add(new Integer(remoteSize));
            if (remoteSizeHistory.size() 
                    >= ContextSettings.MAX_REMOTE_HISTORY_SIZE.getValue()) {
                remoteSizeHistory.removeFirst();
            }
        }
    }
    
    /**
     * Computes and returns the approximate DHT size
     */
    private int getEstimatedSize() {
        KUID localNodeId = getLocalNodeID();
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        // TODO only live nodes?
        List<Contact> nodes = routeTable.select(localNodeId, k, false);
        
        // TODO accoriding to Az code it works only with more than
        // two Nodes
        if (nodes.size() <= 2) {
            // There's always we!
            return Math.max(1, nodes.size());
        }
        
        // See Azureus DHTControlImpl.estimateDHTSize()
        // Di = localNodeID xor NodeIDi
        // Dc = sum(i * Di) / sum(i * i)
        // Size = 2**160 / Dc
        
        BigInteger sum1 = BigInteger.ZERO;
        BigInteger sum2 = BigInteger.ZERO;
        
        for(int i = 1; i < nodes.size(); i++) {
            Contact node = nodes.get(i);
            
            BigInteger distance = localNodeId.xor(node.getNodeID()).toBigInteger();
            BigInteger j = BigInteger.valueOf(i);
            
            sum1 = sum1.add(j.multiply(distance));
            sum2 = sum2.add(j.pow(2));
        }
        
        int estimatedSize = 0;
        if (!sum1.equals(BigInteger.ZERO)) {
            estimatedSize = KUID.MAX_NODE_ID.toBigInteger().multiply(sum2).divide(sum1).intValue();
        }
        estimatedSize = Math.max(1, estimatedSize);
        
        int localSize = 0;
        synchronized (localSizeHistory) {
            localSizeHistory.add(new Integer(estimatedSize));
            if (localSizeHistory.size() >= ContextSettings.MAX_LOCAL_HISTORY_SIZE.getValue()) {
                localSizeHistory.removeFirst();
            }
        
            int localSizeSum = 0;
            for (Integer size : localSizeHistory) {
                localSizeSum += size.intValue();
            }
            
            // If somebody is playing around with MAX_HISTORY_SIZE
            // then localSizeHistory.size() might be zero which
            // would cause a div by zero error
            localSize = (!localSizeHistory.isEmpty() ? localSizeSum/localSizeHistory.size() : 0);
        }
        
        int combinedSize = localSize;
        if (ContextSettings.COUNT_REMOTE_SIZE.getValue()) {
            synchronized (remoteSizeHistory) {
                if (remoteSizeHistory.size() >= 3) {
                    Integer[] remote = remoteSizeHistory.toArray(new Integer[0]);
                    Arrays.sort(remote);
                    
                    // Skip the smallest and largest value
                    int count = 1;
                    while(count < remote.length-1) {
                        combinedSize += remote[count++].intValue();
                    }
                    combinedSize /= count;
                }
            }
        }
        
        // There's always we!
        return Math.max(1, combinedSize);
    }
    
    public NetworkStatisticContainer getNetworkStats() {
        return networkStats;
    }
    
    public GlobalLookupStatisticContainer getGlobalLookupStats() {
        return globalLookupStats;
    }
    
    public DatabaseStatisticContainer getDatabaseStats() {
        return databaseStats;
    }
    
    /**
     * A default implementation of the ThreadFactory.
     */
    private class DefaultThreadFactory implements ThreadFactory {
        
        private volatile int num = 0;
        
        public Thread newThread(Runnable r) {
            return new Thread(r, getName()+"-"+(num++));
        }
    }
}
