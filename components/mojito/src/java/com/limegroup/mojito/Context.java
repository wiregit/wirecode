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
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.concurrent.DHTExecutorService;
import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValueFactory;
import com.limegroup.mojito.db.DHTValueManager;
import com.limegroup.mojito.db.DHTValueType;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.impl.DatabaseImpl;
import com.limegroup.mojito.exceptions.NotBootstrappedException;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.io.MessageDispatcherImpl;
import com.limegroup.mojito.manager.BootstrapManager;
import com.limegroup.mojito.manager.FindNodeManager;
import com.limegroup.mojito.manager.FindValueManager;
import com.limegroup.mojito.manager.GetValueManager;
import com.limegroup.mojito.manager.PingManager;
import com.limegroup.mojito.manager.StoreManager;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.messages.MessageHelper;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.result.BootstrapResult;
import com.limegroup.mojito.result.FindNodeResult;
import com.limegroup.mojito.result.FindValueResult;
import com.limegroup.mojito.result.PingResult;
import com.limegroup.mojito.result.StoreResult;
import com.limegroup.mojito.routing.BucketRefresher;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.LocalContact;
import com.limegroup.mojito.routing.impl.RouteTableImpl;
import com.limegroup.mojito.settings.ContextSettings;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.DHTStats;
import com.limegroup.mojito.statistics.DHTStatsManager;
import com.limegroup.mojito.statistics.DatabaseStatisticContainer;
import com.limegroup.mojito.statistics.GlobalLookupStatisticContainer;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;
import com.limegroup.mojito.statistics.RoutingStatisticContainer;
import com.limegroup.mojito.util.BucketUtils;
import com.limegroup.mojito.util.CryptoUtils;
import com.limegroup.mojito.util.DHTSizeEstimator;
import com.limegroup.mojito.util.DatabaseUtils;
import com.limegroup.mojito.util.HostFilter;

/**
 * The Context is the heart of Mojito where everything comes 
 * together. 
 */
public class Context implements MojitoDHT, RouteTable.PingCallback {
    
    private static final Log LOG = LogFactory.getLog(Context.class);
    
    private KeyPair masterKeyPair;
    
    private String name;
    
    private Database database;
    private RouteTable routeTable;
    private MessageDispatcher messageDispatcher;
    private MessageHelper messageHelper;
    private DHTValueManager valueManager;
    private BucketRefresher bucketRefresher;
    
    private PingManager pingManager;
    private FindNodeManager findNodeManager;
    private FindValueManager findValueManager;
    private StoreManager storeManager;
    private BootstrapManager bootstrapManager;
    private GetValueManager getValueManager;
    
    private volatile boolean running = false;
    
    private DHTStats stats;
    private NetworkStatisticContainer networkStats;
    private GlobalLookupStatisticContainer globalLookupStats;
    private DatabaseStatisticContainer databaseStats;
    
    private DHTSizeEstimator estimator;
    
    private volatile DHTExecutorService executorService = new DefaultDHTExecutorService();
    
    /**
     * Constructor to create a new Context
     */
    Context(String name, int vendor, int version, boolean firewalled) {
        this.name = name;
        init(null, null);
        
        getLocalNode().setVendor(vendor);
        getLocalNode().setVersion(version);
        getLocalNode().setFirewalled(firewalled);
    }
    
    /**
     * Constructor to create a Context from a pre-existing
     * RouteTable and Database.
     */
    Context(String name, int vendor, int version, RouteTable routeTable, Database database) {
        this.name = name;
        
        assert (routeTable != null);
        assert (database != null);
        
        init(routeTable, database);
        
        getLocalNode().setVendor(vendor);
        getLocalNode().setVersion(version);
    }
    
    /**
     * Initializes the Context
     */
    private void init(RouteTable routeTable, Database database) {
        PublicKey masterKey = null;
        try {
            File file = new File(ContextSettings.MASTER_KEY.getValue());
            if (file.exists() && file.isFile()) {
                masterKey = CryptoUtils.loadMasterKey(file);
            }
        } catch (Exception err) {
            LOG.error("Loading the MasterKey failed!", err);
        }
        masterKeyPair = new KeyPair(masterKey, null);
        
        setRouteTable(routeTable);
        setDatabase(database, false);
        
        initStats();
        
        setMessageDispatcher(null);
        
        messageHelper = new MessageHelper(this);
        valueManager = new DHTValueManager(this);

        bucketRefresher = new BucketRefresher(this);
        
        pingManager = new PingManager(this);
        findNodeManager = new FindNodeManager(this);
        findValueManager = new FindValueManager(this);
        storeManager = new StoreManager(this);
        bootstrapManager = new BootstrapManager(this);
        getValueManager = new GetValueManager(this);
    }
    
    /**
     * Initializes the Stats package
     */
    private void initStats() {
        stats = DHTStatsManager.getInstance(getLocalNodeID());
        networkStats = new NetworkStatisticContainer(getLocalNodeID());
        globalLookupStats = new GlobalLookupStatisticContainer(getLocalNodeID());
        databaseStats = new DatabaseStatisticContainer(getLocalNodeID());
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getName()
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getDHTStats()
     */
    public DHTStats getDHTStats() {
        return stats;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getVendor()
     */
    public int getVendor() {
        return getLocalNode().getVendor();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getVersion()
     */
    public int getVersion() {
        return getLocalNode().getVersion();
    }
    
    /**
     * Returns the master public key
     */
    public PublicKey getMasterKey() {
        if (masterKeyPair != null) {
            return masterKeyPair.getPublic();
        }
        return null;
    }
    
    /**
     * Returns the master key pair (public + private key)
     */
    public KeyPair getMasterKeyPair() {
        return masterKeyPair;
    }
    
    /**
     * Sets the master key pair
     */
    public void setMasterKeyPair(KeyPair masterKeyPair) {
        this.masterKeyPair = masterKeyPair;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getLocalNode()
     */
    public LocalContact getLocalNode() {
        return (LocalContact)getRouteTable().getLocalNode();
    }
    
    /**
     * Generates a new random Node ID for the local Node, 
     * rebuild the routing table with this new ID and purge 
     * the database (it doesn't make sense to keep the key-values
     * from our old node ID).
     * 
     * WARNING: Meant to be called only by BootstrapManager 
     *          or MojitoFactory!
     */
    public void changeNodeID() {
        
        KUID newID = KUID.createRandomID();
        
        if (LOG.isInfoEnabled()) {
            LOG.info("Changing local Node ID from " + getLocalNodeID() + " to " + newID);
        }
        
        setLocalNodeID(newID);
        purgeDatabase(true);
    }
    
    /**
     * Rebuilds the routeTable with the given local node ID.
     * This will effectively clear the route table and
     * re-add any previous node in the MRS order.
     * 
     * @param localNodeID the local node's KUID
     */
    private void setLocalNodeID(KUID localNodeID) {
        RouteTable routeTable = getRouteTable();
        synchronized (routeTable) {
            // Change the Node ID
            getLocalNode().setNodeID(localNodeID);
            routeTable.rebuild();
            
            assert(getLocalNode().equals(routeTable.get(localNodeID)));
        }
    }
    
    /**
     * Returns true if the given Contact is equal to the
     * local Node.
     */
    public boolean isLocalNode(Contact node) {
        return node.equals(getLocalNode());
    }
    
    /**
     * Returns true if the given KUID and SocketAddress are
     * equal to local Node's KUID and SocketAddress (contact address)
     */
    public boolean isLocalNode(KUID nodeId, SocketAddress addr) {
        return isLocalNodeID(nodeId) && isLocalContactAddress(addr);
    }
    
    /**
     * Returns true if the given KUID is equal to local Node's KUID
     */
    public boolean isLocalNodeID(KUID nodeId) {
        return nodeId != null && nodeId.equals(getLocalNodeID());
    }
    
    /**
     * Returns true if the given SocketAddress is equal to local 
     * Node's SocketAddress
     */
    public boolean isLocalContactAddress(SocketAddress address) {
        return getContactAddress().equals(address);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getLocalNodeID()
     */
    public KUID getLocalNodeID() {
        return getLocalNode().getNodeID();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setMessageDispatcher(java.lang.Class)
     */
    public synchronized MessageDispatcher setMessageDispatcher(Class<? extends MessageDispatcher> clazz) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MessageDispatcher while " + getName() + " is running");
        }

        if (clazz == null) {
            clazz = MessageDispatcherImpl.class;
        }
        
        try {
            Constructor<? extends MessageDispatcher> c = clazz.getConstructor(Context.class);
            c.setAccessible(true);
            messageDispatcher = c.newInstance(this);
            
            messageDispatcher.addMessageDispatcherListener(
                    new NetworkStatisticContainer.Listener(networkStats));
            
            return messageDispatcher;
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
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
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setRouteTable(com.limegroup.mojito.routing.RouteTable)
     */
    public synchronized void setRouteTable(RouteTable routeTable) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch RouteTable while " + getName() + " is running");
        }

        if (this.routeTable != null && this.routeTable == routeTable) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot set the same instance multiple times");
            }
            //throw new IllegalArgumentException();
            return;
        }
        
        if (routeTable == null) {
            routeTable = new RouteTableImpl();
        }
        
        routeTable.setPingCallback(this);
        routeTable.addRouteTableListener(new RoutingStatisticContainer.Listener());
        
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

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setDatabase(com.limegroup.mojito.db.Database)
     */
    public synchronized void setDatabase(Database database) {
        setDatabase(database, true);
    }
    
    /**
     * Sets the Database
     * 
     * @param database The Database (can be null to use the default Database implemetation)
     * @param remove Whether or not to remove non local DHTValues
     */
    private synchronized void setDatabase(Database database, boolean remove) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch Database while " + getName() + " is running");
        }
        
        if (this.database != null && this.database == database) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot set the same instance multiple times");
            }
            //throw new IllegalArgumentException();
            return;
        }
        
        if (database == null) {
            database = new DatabaseImpl(
                    DatabaseSettings.MAX_DATABASE_SIZE.getValue(), 
                    DatabaseSettings.MAX_VALUES_PER_KEY.getValue());
        }
        
        this.database = database;
        purgeDatabase(remove);
    }
    
    /**
     * Purge Database makes sure the originator of all local DHTValues 
     * is the LocalContact and that all non local DHTValues get removed
     * from the Database if they're expired or if <tt>remove</tt> is
     * true.
     * 
     * @param remove Whether or not to remove non local DHTValues
     */
    private void purgeDatabase(boolean remove) {
        synchronized (database) {
            Contact localNode = getLocalNode();
            int oldValueCount = database.getValueCount();
            int removedCount = 0;
            for (DHTValue value : database.values()) {
                if (value.isLocalValue()) {
                    // Make sure all local DHTValues have the
                    // local Node as the originator
                    DHTValueFactory.setCreator(value, localNode);
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
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setMessageFactory(com.limegroup.mojito.messages.MessageFactory)
     */
    public synchronized void setMessageFactory(MessageFactory messageFactory) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MessageFactory while " + getName() + " is running");
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
            throw new IllegalStateException("Cannot switch MessageHelper while " + getName() + " is running");
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
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setHostFilter(com.limegroup.mojito.util.HostFilter)
     */
    public void setHostFilter(HostFilter hostFilter) {
        database.setHostFilter(hostFilter);
        messageDispatcher.setFilter(hostFilter);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setExternalPort(int)
     */
    public void setExternalPort(int port) {
        getLocalNode().setExternalPort(port);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getExternalPort()
     */
    public int getExternalPort() {
        return getLocalNode().getExternalPort();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getContactAddress()
     */
    public SocketAddress getContactAddress() {
        return getLocalNode().getContactAddress();
    }
    
    /**
     * Sets the contact address of the local Node. Effectively
     * we're maybe only using the port number.
     */
    public void setContactAddress(SocketAddress externalAddress) {
        getLocalNode().setContactAddress(externalAddress);
    }
    
    /**
     * Sets the local Node's external address (the address other are 
     * seeing if this Node is behind a NAT router)
     */
    public void setExternalAddress(SocketAddress externalSocketAddress) {
        getLocalNode().setExternalAddress(externalSocketAddress);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getLocalAddress()
     */
    public SocketAddress getLocalAddress() {
        return getLocalNode().getSourceAddress();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#isFirewalled()
     */
    public boolean isFirewalled() {
        return getLocalNode().isFirewalled();
    }

    /**
     * Returns whether or not the MessageDispatcher has
     * an open DatagramChannel
     */
    public boolean isOpen() {
        return messageDispatcher.isOpen();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#setDHTExecutorService(com.limegroup.mojito.concurrent.DHTExecutorService)
     */
    public void setDHTExecutorService(DHTExecutorService executorService) {
        if (executorService == null) {
            executorService = new DefaultDHTExecutorService();
        }
        
        this.executorService = executorService;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getDHTExecutorService()
     */
    public DHTExecutorService getDHTExecutorService() {
        return executorService;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#isRunning()
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
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#isBootstrapped()
     */
    public synchronized boolean isBootstrapped() {
        return isRunning() && bootstrapManager.isBootstrapped();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#bind(int)
     */
    public synchronized void bind(int port) throws IOException {
        bind(new InetSocketAddress(port));
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#bind(java.net.InetAddress, int)
     */
    public synchronized void bind(InetAddress addr, int port) throws IOException {
        bind(new InetSocketAddress(addr, port));
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#bind(java.net.SocketAddress)
     */
    public synchronized void bind(SocketAddress localAddress) throws IOException {
        if (isOpen()) {
            throw new IOException(getName() + " is already bound");
        }
        
        int port = ((InetSocketAddress)localAddress).getPort();
        if (port == 0) {
            throw new IllegalArgumentException("Cannot bind Socket to Port " + port);
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Binding " + getName() + " to address: " + localAddress);
        }
        
        // If we not firewalled and the external port has not 
        // been set yet then set it to the same port as the 
        // local address.
        if (!isFirewalled() && getExternalPort() == 0) {
            setExternalPort(((InetSocketAddress)localAddress).getPort());
        }
        
        getLocalNode().setSourceAddress(localAddress);
        getLocalNode().nextInstanceID();
        
        messageDispatcher.bind(localAddress);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#start()
     */
    public synchronized void start() {
        if (!isOpen()) {
            throw new IllegalStateException(getName() + " is not bound");
        }
        
        if (isRunning()) {
            LOG.error(getName() + " is already running!");
            return;
        }
        
        // Startup the local Node
        getLocalNode().shutdown(false);
        
        executorService.start();
        
        pingManager.init();
        findNodeManager.init();
        findValueManager.init();
        
        running = true;
        
        estimator = new DHTSizeEstimator();
        messageDispatcher.start();
        bucketRefresher.start();
        valueManager.start();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#stop()
     */
    public synchronized void stop() {
        if (!isRunning()) {
            return;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Stopping " + name);
        }
        
        // Shutdown the local Node
        Contact localNode = getLocalNode();
        localNode.shutdown(true);
        
        if (isBootstrapped()) {
            // We're nice guys and send shutdown messages to the 2*k-closest
            // Nodes which should help to reduce the overall latency.
            int m = KademliaSettings.SHUTDOWN_MULTIPLIER.getValue();
            int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
            int count = m*k;
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending shutdown message to " + count + " Nodes");
            }
            
            List<Contact> nodes = getRouteTable().select(localNode.getNodeID(), count, true);
            
            for (Contact node : nodes) {
                if (!node.equals(localNode)) {
                    // We are not interested in the responses as we're going
                    // to shutdown. Send pings without tagging the MessageIDs
                    // and to register a response handler for the pings.
                    RequestMessage request = getMessageFactory()
                        .createPingRequest(localNode, MessageID.createWithSocketAddress(null));
                    
                    try {
                        messageDispatcher.send(node, request, null);
                    } catch (IOException err) {
                        err.printStackTrace();
                        LOG.error("IOException", err);
                    }
                }
            }
        }
        
        running = false;
        
        bucketRefresher.stop();
        valueManager.stop();
        
        executorService.stop();
        messageDispatcher.stop();
        
        bootstrapManager.setBootstrapped(false);
        
        estimator.clear();
    }
    
    /**
     * Returns a Set of active Contacts sorted by most recently
     * seen to least recently seen
     */
    private Set<Contact> getActiveContacts() {
        Set<Contact> nodes = new LinkedHashSet<Contact>();
        List<Contact> contactList = getRouteTable().getActiveContacts();
        Collections.sort(contactList, BucketUtils.MRS_COMPARATOR);
        nodes.addAll(contactList);
        nodes.remove(getLocalNode());
        return nodes;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#ping()
     */
    public DHTFuture<PingResult> findActiveContact() {
        return pingManager.ping(getActiveContacts());
    }
    
    /**
     * Tries to ping a Set of hosts
     */
    public DHTFuture<PingResult> ping(Set<SocketAddress> hosts) {
        return pingManager.pingAddresses(hosts);
    }
    
    /**
     * Pings the DHT node with the given SocketAddress. 
     * 
     * @param address The address of the remote Node
     */
    public DHTFuture<PingResult> ping(SocketAddress address) {
        return pingManager.ping(address);
    }
    
    /** 
     * Pings the given Node 
     */
    public DHTFuture<PingResult> ping(Contact node) {
        return pingManager.ping(node);
    }
    
    /** 
     * Sends a special collision test Ping to the given Node 
     */
    public DHTFuture<PingResult> collisionPing(Contact node) {
        return pingManager.collisionPing(node);
    }
    
    /** 
     * Sends a special collision test Ping to the given Node 
     */
    public DHTFuture<PingResult> collisionPing(Set<Contact> nodes) {
        return pingManager.collisionPing(nodes);
    }
    
    /** 
     * Starts a value lookup for the given KUID 
     */
    public DHTFuture<FindValueResult> get(KUID key) {
        if(!isBootstrapped()) {
            throw new NotBootstrappedException(getName(), "get()");
        }
        
        return findValueManager.lookup(key);
    }
    
    /** 
     * Retrieve all DHTValues from the remote Node that are 
     * stored under the given valueId and nodeIds
     */
    public DHTFuture<Collection<DHTValue>> get(Contact node, KUID valueId, KUID nodeId) {
        return get(node, valueId, Collections.singleton(nodeId));
    }
    
    /** 
     * Retrieve all DHTValues from the remote Node that are 
     * stored under the given valueId and nodeIds
     */
    public DHTFuture<Collection<DHTValue>> get(Contact node, 
            KUID valueId, Collection<KUID> nodeIds) {
        return getValueManager.get(node, valueId, nodeIds);
    }
    
    /** 
     * Starts a Node lookup for the given KUID 
     */
    public DHTFuture<FindNodeResult> lookup(KUID lookupId) {
        return findNodeManager.lookup(lookupId);
    }
    
    public DHTFuture<BootstrapResult> bootstrap(Contact node) {
        return bootstrapManager.bootstrap(node);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#put(com.limegroup.mojito.KUID, byte[])
     */
    public DHTFuture<StoreResult> put(KUID key, DHTValueType type, int version, byte[] value) {
        if(!isBootstrapped()) {
            throw new NotBootstrappedException(getName(), "put()");
        }
        
        DHTValue dhtValue = DHTValueFactory
            .createLocalValue(getLocalNode(), key, type, version, value);
        database.store(dhtValue);
        return store(dhtValue);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#remove(com.limegroup.mojito.KUID)
     */
    public DHTFuture<StoreResult> remove(KUID key) {
        if(!isBootstrapped()) {
            throw new NotBootstrappedException(getName(), "remove()");
        }
        
        // To remove a KeyValue you just store an empty value!
        return put(key, DHTValueType.BINARY, 0, DHTValue.EMPTY_DATA);
    }
    
    /** 
     * Stores the given DHTValue 
     */
    public DHTFuture<StoreResult> store(DHTValue value) {
        if(!isBootstrapped()) {
            throw new NotBootstrappedException(getName(), "store()");
        }
        
        return storeManager.store(value);
    }
   
    /**
     * Stores a Collection of DHTValue(s). All values must have the same
     * valueId!
     */
    public DHTFuture<StoreResult> store(Collection<? extends DHTValue> values) {
        if(!isBootstrapped()) {
            throw new NotBootstrappedException(getName(), "store()");
        }
        
        return storeManager.store(values);
    }
    
    /** 
     * Stores the given DHTValue 
     */
    public DHTFuture<StoreResult> store(Contact node, QueryKey queryKey, DHTValue value) {
        return store(node, queryKey, Collections.singleton(value));
    }
    
    /**
     * Stores a Collection of DHTValue(s) at the given Node. 
     * All values must have the same valueId!
     */
    public DHTFuture<StoreResult> store(Contact node, QueryKey queryKey, 
            Collection<? extends DHTValue> values) {
        
        return storeManager.store(node, queryKey, values);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#keySet()
     */
    public Set<KUID> keySet() {
        return getDatabase().keySet();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#getValues()
     */
    public Collection<DHTValue> getValues() {
        Database database = getDatabase();
        synchronized (database) {
            return new ArrayList<DHTValue>(database.values());
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#size()
     */
    public synchronized BigInteger size() {
        if (!isRunning()) {
            return BigInteger.ZERO;
        }
        
        return estimator.getEstimatedSize(getRouteTable());
    }
    
    /**
     * Adds the approximate DHT size as returned by a remote Node.
     * The average of the remote DHT sizes is incorporated into
     * our local computation.
     */
    public void addEstimatedRemoteSize(BigInteger remoteSize) {
    	estimator.addEstimatedRemoteSize(remoteSize);
    }
    
    /**
     * Updates the approxmiate DHT size based on the given Contacts
     */
    public void updateEstimatedSize(Collection<? extends Contact> nodes) {
        estimator.updateSize(nodes);
    }
    
    /**
     * Returns the StatisticsContainer for Network statistics
     */
    public NetworkStatisticContainer getNetworkStats() {
        return networkStats;
    }
    
    /**
     * Returns the StatisticsContainer for lookup statistics
     */
    public GlobalLookupStatisticContainer getGlobalLookupStats() {
        return globalLookupStats;
    }
    
    /**
     * Returns the StatisticsContainer for Database statistics
     */
    public DatabaseStatisticContainer getDatabaseStats() {
        return databaseStats;
    }
    
    /**
     * A default implementation of the ThreadFactory.
     */
    private class DefaultThreadFactory implements ThreadFactory {
        
        private AtomicInteger number = new AtomicInteger(0);
        
        public Thread newThread(Runnable r) {
            return new Thread(r, getName() + "-" + number.getAndIncrement());
        }
    }
    
    /**
     * A default implementation of DHTExecutorService
     */
    private class DefaultDHTExecutorService implements DHTExecutorService {
        
        private volatile ThreadFactory threadFactory = new DefaultThreadFactory();
        
        private ScheduledExecutorService scheduledExecutor;
        
        private ExecutorService cachedExecutor;
        
        /*
         * (non-Javadoc)
         * @see com.limegroup.mojito.concurrent.DHTExecutorService#start()
         */
        public void start() {
            initScheduledExecutor();
            initCachedExecutor();
        }
        
        /*
         * (non-Javadoc)
         * @see com.limegroup.mojito.concurrent.DHTExecutorService#stop()
         */
        public void stop() {
            scheduledExecutor.shutdownNow();
            cachedExecutor.shutdownNow();
        }

        /**
         * Initializes Context's scheduled Executor
         */
        private void initScheduledExecutor() {
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
        
        /**
         * Initializes Context's (regular) Executor
         */
        private void initCachedExecutor() {
            ThreadFactory factory = new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread thread = getThreadFactory().newThread(r);
                    thread.setName(getName() + "-ContextCachedThreadPool");
                    thread.setDaemon(true);
                    return thread;
                }
            };
            
            cachedExecutor = Executors.newCachedThreadPool(factory);
        }
        
        /*
         * (non-Javadoc)
         * @see com.limegroup.mojito.concurrent.DHTExecutorService#getThreadFactory()
         */
        public ThreadFactory getThreadFactory() {
            return threadFactory;
        }

        /*
         * (non-Javadoc)
         * @see com.limegroup.mojito.concurrent.DHTExecutorService#setThreadFactory(java.util.concurrent.ThreadFactory)
         */
        public void setThreadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
        }

        /*
         * (non-Javadoc)
         * @see com.limegroup.mojito.MojitoDHT#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
         */
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, 
                long delay, long period, TimeUnit unit) {
            return scheduledExecutor.scheduleAtFixedRate(command, delay, period, unit);
        }
        
        /*
         * (non-Javadoc)
         * @see com.limegroup.mojito.MojitoDHT#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
         */
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, 
                long initialDelay, long delay, TimeUnit unit) {
            return scheduledExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }
        
        /*
         * (non-Javadoc)
         * @see com.limegroup.mojito.MojitoDHT#schedule(java.util.concurrent.Callable, long, java.util.concurrent.TimeUnit)
         */
        public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
            return scheduledExecutor.schedule(task, delay, unit);
        }
        
        /*
         * (non-Javadoc)
         * @see com.limegroup.mojito.MojitoDHT#submit(java.util.concurrent.Callable)
         */
        public <V> Future<V> submit(Callable<V> task) {
            return cachedExecutor.submit(task);
        }
        
        /*
         * (non-Javadoc)
         * @see com.limegroup.mojito.MojitoDHT#execute(java.lang.Runnable)
         */
        public void execute(Runnable command) {
            cachedExecutor.execute(command);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.MojitoDHT#store(java.io.OutputStream)
     */
    public void store(OutputStream out) throws IOException {
        MojitoFactory.store(this, out);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append("Local Node: ").append(getLocalNode()).append("\n");
        buffer.append("Is running: ").append(isRunning()).append("\n");
        buffer.append("Is bootstrapped/ing: ").append(isBootstrapped()).append("/")
                                            .append(isBootstrapping()).append("\n");
        buffer.append("Database Size (Keys): ").append(getDatabase().getKeyCount()).append("\n");
        buffer.append("Database Size (Values): ").append(getDatabase().getValueCount()).append("\n");
        buffer.append("RouteTable Size: ").append(getRouteTable().size()).append("\n");
        buffer.append("Estimated DHT Size: ").append(size()).append("\n");
        
        return buffer.toString();
    }
}