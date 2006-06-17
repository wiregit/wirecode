/*
 * Mojito Distributed Hash Tabe (DHT)
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.db.KeyValuePublisher;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.event.LookupListener;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.event.StoreListener;
import com.limegroup.mojito.handler.response.LookupResponseHandler;
import com.limegroup.mojito.handler.response.PingResponseHandler;
import com.limegroup.mojito.handler.response.StoreResponseHandler;
import com.limegroup.mojito.handler.response.LookupResponseHandler.ContactNodeEntry;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.io.MessageDispatcherImpl;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.messages.MessageHelper;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.routing.PatriciaRouteTable;
import com.limegroup.mojito.routing.RandomBucketRefresher;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.security.CryptoHelper;
import com.limegroup.mojito.settings.ContextSettings;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.settings.RouteTableSettings;
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
    
    private static final int VENDOR = ContextSettings.getVendorID();
    private static final int VERSION = 0;
    
    private static Timer TIMER = new Timer(true);
    
    private KeyPair masterKeyPair;
    
    private ContactNode localNode;
    private SocketAddress localAddress;
    private SocketAddress tmpExternalAddress;

    private final Object keyPairLock = new Object();
    private volatile KeyPair keyPair;
    
    private Database database;
    private RouteTable routeTable;
    private MessageDispatcher messageDispatcher;
    private MessageHelper messageHelper;
    private KeyValuePublisher keyValuePublisher;
    private RandomBucketRefresher bucketRefresher;
    
    private PingManager pingManager;
    private LookupManager lookupManager;
    
    private volatile boolean bootstrapping = false;
    private boolean running = false;
    
    private DHTStats dhtStats = null;
    
    private final NetworkStatisticContainer networkStats;
    private final GlobalLookupStatisticContainer globalLookupStats;
    private final DatabaseStatisticContainer databaseStats;
    
    private long lastEstimateTime = 0L;
    private int estimatedSize = 0;
    
    private LinkedList<Integer> localSizeHistory = new LinkedList<Integer>();
    private LinkedList<Integer> remoteSizeHistory = new LinkedList<Integer>();
    
    private ProcessingQueue eventQueue;
    
    private ThreadFactory threadFactory = new DefaultThreadFactory();
    
    private String name;
    
    public Context(String name, ContactNode localNode, KeyPair keyPair) {
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
        routeTable = new PatriciaRouteTable(this);
        
        messageDispatcher = new MessageDispatcherImpl(this);
        messageHelper = new MessageHelper(this);
        keyValuePublisher = new KeyValuePublisher(this);

        bucketRefresher = new RandomBucketRefresher(this);
        
        pingManager = new PingManager();
        lookupManager = new LookupManager();
        
        // Add the local to the RouteTable
        localNode.setTimeStamp(Long.MAX_VALUE);
        routeTable.add(localNode, false);
    }

    public String getName() {
        return name;
    }
    
    /**
     * Installs a custom MessageDispatcher implementation. The
     * passed Class must be a subclass of MessageDispatcher.
     */
    public synchronized void setMessageDispatcher(Class clazz) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot switch MessageDispatcher while DHT is running");
        }

        if (clazz == null) {
            clazz = MessageDispatcherImpl.class;
        }
        
        try {
            Constructor c = clazz.getConstructor(new Class[]{Context.class});
            messageDispatcher = (MessageDispatcher)c.newInstance(new Object[]{this});
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
    
    public DHTStats getDHTStats() {
        return dhtStats;
    }
    
    public int getVendor() {
        return VENDOR;
    }
    
    public int getVersion() {
        return VERSION;
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
    
    public ContactNode getLocalNode() {
        return localNode;
    }
    
    public boolean isLocalNode(ContactNode node) {
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
        if(localNode != null) {
            return localNode.isFirewalled();
        } else {
            return false;
        }
    }
    
    public void setFirewalled(boolean firewalled) {
        if (localNode != null) {
            localNode.setFirewalled(firewalled);
        }
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
        return keyValuePublisher;
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
    
    public void setBootstrapping(boolean bootstrapped) {
        this.bootstrapping = bootstrapped;
    }
    
    public boolean isBootstrapping() {
        return (isRunning() && bootstrapping);
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
    public void bind(SocketAddress localAddress) throws IOException {
        if (isOpen()) {
            throw new IOException("DHT is already bound");
        }
        
        this.localAddress = localAddress;
        
        localNode.setSocketAddress(localAddress);
        int instanceId = (localNode.getInstanceID() + 1) % 0xFF;
        localNode.setInstanceID(instanceId);
        
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
        
        pingManager.init();
        lookupManager.init();
        
        running = true;

        eventQueue = new ProcessingQueue(getName() + "-EventDispatcher", true);
        
        Thread keyValuePublisherThread 
            = getThreadFactory().createThread(keyValuePublisher, getName() + "-KeyValuePublisherThread");
        keyValuePublisherThread.setDaemon(true);
        
        Thread messageDispatcherThread 
            = getThreadFactory().createThread(messageDispatcher, getName() + "-MessageDispatcherThread");
        messageDispatcherThread.setDaemon(true);
    
        long bucketRefreshTime = RouteTableSettings.BUCKET_REFRESH_TIME.getValue();
        scheduleAtFixedRate(bucketRefresher, bucketRefreshTime , bucketRefreshTime);
        
        keyValuePublisherThread.start();
        messageDispatcherThread.start();
    }
    
    /**
     * Stops the DHT
     */
    public synchronized void stop() {
        if (!isRunning()) {
            return;
        }
        
        running = false;
        bootstrapping = false;
        
        keyValuePublisher.stop();
        
        eventQueue.clear();
        
        if (bucketRefresher != null) {
            bucketRefresher.cancel();
            bucketRefresher = null;
        }
        
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
    public void scheduleAtFixedRate(final Runnable runnable, long delay, long period) {
        TIMER.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                runnable.run();
            }
        }, delay, period);
    }
    
    /**
     * Fire's an Event
     */
    public void fireEvent(Runnable event) {
        if (!isRunning()) {
            LOG.error("Discarding Event as DHT is not running");
            return;
        }
        
        if (event == null) {
            LOG.error("Discarding Event as it is null");
            return;
        }

        eventQueue.add(event);
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
    
    /** Adds a global LookupListener */
    public void addLookupListener(LookupListener listener) {
        lookupManager.addLookupListener(listener);
    }
    
    /** Removes a global LookupListener */
    public void removeLookupListener(LookupListener listener) {
        lookupManager.removeLookupListener(listener);
    }
    
    /** Returns all global LookupListeners */
    public LookupListener[] getLookupListeners() {
        return lookupManager.getLookupListeners();
    }
    
    /**
     * Pings the DHT node with the given SocketAddress. 
     * Warning: This method should not be used to ping contacts from the routing table
     * 
     * @param address The address of the node
     * @param l the PingListener for incoming pongs
     * @throws IOException
     */
    public void ping(SocketAddress address) throws IOException {
        pingManager.ping(address);
    }
    
    /** Pings the given Node */
    public void ping(SocketAddress address, PingListener listener) throws IOException {
        pingManager.ping(address, listener);
    }
    
    /** Pings the given Node */
    public void ping(ContactNode node) throws IOException {
        pingManager.ping(node);
    }
    
    /** Pings the given Node */
    public void ping(ContactNode node, PingListener listener) throws IOException {
        pingManager.ping(node, listener);
    }
    
    /** Starts a value for the given KUID */
    public void get(KUID key, LookupListener listener) throws IOException {
        lookupManager.lookup(key, listener);
    }
    
    /** Starts a lookup for the given KUID */
    public void lookup(KUID lookup) throws IOException {
        lookupManager.lookup(lookup);
    }
    
    /** Starts a lookup for the given KUID */
    public void lookup(KUID lookup, LookupListener listener) throws IOException {
        lookupManager.lookup(lookup, listener);
    }
    
    /** Bootstraps this Node from the given Node */
    public void bootstrap(SocketAddress address, BootstrapListener listener) throws IOException {
        if(isBootstrapping()) return;
        setBootstrapping(true);
        new BootstrapManager().bootstrap(address, listener);
    }
    
    /**
     * Tries to bootstrap from a List of Hosts.
     * 
     * @param bootstrapHostsList a List of <tt>SocketAddress</tt>
     * @param listener The listener for bootstrap events
     * @throws IOException
     */
    public void bootstrap(List bootstrapHostsList, BootstrapListener listener) throws IOException {
        if(isBootstrapping()) return;
        setBootstrapping(true);
        new BootstrapManager().bootstrap(bootstrapHostsList, listener);
    }
    
    public void bootstrap(BootstrapListener listener) throws IOException {
        if(isBootstrapping()) return;
        setBootstrapping(true);
        new BootstrapManager().bootstrap(listener);
    }
    
    /** Stores a given KeyValue */
    public void store(KeyValue keyValue, StoreListener listener) throws IOException {
        new StoreManager().store(keyValue, listener);
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
        List nodes = routeTable.select(localNodeId, k, false, false);
        
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
            ContactNode node = (ContactNode)nodes.get(i);
            
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
                    Integer[] remote = (Integer[])remoteSizeHistory.toArray(new Integer[0]);
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
     * The BootstrapManager performs a lookup for the local Node ID
     * which is essentially the bootstrap process.
     */
    public class BootstrapManager implements PingListener, LookupListener {
        
        private long startTime = 0L;
        
        private boolean phaseTwo = false;
        private boolean foundNewNodes = false;
        
        private BootstrapListener listener;
        
        private List buckets = Collections.EMPTY_LIST;
        
        private List bootstrapHostsList;
        
        private int failures;
        
        private BootstrapManager() {  
        }
        
        private long time() {
            return System.currentTimeMillis() - startTime;
        }
        
        public void bootstrap(SocketAddress address, 
                BootstrapListener listener) throws IOException {
            this.listener = listener;

            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending initial bootstrap ping to: " + address);
            }
            
            startTime = System.currentTimeMillis();
            ping(address, this);
        }
        
        public void bootstrap(BootstrapListener listener) throws IOException {
            bootstrap(routeTable.getAllNodesMRS(), listener);
        }
        
        /**
         * Tries to bootstrap from a List of Hosts.
         * 
         * @param bootstrapHostsList a List of <tt>SocketAddress</tt>
         * @param listener The listener for bootstrap events
         * @throws IOException
         */
        public void bootstrap(List bootstrapHostsList, 
                BootstrapListener listener) throws IOException {
            
            this.listener = listener;
            this.bootstrapHostsList = bootstrapHostsList;
            
            if (!bootstrapHostsList.isEmpty()) {
                SocketAddress firstHost = (SocketAddress)bootstrapHostsList.remove(0);
                bootstrap(firstHost, listener);
            } else {
                fireNoBootstrapHost();
            }
        }
        
        public void response(ResponseMessage response, long time) {
            try {
                if (response instanceof PingResponse) {
                    lookup(getLocalNodeID(), this);
                }
            } catch (IOException err) {
                LOG.error("Bootstrap lookup failed: ", err);
                
                firePhaseOneFinished();
                firePhaseTwoFinished();
            }
        }

        public void timeout(KUID nodeId, SocketAddress address, RequestMessage request, long time) {
            if (request instanceof PingRequest) {
                
                networkStats.BOOTSTRAP_PING_FAILURES.incrementStat();
                if (LOG.isErrorEnabled()) {
                    LOG.error("Initial bootstrap ping timeout, failure " + failures);
                }
                
                failures++;
                if (failures < KademliaSettings.MAX_BOOTSTRAP_FAILURES.getValue()) {
                    if (bootstrapHostsList == null || bootstrapHostsList.isEmpty()) {
                        bootstrapHostsList = routeTable.getAllNodesMRS();
                    }
                    
                    for (Iterator iter = bootstrapHostsList.iterator(); iter.hasNext(); ) {
                        ContactNode node = (ContactNode) iter.next();
                        //do not send to ourselve or the node which just timed out
                        if(!node.getNodeID().equals(getLocalNodeID()) 
                                && !node.getSocketAddress().equals(address)) {
                            
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Retrying bootstrap ping with node: " + node);
                            }
                            
                            try {
                                iter.remove();
                                ping(node, this);
                                return;
                            } catch (IOException err) {
                                LOG.error(err);
                                fireNoBootstrapHost();
                            }
                        }
                    }
                    
                    fireNoBootstrapHost();
                    
                } else {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Initial bootstrap ping timeout, giving up bootstrap after " + failures + " tries");
                    }
                    
                    fireNoBootstrapHost();
                }
            }
        }
        
        public void found(KUID lookup, Collection c, long time) {
        }
        
        public void finish(KUID lookup, Collection c, long time) {
            if (!phaseTwo) {
                phaseTwo = true;
                firePhaseOneFinished();
                
                try {
                    routeTable.refreshBuckets(true, this);
                } catch (IOException err) {
                    LOG.error("Beginning second phase failed: ", err);
                    firePhaseTwoFinished();
                }
            } else {
                if (!c.isEmpty()) {
                    foundNewNodes = true;
                }
                
                buckets.remove(lookup);
                if (buckets.isEmpty()) {
                    firePhaseTwoFinished();
                }
            }
        }

        public void setBuckets(List buckets) {
            this.buckets = buckets;
            
            if (buckets.isEmpty()) {
                firePhaseTwoFinished();
            }
        }
        
        private void firePhaseOneFinished() {
            if (listener != null) {
                final long time = time();
                
                fireEvent(new Runnable() {
                    public void run() {
                        listener.phaseOneComplete(time);
                    }
                });
            }
        }
        
        private void firePhaseTwoFinished() {
            setBootstrapping(false);
            
            if (listener != null) {
                final long time = time();
                
                fireEvent(new Runnable() {
                    public void run() {
                        listener.phaseTwoComplete(foundNewNodes, time);
                    }
                });
            }
        }
        
        private void fireNoBootstrapHost() {
            if (listener != null) {
                fireEvent(new Runnable() {
                    public void run() {
                        listener.noBootstrapHost();
                    }
                });
            }
        }
    }
    
    /**
     * The StoreManager performs a lookup for the Value ID we're
     * going to store and stores the KeyValue at the k closest 
     * Nodes of the result set.
     */
    private class StoreManager implements LookupListener {
        
        private KeyValue keyValue;
        private StoreListener listener;
        
        private StoreManager() {
        }
        
        private void store(KeyValue keyValue, 
                StoreListener listener) throws IOException {
            this.keyValue = keyValue;
            this.listener = listener;
            
            KUID nodeId = ((KUID)keyValue.getKey()).toNodeID();
            lookup(nodeId, this);
        }
        
        public void response(ResponseMessage response, long time) {
        }

        public void timeout(KUID nodeId, SocketAddress address, 
                RequestMessage request, long time) {
        }
        
        public void found(KUID lookup, Collection c, long time) {
            
        }
        
        public void finish(KUID lookup, Collection c, long time) {
            // List of ContactNodes where we stored the KeyValues.
            final List<ContactNode> storeTargets = new ArrayList<ContactNode>(c.size());
            
            for(Iterator it = c.iterator(); it.hasNext(); ) {
                ContactNodeEntry entry = (ContactNodeEntry)it.next();
                ContactNode node = entry.getContactNode();
                QueryKey queryKey = entry.getQueryKey();
                
                if (isLocalNodeID(node.getNodeID())) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Skipping local Node as KeyValue is already stored at this Node");
                    }
                    storeTargets.add(node);
                    continue;
                }
                
                if (queryKey == null) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Cannot store " + keyValue + " at " 
                                + node + " because we have no QueryKey for it");
                    }
                    continue;
                }
                
                try {
                    new StoreResponseHandler(Context.this, queryKey, keyValue).store(node);
                    storeTargets.add(node);
                } catch (IOException err) {
                    LOG.error("Failed to store KeyValue", err);
                }
            }
            
            keyValue.setNumLocs(storeTargets.size());
            
            if (listener != null) {
                fireEvent(new Runnable() {
                    public void run() {
                        listener.store(keyValue, storeTargets);
                    }
                });
            }
        }
    }
    
    /**
     * The PingManager takes care of concurrent Pings and makes sure
     * a single Node cannot be pinged multiple times.
     */
    private class PingManager implements PingListener {
        
        private Map handlerMap = new HashMap();
        private List listeners = new ArrayList();
        
        private PingManager() {
        }
        
        public void init() {
            synchronized (handlerMap) {
                handlerMap.clear();
            }
        }
        
        public void addPingListener(PingListener listener) {
            synchronized (listeners) {
                listeners.add(listener);
            }
        }
        
        public void removePingListener(PingListener listener) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }

        public PingListener[] getPingListeners() {
            synchronized (listeners) {
                return (PingListener[])listeners.toArray(new PingListener[0]);
            }
        }
        
        public void ping(SocketAddress address) throws IOException {
            ping(null, address, null);
        }

        public void ping(SocketAddress address, PingListener listener) throws IOException {
            ping(null, address, listener);
        }

        public void ping(ContactNode node) throws IOException {
            ping(node.getNodeID(), node.getSocketAddress(), null);
        }
        
        public void ping(ContactNode node, PingListener listener) throws IOException {
            ping(node.getNodeID(), node.getSocketAddress(), listener);
        }

        public void ping(KUID nodeId, SocketAddress address, PingListener listener) throws IOException {
            synchronized (handlerMap) {
                PingResponseHandler responseHandler = (PingResponseHandler)handlerMap.get(address);
                if (responseHandler == null) {
                    
                    responseHandler = new PingResponseHandler(Context.this);
                    responseHandler.addPingListener(this);
                    
                    if (listener != null) {
                        responseHandler.addPingListener(listener);
                    }
                    
                    PingRequest request = getMessageHelper().createPingRequest(address);
                    messageDispatcher.send(nodeId, address, request, responseHandler);

                    handlerMap.put(address, responseHandler);
                    networkStats.PINGS_SENT.incrementStat();
                    
                } else if (listener != null) {
                    responseHandler.addPingListener(listener);
                }
            }
        }
        
        public void response(final ResponseMessage response, final long time) {
            networkStats.PINGS_OK.incrementStat();
            
            synchronized (handlerMap) {
                handlerMap.remove(response.getContactNode().getSocketAddress());
                fireEvent(new Runnable() {
                    public void run() {
                        synchronized (listeners) {
                            for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                                PingListener listener = (PingListener)it.next();
                                listener.response(response, time);
                            }
                        }
                    }
                });
            }
        }

        public void timeout(final KUID nodeId, final SocketAddress address, RequestMessage request, final long time) {            
            networkStats.PINGS_FAILED.incrementStat();
            
            synchronized (handlerMap) {
                handlerMap.remove(address);
                fireEvent(new Runnable() {
                    public void run() {
                        synchronized (listeners) {
                            for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                                PingListener listener = (PingListener)it.next();
                                listener.timeout(nodeId, address, null, time);
                            }
                        }
                    }
                });
            }
        }

        public boolean cancel(ContactNode node) {
            return cancel(node.getSocketAddress());
        }
        
        public boolean cancel(SocketAddress address) {
            synchronized (handlerMap) {
                PingResponseHandler handler 
                    = (PingResponseHandler)handlerMap.remove(address);
                if (handler != null) {
                    handler.stop();
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * The LookupManager takes care of multiple concurrent lookups
     */
    private class LookupManager implements LookupListener {
        
        private Map handlerMap = new HashMap();
        
        private List listeners = new ArrayList();
        
        private LookupManager() {
            
        }
        
        public void init() {
            synchronized (handlerMap) {
                handlerMap.clear();
            }
        }
        
        public void addLookupListener(LookupListener listener) {
            synchronized (listeners) {
                listeners.add(listener);
            }
        }
        
        public void removeLookupListener(LookupListener listener) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }

        public LookupListener[] getLookupListeners() {
            synchronized (listeners) {
                return (LookupListener[])listeners.toArray(new LookupListener[0]);
            }
        }
        
        public void lookup(KUID lookup) throws IOException {
            lookup(lookup, null);
        }
        
        public void lookup(KUID lookup, LookupListener listener) throws IOException {
            if (!lookup.isNodeID() && !lookup.isValueID()) {
                throw new IllegalArgumentException("Lookup ID must be either a NodeID or ValueID");
            }
            
            synchronized (handlerMap) {
                LookupResponseHandler handler = (LookupResponseHandler)handlerMap.get(lookup);
                if (handler == null) {
                    handler = new LookupResponseHandler(lookup, Context.this);
                    handler.addLookupListener(this);
                    
                    if (listener != null) {
                        handler.addLookupListener(listener);
                    }
                    
                    handler.start();
                    handlerMap.put(lookup, handler);
                    
                } else if (listener != null) {
                    handler.addLookupListener(listener);
                }
            }
        }
        
        public boolean cancel(KUID lookup) {
            synchronized (handlerMap) {
                LookupResponseHandler handler = (LookupResponseHandler)handlerMap.remove(lookup);
                if (handler != null) {
                    handler.stop();
                    return true;
                }
            }
            return false;
        }

        public void response(final ResponseMessage response, final long time) {
            synchronized (handlerMap) {
                fireEvent(new Runnable() {
                    public void run() {
                        synchronized (listeners) {
                            for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                                LookupListener listener = (LookupListener)it.next();
                                listener.response(response, time);
                            }
                        }
                    }
                });
            }
        }

        public void timeout(final KUID nodeId, final SocketAddress address, 
                final RequestMessage request, final long time) {
            synchronized (handlerMap) {
                fireEvent(new Runnable() {
                    public void run() {
                        synchronized (listeners) {
                            for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                                LookupListener listener = (LookupListener)it.next();
                                listener.timeout(nodeId, address, request, time);
                            }
                        }
                    }
                });
            }
        }
        
        public void found(final KUID lookup, final Collection c, final long time) {
            synchronized (handlerMap) {
                handlerMap.remove(lookup);
                
                fireEvent(new Runnable() {
                    public void run() {
                        synchronized (listeners) {
                            for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                                LookupListener listener = (LookupListener)it.next();
                                listener.found(lookup, c, time);
                            }
                        }
                    }
                });
            }
        }
        
        public void finish(final KUID lookup, final Collection c, final long time) {
            synchronized (handlerMap) {
                handlerMap.remove(lookup);
                
                fireEvent(new Runnable() {
                    public void run() {
                        synchronized (listeners) {
                            for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                                LookupListener listener = (LookupListener)it.next();
                                listener.finish(lookup, c, time);
                            }
                        }
                    }
                });
            }
        }
    }
    
    /**
     * A default implementation of the ThreadFactory.
     */
    private static class DefaultThreadFactory implements ThreadFactory {
        public Thread createThread(Runnable runnable, String name) {
            return new Thread(runnable, name);
        }
    }
}
