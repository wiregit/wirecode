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
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValuePublisher;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.impl.DatabaseImpl;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.event.DHTEventListener;
import com.limegroup.mojito.event.FindNodeEvent;
import com.limegroup.mojito.event.FindValueEvent;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.event.StoreEvent;
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
import com.limegroup.mojito.routing.impl.ContactNode;
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
public class Context implements MojitoDHT {
    
    private static final Log LOG = LogFactory.getLog(Context.class);
    
    private KeyPair masterKeyPair;
    
    private String name;
    
    private Contact localNode;
    private SocketAddress localAddress;
    
    private SocketAddress tmpExternalAddress;
    
    private Database database;
    private RouteTable routeTable;
    private MessageDispatcher messageDispatcher;
    private MessageHelper messageHelper;
    private DHTValuePublisher publisher;
    private RandomBucketRefresher bucketRefresher;
    
    private PingManager pingManager;
    private FindNodeManager findNodeManager;
    private FindValueManager findValueManager;
    private StoreManager storeManager;
    private BootstrapManager bootstrapManager;
    
    private volatile boolean running = false;
    
    private DHTStats dhtStats = null;
    
    private NetworkStatisticContainer networkStats;
    private GlobalLookupStatisticContainer globalLookupStats;
    private DatabaseStatisticContainer databaseStats;
    
    private long lastEstimateTime = 0L;
    private int estimatedSize = 0;
    
    private LinkedList<Integer> localSizeHistory = new LinkedList<Integer>();
    private LinkedList<Integer> remoteSizeHistory = new LinkedList<Integer>();
    
    private volatile ThreadFactory threadFactory = new DefaultThreadFactory();
    
    private ScheduledExecutorService scheduledExecutor;
    private ExecutorService contextExecutor;
    
    Context(String name, Contact localNode) {
        this.name = name;
        this.localNode = localNode;
        
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
        
        database = new DatabaseImpl();
        routeTable = new RouteTableImpl(this);
        
        messageDispatcher = new MessageDispatcherImpl(this);
        messageHelper = new MessageHelper(this);
        publisher = new DHTValuePublisher(this);

        bucketRefresher = new RandomBucketRefresher(this);
        
        pingManager = new PingManager(this);
        findNodeManager = new FindNodeManager(this);
        findValueManager = new FindValueManager(this);
        storeManager = new StoreManager(this);
        bootstrapManager = new BootstrapManager(this);
        
        // Add the local to the RouteTable
        localNode.setTimeStamp(Long.MAX_VALUE);
        routeTable.add(localNode);
        
        // Clear the firewalled flag for a moment so that we can
        // add the local node to the Route Table
        if (localNode.isFirewalled()) {
            ((ContactNode)localNode).setFirewalled(false);
            routeTable.add(localNode);
            ((ContactNode)localNode).setFirewalled(true);
        } else {
            routeTable.add(localNode);
        }
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
    
    public Contact getLocalNode() {
        return localNode;
    }
    
    public boolean isLocalNode(Contact node) {
        return isLocalNode(node.getNodeID(), node.getContactAddress());
    }
    
    public boolean isLocalNode(KUID nodeId, SocketAddress addr) {
        return isLocalNodeID(nodeId) || isLocalContactAddress(addr);
    }
    
    public boolean isLocalNodeID(KUID nodeId) {
        return nodeId != null && nodeId.equals(localNode.getNodeID());
    }
    
    public boolean isLocalContactAddress(SocketAddress address) {
        return localNode.getContactAddress().equals(address);
    }
    
    public KUID getLocalNodeID() {
        return localNode.getNodeID();
    }
    
    public synchronized MessageDispatcher setMessageDispatcher(Class<? extends MessageDispatcher> clazz) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MessageDispatcher while DHT is running");
        }

        if (clazz == null) {
            clazz = MessageDispatcherImpl.class;
        }
        
        try {
            Constructor c = clazz.getConstructor(Context.class);
            messageDispatcher = (MessageDispatcher)c.newInstance(this);
            return messageDispatcher;
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
    
    /**
     * Returns the MessageDispatcher
     */
    public MessageDispatcher getMessageDispatcher() {
        // Not synchronized 'cause only called when Mojito is running and 
        // while Mojito is running you cannot change the MessageDispatcher
        return messageDispatcher;
    }
    
    public synchronized RouteTable setRouteTable(Class<? extends RouteTable> clazz) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch RouteTable while DHT is running");
        }

        if (clazz == null) {
            clazz = RouteTableImpl.class;
        }
        
        try {
            Constructor c = clazz.getConstructor(Context.class);
            routeTable = (RouteTable)c.newInstance(this);
            
            // Clear the firewalled flag for a moment so that we can
            // add the local node to the Route Table
            if (localNode.isFirewalled()) {
                ((ContactNode)localNode).setFirewalled(false);
                routeTable.add(localNode);
                ((ContactNode)localNode).setFirewalled(true);
            } else {
                routeTable.add(localNode);
            }
            
            return routeTable;
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
    
    /**
     * Returns the RouteTable
     */
    public RouteTable getRouteTable() {
        // Not synchronized 'cause only called when Mojito is running and 
        // while Mojito is running you cannot change the RouteTable
        return routeTable;
    }
    
    public synchronized void setDatabase(Database database) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch Database while DHT is running");
        }
        
        if (database == null) {
            database = new DatabaseImpl();
        }
        
        this.database = database;
        
        // Make sure all local values have the local Node
        // as originator
        for (KUID valueId : database.keySet()) {
            for (DHTValue value : database.get(valueId).values()) {
                if (value.isLocalValue()) {
                    value.setOriginator(localNode);
                }
            }
        }
    }
    
    /**
     * Returns the Database
     */
    public Database getDatabase() {
        // Not synchronized 'cause only called when Mojito is running and 
        // while Mojito is running you cannot change the Database
        return database;
    }
    
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
    
    public synchronized void setMessageFactory(MessageFactory messageFactory) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MessageFactory while DHT is running");
        }
        
        messageHelper.setMessageFactory(messageFactory);
    }
    
    /**
     * Returns the current MessageFactory. In most cases you want to use
     * the MessageHelper instead which is a simplified version of the
     * MessageFactory.
     */
    public MessageFactory getMessageFactory() {
        // Not synchronized 'cause only called when Mojito is running and 
        // while Mojito is running you cannot change the MessageHelper
        return messageHelper.getMessageFactory();
    }
    
    /**
     * Sets the MessageHelper
     */
    public synchronized void setMessageHelper(MessageHelper messageHelper) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MessageHelper while DHT is running");
        }
        
        this.messageHelper = messageHelper;
    }
    
    /**
     * Returns the current MessageHelper which is a simplified
     * MessageFactory
     */
    public MessageHelper getMessageHelper() {
        // Not synchronized 'cause only called when Mojito is running and 
        // while Mojito is running you cannot change the MessageHelper
        return messageHelper;
    }
    
    public synchronized void setExternalPort(int port) {
        InetSocketAddress addr = (InetSocketAddress)localNode.getContactAddress();
        setContactAddress(new InetSocketAddress(addr.getAddress(), port));
    }
    
    public synchronized int getExternalPort() {
        return ((InetSocketAddress)localNode.getContactAddress()).getPort();
    }
    
    public synchronized SocketAddress getContactAddress() {
        return localNode.getContactAddress();
    }
    
    public synchronized void setContactAddress(SocketAddress externalAddress) {
        if (isFirewalled() && ((InetSocketAddress)externalAddress).getPort() != 0) {
            throw new IllegalStateException();
        }
        
        ((ContactNode)localNode).setContactAddress(externalAddress);
        
        // NOTE: local DHTValues are holding a reference to
        // 'localNode' and setting the originator is not
        // neccessary thus.
    }
    
    public SocketAddress getLocalAddress() {
        return localAddress;
    }
    
    public void setExternalSocketAddress(SocketAddress externalSocketAddress)
            throws IOException {
        if (externalSocketAddress == null) {
            return;
        }
        
        // --- DOES NOT CHANGE THE PORT! ---
        
        InetAddress externalAddress = ((InetSocketAddress)externalSocketAddress).getAddress();
        //int externalPort = ((InetSocketAddress)externalSocketAddress).getPort();
        
        InetAddress currentAddress = ((InetSocketAddress)localNode.getContactAddress()).getAddress();
        int currentPort = ((InetSocketAddress)localNode.getContactAddress()).getPort();
        
        if (externalAddress.equals(currentAddress)) {
            return;
        }
        
        InetSocketAddress addr = new InetSocketAddress(externalAddress, currentPort);
        
        if (tmpExternalAddress == null 
                || tmpExternalAddress.equals(addr)) {
            setContactAddress(addr);
            
            //if (externalPort == currentPort) {}
        }
        
        tmpExternalAddress = addr;
    }
    
    public boolean isFirewalled() {
        return localNode.isFirewalled();
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
     * Returns whether or not the MessageDispatcher has
     * an open DatagramChannel
     */
    public boolean isOpen() {
        return messageDispatcher.isOpen();
    }
    
    /**
     * Returns whether or not the MojitoDHT is running
     */
    public synchronized boolean isRunning() {
        return running;
    }

    /**
     * Returns whether or not the MojitoDHT is bootstrapping
     */
    public synchronized boolean isBootstrapping() {
        return isRunning() && bootstrapManager.isBootstrapping();
    }
    
    /**
     * Returns whether or not the MojitoDHT is bootstrapped
     */
    public synchronized boolean isBootstrapped() {
        return isRunning() && bootstrapManager.isBootstrapped();
    }
    
    public synchronized void bind(int port) throws IOException {
        bind(new InetSocketAddress(port));
    }
    
    public synchronized void bind(InetAddress addr, int port) throws IOException {
        bind(new InetSocketAddress(addr, port));
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
        
        int port = ((InetSocketAddress)localAddress).getPort();
        if (port == 0) {
            throw new IllegalArgumentException("Cannot bind Socket to Port " + port);
        }
        
        this.localAddress = localAddress;
        
        // If we not firewalled and the external port has not 
        // been set yet then set it to the same port as the 
        // local address.
        if (!isFirewalled() && getExternalPort() == 0) {
            setExternalPort(((InetSocketAddress)localAddress).getPort());
        }
        
        ((ContactNode)localNode).setSourceAddress(localAddress);
        ((ContactNode)localNode).nextInstanceID();
        
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
        
        tmpExternalAddress = null;
        
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
        
        bootstrapManager.setBootstrapped(false);
        
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
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long delay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(r, delay, period, unit);
    }
    
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
        return scheduledExecutor.schedule(task, delay, unit);
    }
    
    public <V> Future<V> schedule(Callable<V> task) {
        return contextExecutor.submit(task);
    }
    
    public void execute(Runnable command) {
        contextExecutor.execute(command);
    }
    
    /** Adds a global PingListener */
    public void addPingListener(PingListener listener) {
        pingManager.addDHTEventListener(listener);
    }
    
    /** Removes a global PingListener */
    public void removePingListener(PingListener listener) {
        pingManager.removeDHTEventListener(listener);
    }
    
    /** Returns all global PingListeners */
    public DHTEventListener[] getPingListeners() {
        return pingManager.getDHTEventListeners();
    }
    
    /**
     * Pings the DHT node with the given SocketAddress. 
     * Warning: This method should not be used to ping contacts from the routing table
     * 
     * @param address The address of the node
     * @param l the PingListener for incoming pongs
     * @throws IOException
     */
    public DHTFuture<Contact> ping(SocketAddress address) {
        return pingManager.ping(address);
    }
    
    /** Pings the given Node */
    public DHTFuture<Contact> ping(Contact node) {
        return pingManager.ping(node);
    }
    
    /** Starts a value for the given KUID */
    public DHTFuture<FindValueEvent> get(KUID key) {
        return findValueManager.lookup(key);
    }
    
    /** Starts a lookup for the given KUID */
    public DHTFuture<FindNodeEvent> lookup(KUID lookupId) {
        return findNodeManager.lookup(lookupId);
    }
    
    /**
     * Cancels an active bootstrap process
     */
    public boolean cancelBootstrapping() {
        return bootstrapManager.cancelBootstrapping();
    }
    
    /**
     * Tries to bootstrap from the local Route Table.
     */
    public DHTFuture<BootstrapEvent> bootstrap() {
        return bootstrapManager.bootstrap();
    }
    
    public DHTFuture<BootstrapEvent> bootstrap(SocketAddress address) {
        return bootstrap(Arrays.asList(address));
    }
    
    public DHTFuture<BootstrapEvent> bootstrap(List<? extends SocketAddress> hostList) {
        return bootstrapManager.bootstrap(hostList);
    }

    public DHTFuture<StoreEvent> put(KUID key, byte[] value) {
        DHTValue dhtValue = DHTValue.createLocalValue(getLocalNode(), key, value);
        database.store(dhtValue);
        return store(dhtValue);
    }
    
    public DHTFuture<StoreEvent> remove(KUID key) {
        // To remove a KeyValue you just store an empty value!
        return put(key, DHTValue.EMPTY_DATA);
    }
    
    /** 
     * Stores the given KeyValue 
     */
    public DHTFuture<StoreEvent> store(DHTValue dhtValue) {
        return storeManager.store(dhtValue);
    }
   
    /** 
     * Stores the given KeyValue 
     */
    public DHTFuture<StoreEvent> store(Contact node, QueryKey queryKey, DHTValue dhtValue) {
        return storeManager.store(node, queryKey, dhtValue);
    }
    
    public Set<KUID> keySet() {
        return getDatabase().keySet();
    }
    
    public Collection<DHTValue> getValues() {
        Database database = getDatabase();
        synchronized (database) {
            return new ArrayList<DHTValue>(database.values());
        }
    }
    
    /**
     * Returns the approximate DHT size
     */
    public synchronized int size() {
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
    
    public void store(OutputStream out) throws IOException {
        MojitoFactory.store(this, out);
    }
}