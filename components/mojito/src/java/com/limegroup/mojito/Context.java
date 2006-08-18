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
import java.util.HashSet;
import java.util.Iterator;
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
import com.limegroup.mojito.exceptions.NotBootstrappedException;
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
import com.limegroup.mojito.routing.impl.LocalContactImpl;
import com.limegroup.mojito.routing.impl.RouteTableImpl;
import com.limegroup.mojito.security.CryptoHelper;
import com.limegroup.mojito.settings.ContextSettings;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.DHTStats;
import com.limegroup.mojito.statistics.DHTStatsFactory;
import com.limegroup.mojito.statistics.DatabaseStatisticContainer;
import com.limegroup.mojito.statistics.GlobalLookupStatisticContainer;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;
import com.limegroup.mojito.util.BucketUtils;

/**
 * The Context is the heart of Mojito where everything comes 
 * together. 
 */
public class Context implements MojitoDHT, RouteTable.Callback {
    
    private static final Log LOG = LogFactory.getLog(Context.class);
    
    private KeyPair masterKeyPair;
    
    private String name;
    
    private LocalContactImpl localNode;
    
    private SocketAddress localAddress;
    
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
    
    private List<Integer> localSizeHistory = new LinkedList<Integer>();
    private List<Integer> remoteSizeHistory = new LinkedList<Integer>();
    
    private volatile ThreadFactory threadFactory = new DefaultThreadFactory();
    
    private ScheduledExecutorService scheduledExecutor;
    private ExecutorService contextExecutor;
    
    Context(String name, LocalContactImpl localNode) {
        this(name, localNode, null, null);
    }
    
    Context(String name, LocalContactImpl localNode, 
            RouteTable routeTable, Database database) {
        
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
        
        dhtStats = DHTStatsFactory.newInstance(this);
        
        networkStats = new NetworkStatisticContainer(localNode.getNodeID());
        globalLookupStats = new GlobalLookupStatisticContainer(localNode.getNodeID());
        databaseStats = new DatabaseStatisticContainer(localNode.getNodeID());
        
        setRouteTable(routeTable);
        setDatabase(database, false);
        
        messageDispatcher = new MessageDispatcherImpl(this);
        messageHelper = new MessageHelper(this);
        publisher = new DHTValuePublisher(this);

        bucketRefresher = new RandomBucketRefresher(this);
        
        pingManager = new PingManager(this);
        findNodeManager = new FindNodeManager(this);
        findValueManager = new FindValueManager(this);
        storeManager = new StoreManager(this);
        bootstrapManager = new BootstrapManager(this);
        
        // Init the RouteTable with the local Node
        initRouteTable();
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
        return localNode.getVendor();
    }
    
    public int getVersion() {
        return localNode.getVersion();
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
    
    /**
     * Clears the RouteTable, generates a new random Node ID
     * for the local Node and adds the (new) Node to the RouteTable.
     * 
     * WARNING: Meant to be called only by BootstrapManager 
     *          or MojitoFactory!
     */
    public void changeNodeID() {
        setLocalNodeID(KUID.createRandomNodeID());
    }
    
    /**
     * Sets the local Node ID to the given ID. See also 
     * changeNodeID() !
     */
    private void setLocalNodeID(KUID nodeId) {
        if (!nodeId.equals(getLocalNodeID())) {
            
            if (LOG.isInfoEnabled()) {
                LOG.info("Changing local Node ID from " + getLocalNodeID() + " to " + nodeId);
            }
            
            synchronized (routeTable) {
                // Backup the current Node ID and all live Contacts
                KUID oldNodeId = localNode.getNodeID();
                List<Contact> backup = new ArrayList<Contact>(routeTable.getLiveContacts());
                
                // Change the Node ID
                localNode.setNodeID(nodeId);
                localNode.nextInstanceID();
                
                // Clear the RouteTable and add the local Node with our
                // new Node ID
                routeTable.clear();
                initRouteTable();
                
                // Sort the Nodes list (because rebuilding the table with 
                // the new Node ID will probably evict some nodes)
                backup = BucketUtils.sortAliveToFailed(backup);
                
                // Re-add the Contacts but set their state to Unknown
                // so that they can be easily replaced by new live 
                // Contacts
                for (Contact node : backup) {
                    if (!oldNodeId.equals(node.getNodeID())) {
                        node.unknown();
                        routeTable.add(node);
                    }
                }
            }
            
            synchronized (database) {
                // And finally clean up the Database
                int oldValueCount = database.getValueCount();
                int removedCount = 0;
                for (Iterator<DHTValue> it = database.values().iterator(); it.hasNext(); ) {
                    DHTValue value = it.next();
                    if (value.isLocalValue()) {
                        // This is technically not necessary as they're 
                        // already holding a reference to localNode and 
                        // the new ID gets reflected to them
                        value.setOriginator(localNode);
                    } else {
                        
                        // Remove all non local DHTValues. We're assuming
                        // the Node IDs are totally random so chances are 
                        // slim to none that we're responsible for the values
                        // again. Even if we are there's no way to test
                        // it until we've re-bootstrapped in which case the
                        // the other guys will send us anyways DHTValues to
                        // store. So, any work would be redundant!
                        it.remove();
                        removedCount++;
                    }
                }
                
                // Make sure we've really removed the values
                assert (database.getValueCount() == (oldValueCount - removedCount));
            }
        }
    }
    
    /**
     * Adds the local Node to the RouteTable
     */
    private void initRouteTable() {
        routeTable.add(localNode);
    }
    
    public boolean isLocalNode(Contact node) {
        return node.equals(getLocalNode());
    }
    
    public boolean isLocalNode(KUID nodeId, SocketAddress addr) {
        return isLocalNodeID(nodeId) && isLocalContactAddress(addr);
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
            Constructor<? extends MessageDispatcher> c = clazz.getConstructor(Context.class);
            c.setAccessible(true);
            messageDispatcher = c.newInstance(this);
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
    
    public synchronized void setRouteTable(RouteTable routeTable) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch RouteTable while DHT is running");
        }

        if (routeTable == null) {
            routeTable = new RouteTableImpl();
        }
        
        routeTable.setRouteTableCallback(this);
        this.routeTable = routeTable;
        initRouteTable();
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
        setDatabase(database, true);
    }
    
    private synchronized void setDatabase(Database database, boolean remove) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch Database while DHT is running");
        }
        
        if (database == null) {
            database = new DatabaseImpl();
            
        } else {
            
            synchronized (database) {
                for (Iterator<DHTValue> it = database.values().iterator(); it.hasNext(); ) {
                    DHTValue value = it.next();
                    
                    // Make sure all local values have the local Node
                    // as originator
                    if (value.isLocalValue()) {
                        value.setOriginator(localNode);
                        
                    // Else prune out either all non-local values or
                    // remove them if they've expired.
                    } else if (remove 
                            || (!value.isLocalValue() && isExpired(value))) {
                        it.remove();
                    }
                }
            }
        }
        
        this.database = database;
    }
    
    /**
     * Returns the Database
     */
    public Database getDatabase() {
        // Not synchronized 'cause only called when Mojito is running and 
        // while Mojito is running you cannot change the Database
        return database;
    }
    
    /**
     * Returns the expiration time of the given DHTValue
     */
    public long getExpirationTime(DHTValue value) {
        if (value.isLocalValue()) {
            return Long.MAX_VALUE;
        }
        
        KUID valueId = value.getValueID();
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        List<Contact> nodes = getRouteTable().select(valueId, k, false);
        
        long creationTime = value.getCreationTime();
        long expirationTime = DatabaseSettings.VALUE_EXPIRATION_TIME.getValue();
        
        long expiresAt = 0L;
        
        if (nodes.size() < k || nodes.contains(getLocalNode())) {
            expiresAt = creationTime + expirationTime;
            
        } else {
            KUID nearestId = nodes.get(0).getNodeID();
            KUID localId = getLocalNodeID();
            KUID xor = localId.xor(nearestId);
            int log2 = xor.log2();
            
            expiresAt = creationTime + (expirationTime / KUID.LENGTH_IN_BITS * log2);
        }
        
        return expiresAt;
    }
    
    /**
     * Returns whether or not the given DHTValue has expired
     */
    public boolean isExpired(DHTValue value) {
        return System.currentTimeMillis() >= getExpirationTime(value);
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
    
    public void setExternalPort(int port) {
        localNode.setExternalPort(port);
    }
    
    public int getExternalPort() {
        return localNode.getExternalPort();
    }
    
    public SocketAddress getContactAddress() {
        return localNode.getContactAddress();
    }
    
    public void setContactAddress(SocketAddress externalAddress) {
        localNode.setContactAddress(externalAddress);
    }
    
    public SocketAddress getLocalAddress() {
        return localAddress;
    }
    
    public void setExternalAddress(SocketAddress externalSocketAddress) {
        localNode.setExternalAddress(externalSocketAddress);
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
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Binding DHT "+ name+ " to address: "+localAddress);
        }
        
        this.localAddress = localAddress;
        
        // If we not firewalled and the external port has not 
        // been set yet then set it to the same port as the 
        // local address.
        if (!isFirewalled() && getExternalPort() == 0) {
            setExternalPort(((InetSocketAddress)localAddress).getPort());
        }
        
        localNode.setSourceAddress(localAddress);
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
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Stopping DHT "+name);
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
    
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long delay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(command, delay, period, unit);
    }
    
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
        return scheduledExecutor.schedule(task, delay, unit);
    }
    
    public <V> Future<V> submit(Callable<V> task) {
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
    
    /** Sends a special collision test Ping to the given Node */
    public DHTFuture<Contact> collisionPing(Contact node) {
        return pingManager.collisionPing(node);
    }
    
    /** Starts a value for the given KUID */
    public DHTFuture<FindValueEvent> get(KUID key) {
        if(!isBootstrapped()) {
            throw new NotBootstrappedException("get");
        }
        return findValueManager.lookup(key);
    }
    
    public DHTFuture<BootstrapEvent> bootstrap(SocketAddress address) {
        Set<SocketAddress> set = new HashSet<SocketAddress>();
        set.add(address);
        return bootstrap(set);
    }

    /** Starts a lookup for the given KUID */
    public DHTFuture<FindNodeEvent> lookup(KUID lookupId) {
        return findNodeManager.lookup(lookupId);
    }
    
    public DHTFuture<BootstrapEvent> bootstrap(Set<? extends SocketAddress> hostList) {
        return bootstrapManager.bootstrap(hostList);
    }

    /**
     * Tries to bootstrap from the local Route Table.
     */
    public DHTFuture<BootstrapEvent> bootstrap() {
        return bootstrapManager.bootstrap();
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
    public DHTFuture<StoreEvent> store(DHTValue value) {
        return storeManager.store(value);
    }
   
    /**
     * 
     */
    public DHTFuture<StoreEvent> store(Collection<? extends DHTValue> values) {
        return storeManager.store(values);
    }
    
    /** 
     * Stores the given KeyValue 
     */
    public DHTFuture<StoreEvent> store(Contact node, QueryKey queryKey, DHTValue value) {
        return storeManager.store(node, queryKey, value);
    }
    
    /**
     * 
     */
    public DHTFuture<StoreEvent> store(Contact node, QueryKey queryKey, Collection<? extends DHTValue> values) {
        return storeManager.store(node, queryKey, values);
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
                remoteSizeHistory.remove(0);
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
            estimatedSize = KUID.MAXIMUM.toBigInteger().multiply(sum2).divide(sum1).intValue();
        }
        estimatedSize = Math.max(1, estimatedSize);
        
        int localSize = 0;
        synchronized (localSizeHistory) {
            localSizeHistory.add(new Integer(estimatedSize));
            if (localSizeHistory.size() >= ContextSettings.MAX_LOCAL_HISTORY_SIZE.getValue()) {
                localSizeHistory.remove(0);
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
        
        // There's always us!
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