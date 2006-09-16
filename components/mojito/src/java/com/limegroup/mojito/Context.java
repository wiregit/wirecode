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
import java.util.Collection;
import java.util.Collections;
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
import com.limegroup.mojito.routing.ContactFactory;
import com.limegroup.mojito.routing.RandomBucketRefresher;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.LocalContact;
import com.limegroup.mojito.routing.impl.RouteTableImpl;
import com.limegroup.mojito.security.CryptoHelper;
import com.limegroup.mojito.settings.ContextSettings;
import com.limegroup.mojito.statistics.DHTStats;
import com.limegroup.mojito.statistics.DHTStatsManager;
import com.limegroup.mojito.statistics.DatabaseStatisticContainer;
import com.limegroup.mojito.statistics.GlobalLookupStatisticContainer;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;
import com.limegroup.mojito.util.BucketUtils;
import com.limegroup.mojito.util.DHTSizeEstimator;
import com.limegroup.mojito.util.DatabaseUtils;

/**
 * The Context is the heart of Mojito where everything comes 
 * together. 
 */
public class Context implements MojitoDHT, RouteTable.Callback {
    
    private static final Log LOG = LogFactory.getLog(Context.class);
    
    private KeyPair masterKeyPair;
    
    private String name;
    
    private LocalContact localNode;
    
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
    
    private DHTStats stats;
    private NetworkStatisticContainer networkStats;
    private GlobalLookupStatisticContainer globalLookupStats;
    private DatabaseStatisticContainer databaseStats;
    
    private volatile ThreadFactory threadFactory = new DefaultThreadFactory();
    
    private ScheduledExecutorService scheduledExecutor;
    private ExecutorService contextExecutor;
    
    private DHTSizeEstimator estimator;
    
    /**
     * Constructor to create a new Context
     */
    Context(String name, int vendor, int version, boolean firewalled) {
        this.name = name;
        this.localNode = (LocalContact)ContactFactory
            .createLocalContact(vendor, version, firewalled);
        
        init();
    }
    
    /**
     * Constructor to create a Context from a pre-existing
     * RouteTable and Database.
     */
    Context(String name, int vendor, int version, RouteTable routeTable, Database database) {
        this.name = name;
        
        assert (routeTable != null);
        assert (database != null);
        
        this.localNode = (LocalContact)routeTable.getLocalNode();
        this.localNode.setVendor(vendor);
        this.localNode.setVersion(version);
        
        this.routeTable = routeTable;
        this.database = database;
        
        init();
    }
    
    private void init() {
        PublicKey masterKey = null;
        try {
            File file = new File(ContextSettings.MASTER_KEY.getValue());
            if (file.exists() && file.isFile()) {
                masterKey = CryptoHelper.loadMasterKey(file);
            }
        } catch (Exception err) {
            LOG.error("Loading the MasterKey failed!", err);
        }
        masterKeyPair = new KeyPair(masterKey, null);
        
        initStats();
        
        if (routeTable == null) {
            setRouteTable(null);
        } else {
            routeTable.setRouteTableCallback(this);
        }
        
        if (database == null) {
            setDatabase(null, false);
        }
        
        messageDispatcher = new MessageDispatcherImpl(this);
        messageHelper = new MessageHelper(this);
        publisher = new DHTValuePublisher(this);

        bucketRefresher = new RandomBucketRefresher(this);
        
        pingManager = new PingManager(this);
        findNodeManager = new FindNodeManager(this);
        findValueManager = new FindValueManager(this);
        storeManager = new StoreManager(this);
        bootstrapManager = new BootstrapManager(this);
    }
    
    private void initStats() {
        stats = DHTStatsManager.getInstance(localNode.getNodeID());
        networkStats = new NetworkStatisticContainer(localNode.getNodeID());
        globalLookupStats = new GlobalLookupStatisticContainer(localNode.getNodeID());
        databaseStats = new DatabaseStatisticContainer(localNode.getNodeID());
    }
    
    private void initContextTimer() {
        ThreadFactory factory = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread thread = getThreadFactory().newThread(r);
                thread.setName(getName() + "-ContextScheduledThreadPool");
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
                thread.setName(getName() + "-ContextCachedThreadPool");
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
        return stats;
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
        setLocalNodeID(KUID.createRandomID());
    }
    
    /**
     * Sets the local Node ID to the given ID. See also 
     * changeNodeID() !
     */
    private synchronized void setLocalNodeID(KUID nodeId) {
        if (!nodeId.equals(getLocalNodeID())) {
            
            if (LOG.isInfoEnabled()) {
                LOG.info("Changing local Node ID from " + getLocalNodeID() + " to " + nodeId);
            }
            
            synchronized (routeTable) {
                // Backup the current Node ID and all live Contacts
                List<Contact> backup = new ArrayList<Contact>(routeTable.getLiveContacts());
                backup.remove(localNode);
                
                // Change the Node ID
                localNode.setNodeID(nodeId);
                localNode.nextInstanceID();
                
                // Clear the RouteTable and add the local Node with our
                // new Node ID
                routeTable.clear();
                routeTable.add(localNode);
                
                // Sort the Nodes list (because rebuilding the table with 
                // the new Node ID will probably evict some nodes)
                backup = BucketUtils.sortAliveToFailed(backup);
                
                // Re-add the Contacts but set their state to Unknown
                // so that they can be easily replaced by new live 
                // Contacts
                for (Contact node : backup) {
                    node.unknown();
                    routeTable.add(node);
                }
            }
            
            purgeDatabase(true);
        }
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
        
        // Initialize the RouteTable with the local Node and
        // if it's pre-initialized then get the local Node
        if (routeTable.size() == 0) {
            routeTable.add(localNode);
        } else {
            localNode = (LocalContact)routeTable.getLocalNode();
        }
        
        routeTable.setRouteTableCallback(this);
        this.routeTable = routeTable;
        
        if (database != null) {
            purgeDatabase(true);
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
        setDatabase(database, true);
    }
    
    private synchronized void setDatabase(Database database, boolean remove) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch Database while DHT is running");
        }
        
        if (database == null) {
            database = new DatabaseImpl();
            
        } else {
            purgeDatabase(remove);
        }
        
        this.database = database;
    }
    
    /**
     * 
     * @param remove
     */
    private void purgeDatabase(boolean remove) {
        
        synchronized (database) {
            int oldValueCount = database.getValueCount();
            int removedCount = 0;
            for (DHTValue value : database.values()) {
                if (value.isLocalValue()) {
                    // Make sure all local DHTValues have the
                    // local Node as the originator
                    value.setOriginator(localNode);
                } else {
                    
                    // Remove all non local DHTValues. We're assuming
                    // the Node IDs are totally random so chances are 
                    // slim to none that we're responsible for the values
                    // again. Even if we are there's no way to test
                    // it until we've re-bootstrapped in which case the
                    // the other guys will send us anyways DHTValues to
                    // store. So, any work would be redundant!
                    
                    if (remove || DatabaseUtils.isExpired(getRouteTable(), value)) {
                        database.remove(value);
                        removedCount++;
                    }
                }
            }
            
            // Make sure we've really removed the values
            assert (database.getValueCount() == (oldValueCount - removedCount));
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
        return localNode.getSourceAddress();
    }
    
    public void setExternalAddress(SocketAddress externalSocketAddress) {
        localNode.setExternalAddress(externalSocketAddress);
    }
    
    public boolean isFirewalled() {
        return localNode.isFirewalled();
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
        
        estimator = new DHTSizeEstimator(getRouteTable());
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
        
        estimator.clear();
    }
    
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, 
            long delay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(command, delay, period, unit);
    }
    
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, 
            long initialDelay, long delay, TimeUnit unit) {
        return scheduledExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
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
    
    /** Starts a lookup for the given KUID */
    public DHTFuture<FindNodeEvent> lookup(KUID lookupId) {
        return findNodeManager.lookup(lookupId);
    }
    
    /**
     * Tries to bootstrap from the local Route Table.
     */
    public DHTFuture<BootstrapEvent> bootstrap() {
        return bootstrapManager.bootstrap();
    }
    
    public DHTFuture<BootstrapEvent> bootstrap(SocketAddress address) {
        return bootstrap(Collections.singleton(address));
    }
    
    public DHTFuture<BootstrapEvent> bootstrap(Set<? extends SocketAddress> hostList) {
        return bootstrapManager.bootstrap(hostList);
    }
    
    public DHTFuture<StoreEvent> put(KUID key, byte[] value) {
        if(!isBootstrapped()) {
            throw new NotBootstrappedException(getName() + " put()");
        }
        
        DHTValue dhtValue = DHTValue.createLocalValue(getLocalNode(), key, value);
        database.store(dhtValue);
        return store(dhtValue);
    }
    
    public DHTFuture<StoreEvent> remove(KUID key) {
        if(!isBootstrapped()) {
            throw new NotBootstrappedException(getName() + " remove()");
        }
        
        // To remove a KeyValue you just store an empty value!
        return put(key, DHTValue.EMPTY_DATA);
    }
    
    /** 
     * Stores the given KeyValue 
     */
    public DHTFuture<StoreEvent> store(DHTValue value) {
        if(!isBootstrapped()) {
            throw new NotBootstrappedException(getName() + " store()");
        }
        
        return storeManager.store(value);
    }
   
    /**
     * 
     */
    public DHTFuture<StoreEvent> store(Collection<? extends DHTValue> values) {
        if(!isBootstrapped()) {
            throw new NotBootstrappedException(getName() + " store()");
        }
        
        return storeManager.store(values);
    }
    
    /** 
     * Stores the given KeyValue 
     */
    public DHTFuture<StoreEvent> store(Contact node, QueryKey queryKey, DHTValue value) {
        if(!isBootstrapped()) {
            throw new NotBootstrappedException(getName() + " store()");
        }
        
        return storeManager.store(node, queryKey, value);
    }
    
    /**
     * 
     */
    public DHTFuture<StoreEvent> store(Contact node, QueryKey queryKey, Collection<? extends DHTValue> values) {
        if(!isBootstrapped()) {
            throw new NotBootstrappedException(getName() + " store()");
        }
        
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
    public synchronized BigInteger size() {
        if (!isRunning()) {
            return BigInteger.ZERO;
        }
        
        return estimator.getEstimatedSize();
    }
    
    /**
     * Adds the approximate DHT size as returned by a remote Node.
     * The average of the remote DHT sizes is incorporated into into
     * our local computation.
     */
    public void addEstimatedRemoteSize(BigInteger remoteSize) {
    	estimator.addEstimatedRemoteSize(remoteSize);
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
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append("Local ContactNode: ").append(getLocalNode()).append("\n");
        buffer.append("Is running: ").append(isRunning()).append("\n");
        buffer.append("Database Size (Keys): ").append(getDatabase().getKeyCount()).append("\n");
        buffer.append("Database Size (Values): ").append(getDatabase().getValueCount()).append("\n");
        buffer.append("RouteTable Size: ").append(getRouteTable().size()).append("\n");
        buffer.append("Estimated DHT Size: ").append(size()).append("\n");
        
        return buffer.toString();
    }
}