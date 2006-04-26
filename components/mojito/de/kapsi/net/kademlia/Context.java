/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package de.kapsi.net.kademlia;

import java.io.File;
import java.io.IOException;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.DHTNodeStat;
import com.limegroup.gnutella.dht.statistics.DHTStats;
import com.limegroup.gnutella.dht.statistics.DataBaseStatisticContainer;
import com.limegroup.gnutella.dht.statistics.GlobalLookupStatisticContainer;
import com.limegroup.gnutella.dht.statistics.NetworkStatisticContainer;

import de.kapsi.net.kademlia.db.Database;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.db.KeyValuePublisher;
import de.kapsi.net.kademlia.event.BootstrapListener;
import de.kapsi.net.kademlia.event.EventDispatcher;
import de.kapsi.net.kademlia.event.FindNodeListener;
import de.kapsi.net.kademlia.event.FindValueListener;
import de.kapsi.net.kademlia.event.LookupListener;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.event.ResponseListener;
import de.kapsi.net.kademlia.event.StoreListener;
import de.kapsi.net.kademlia.handler.ResponseHandler;
import de.kapsi.net.kademlia.handler.response.FindNodeResponseHandler;
import de.kapsi.net.kademlia.handler.response.FindValueResponseHandler;
import de.kapsi.net.kademlia.handler.response.LookupResponseHandler2;
import de.kapsi.net.kademlia.handler.response.PingResponseHandler;
import de.kapsi.net.kademlia.handler.response.StoreResponseHandler;
import de.kapsi.net.kademlia.io.MessageDispatcher;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.MessageFactory;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.routing.PatriciaRouteTable;
import de.kapsi.net.kademlia.routing.RandomBucketRefresher;
import de.kapsi.net.kademlia.routing.RoutingTable;
import de.kapsi.net.kademlia.security.CryptoHelper;
import de.kapsi.net.kademlia.security.QueryKey;
import de.kapsi.net.kademlia.settings.ContextSettings;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.settings.RouteTableSettings;

public class Context implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(Context.class);
    
    private static final int VENDOR = 0xDEADBEEF;
    private static final int VERSION = 0;
    
    private PublicKey masterKey;
    
    private KUID nodeId;
    private SocketAddress localAddress;
    
    private SocketAddress tmpExternalAddress;
    private SocketAddress externalAddress;
    
    private KeyPair keyPair;
    
    private Database database;
    private RoutingTable routeTable;
    private MessageDispatcher messageDispatcher;
    private EventDispatcher eventDispatcher;
    private MessageFactory messageFactory;
    private KeyValuePublisher keyValuePublisher;
    private RandomBucketRefresher bucketRefresher;
    
    private PingManager pingContext;
    
    private volatile boolean bootstrapped = false;
    private boolean running = false;
    
    private final Timer scheduler = new Timer(true);
    private Thread messageDispatcherThread = null;
    
    private DHTStats dhtStats = null;
    
    private final NetworkStatisticContainer networkStats;
    private final GlobalLookupStatisticContainer globalLookupStats;
    private final DataBaseStatisticContainer dataBaseStats;
    
    private long lastEstimateTime = 0L;
    private int estimatedSize = 0;
    
    private LinkedList localSizeHistory = new LinkedList();
    private LinkedList remoteSizeHistory = new LinkedList();
    
    public Context() {
        
        try {
            File file = new File("public.key");
            if (file.exists() && file.isFile()) {
                masterKey = CryptoHelper.loadMasterKey(file);
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        dhtStats = new DHTNodeStat(this);
        
        networkStats = new NetworkStatisticContainer(this);
        globalLookupStats = new GlobalLookupStatisticContainer(this);
        dataBaseStats = new DataBaseStatisticContainer(this);
        
        keyPair = CryptoHelper.createKeyPair();

        database = new Database(this);
        routeTable = new PatriciaRouteTable(this);
        messageDispatcher = new MessageDispatcher(this);
        eventDispatcher = new EventDispatcher(this);
        messageFactory = new MessageFactory(this);
        keyValuePublisher = new KeyValuePublisher(this);
        bucketRefresher = new RandomBucketRefresher(this);
        
        pingContext = new PingManager();
        
        ResponseListener listener = new ResponseListener() {
            public void response(KUID nodeId, SocketAddress address, long time) {
                networkStats.RESPONSE_TIME.addData((int)time);
                
                // TODO mark contact as alive
            }

            public void timeout(KUID nodeId, SocketAddress address, long time) {
                if (nodeId != null) {
                    // TODO handle Node error
                }
            }
        };
        
        // pingContext.addResponseListener(listener);
        // lookupContext.addResponseListener(listener);
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
        return masterKey;
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
        ContactNode localNode = (ContactNode)routeTable.get(nodeId);
        if (localNode == null) {
            if (externalAddress != null) {
                localNode = new ContactNode(nodeId, externalAddress);
            } else {
                localNode = new ContactNode(nodeId, localAddress);
            }
        }
        return localNode;
    }
    
    public boolean isLocalNodeID(KUID nodeId) {
        return nodeId.equals(this.nodeId);
    }
    
    public KUID getLocalNodeID() {
        return nodeId;
    }
    
    public SocketAddress getSocketAddress() {
        return (externalAddress != null ? externalAddress : localAddress);
    }
    
    public SocketAddress getLocalSocketAddress() {
        return localAddress;
    }
    
    public SocketAddress getExternalSocketAddress() {
        return externalAddress;
    }
    
    public void setExternalSocketAddress(SocketAddress newExternalAddress) 
                throws IOException {
        if (newExternalAddress != null) {
            if (!newExternalAddress.equals(this.externalAddress)) {
                if (tmpExternalAddress == null) {
                    tmpExternalAddress = newExternalAddress;
                } else if (tmpExternalAddress.equals(newExternalAddress)) {
                    ContactNode localNode = (ContactNode)routeTable.get(nodeId);
                    if (localNode != null) {
                        this.externalAddress = newExternalAddress;
                        localNode.setSocketAddress(newExternalAddress);
                    }
                    this.tmpExternalAddress = null;
                }
            }
        }
    }
    
    public Database getDatabase() {
        return database;
    }
    
    public RoutingTable getRouteTable() {
        return routeTable;
    }
    
    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }
    
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }
    
    public KeyValuePublisher getPublisher() {
        return keyValuePublisher;
    }
    
    public MessageFactory getMessageFactory() {
        return messageFactory;
    }
    
    public boolean isRunning() {
        return running;
    }

    public boolean isOpen() {
        return messageDispatcher.isOpen();
    }
    
    public void setBootstrapped(boolean bootstrapped) {
        this.bootstrapped = bootstrapped;
    }
    
    public boolean isBootstrapped() {
        return isRunning() && bootstrapped;
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
    
    public void bind(SocketAddress address) throws IOException {
        if (isOpen()) {
            throw new IOException("DHT is already bound");
        }
        
        messageDispatcher.bind(address);
        this.localAddress = address;
        
        byte[] id = ContextSettings.getLocalNodeID(address);
        if (id == null) {
            nodeId = KUID.createRandomNodeID(address);
            ContextSettings.setLocalNodeID(address, nodeId.getBytes());
        } else {
            nodeId = KUID.createNodeID(id);
        }
        
        //add ourselve to the routing table
        ContactNode localNode = new ContactNode(nodeId, address);
        localNode.setTimeStamp(Long.MAX_VALUE);
        routeTable.add(localNode, false);
    }
    
    //TODO testing purposes only - remove
    public void bind(SocketAddress address,KUID localNodeID) throws IOException {
        if (isOpen()) {
            throw new IOException("DHT is already bound");
        }
        messageDispatcher.bind(address);
        this.localAddress = address;
        nodeId = localNodeID;
    }
    
    public void close() throws IOException {
        running = false;
        messageDispatcher.close();
        messageDispatcherThread = null;
    }
    
    public void run() {
        if (!isOpen()) {
            throw new RuntimeException("DHT is not bound");
        }
        
        if (isRunning()) {
            LOG.error("DHT is already running!");
            return;
        }
        
        pingContext.init();
        
        bootstrapped = true;
        running = true;
        try {
            
            // TODO use ManagedThread
            Thread keyValuePublisherThread 
                = new Thread(keyValuePublisher, "KeyValuePublisherThread");
            keyValuePublisherThread.setDaemon(true);
            
            scheduler.scheduleAtFixedRate(eventDispatcher, 0L, ContextSettings.DISPATCH_EVENTS_EVERY.getValue());
            
            long bucketRefreshTime = RouteTableSettings.BUCKET_REFRESH_TIME.getValue();
            scheduler.scheduleAtFixedRate(bucketRefresher, bucketRefreshTime , bucketRefreshTime);
            keyValuePublisherThread.start();

            messageDispatcherThread = Thread.currentThread();
            messageDispatcher.run();
            
        } finally {
            keyValuePublisher.stop();
            scheduler.cancel();
            
            running = false;
            bootstrapped = false;
            
            lastEstimateTime = 0L;
            estimatedSize = 0;
            localSizeHistory.clear();
            remoteSizeHistory.clear();
        }
    }

    public void fireEvent(Runnable event) {
        if (event == null) {
            LOG.error("Discarding Event as it is null");
            return;
        }
        
        if (!isRunning()) {
            LOG.error("Discarding Event as DHT is not running");
            return;
        }
        
        if (messageDispatcherThread != null 
                && Thread.currentThread() != messageDispatcherThread) {
            eventDispatcher.run(event);
        } else {
            eventDispatcher.add(event);
        }
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
        pingContext.ping(address, null);
    }
    
    public void ping(SocketAddress address, PingListener listener) throws IOException {
        pingContext.ping(address, listener);
    }
    
    public void ping(ContactNode node) throws IOException {
        pingContext.ping(node, null);
    }
    
    public void ping(ContactNode node, PingListener listener) throws IOException {
        pingContext.ping(node, listener);
    }
    
    public void lookup(KUID lookup, FindNodeListener l) throws IOException {
        FindNodeResponseHandler handler 
            = new FindNodeResponseHandler(this, lookup, l);
        
        handler.lookup();
    }
    
    public void bootstrap(SocketAddress address, BootstrapListener listener) throws IOException {
        setBootstrapped(false);
        ping(address, new BootstrapManager(address, listener));
    }
    
    /** store */
    public void store(KeyValue keyValue, StoreListener l) throws IOException {       
        lookup(keyValue.getKey(), new StoreManager(keyValue, l));
    }
    
    public void store(ContactNode node, QueryKey queryKey, List keyValues) throws IOException {
        if (queryKey == null) {
            throw new IllegalArgumentException("Cannot store KeyValues without a QueryKey");
        }
        
        ResponseHandler handler = new StoreResponseHandler(this, queryKey, keyValues);
        
        RequestMessage request 
            = messageFactory.createStoreRequest(node.getSocketAddress(), keyValues.size(), 
                    queryKey, Collections.EMPTY_LIST);
        
        messageDispatcher.send(node, request, handler);
    }
    
    public void get(KUID key, FindValueListener l) throws IOException {
        FindValueResponseHandler handler 
            = new FindValueResponseHandler(this, key, l);
        
        handler.lookup();
    }
    
    public int size() {
        if (!isRunning()) {
            return 0;
        }
        
        if ((System.currentTimeMillis() - lastEstimateTime) 
                >= ContextSettings.ESTIMATE_NETWORK_SIZE_EVERY.getValue()) {
            
            estimatedSize = getEstimatedSize();
            lastEstimateTime = System.currentTimeMillis();
        }
        
        return estimatedSize;
    }
    
    public void addEstimatedRemoteSize(int remoteSize) {
        if (remoteSize <= 0 || !ContextSettings.COUNT_REMOTE_SIZE.getValue()) {
            return;
        }
        
        synchronized (remoteSizeHistory) {
            remoteSizeHistory.add(new Integer(remoteSize));
            if (remoteSizeHistory.size() >= ContextSettings.MAX_REMOTE_HISTORY_SIZE.getValue()) {
                remoteSizeHistory.removeFirst();
            }
        }
    }
    
    private int getEstimatedSize() {
        KUID localNodeId = getLocalNodeID();
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        // TODO only live nodes?
        List nodes = routeTable.select(localNodeId, k, false, false);
        
        // TODO accoriding to Az code it works only with more than
        // two Nodes
        
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
        
        localSizeHistory.add(new Integer(estimatedSize));
        if (localSizeHistory.size() >= ContextSettings.MAX_LOCAL_HISTORY_SIZE.getValue()) {
            localSizeHistory.removeFirst();
        }
        
        int localSizeSum = 0;
        for(Iterator it = localSizeHistory.iterator(); it.hasNext(); ) {
            localSizeSum += ((Integer)it.next()).intValue();
        }
        
        // If somebody is playing around with MAX_HISTORY_SIZE
        // then localSizeHistory.size() might be zero which
        // would cause a div by zero error
        int localSize = (!localSizeHistory.isEmpty() ? localSizeSum/localSizeHistory.size() : 0);
        
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
    
    public DataBaseStatisticContainer getDataBaseStats() {
        return dataBaseStats;
    }
    
    private class BootstrapManager implements BootstrapListener, PingListener, FindNodeListener {
        
        private long totalTime = 0L;
        
        private SocketAddress address;
        private BootstrapListener listener;
        
        public BootstrapManager(SocketAddress address, BootstrapListener listener) {
            this.address = address;
            this.listener = listener;
        }
        
        public void pingSuccess(KUID nodeId, SocketAddress address, final long time) {
            totalTime += time;
            
            try {
                // Ping was successful, start with lookup...
                lookup(getLocalNodeID(), this);
            } catch (IOException err) {
                LOG.error(err);
                initialPhaseComplete(getLocalNodeID(), Collections.EMPTY_LIST, time);
            }
        }

        public void pingTimeout(KUID nodeId, SocketAddress address, long time) {
            initialPhaseComplete(getLocalNodeID(), Collections.EMPTY_LIST, -1L);
        }

        public void foundNodes(KUID lookup, Collection nodes, Map queryKeys, final long time) {
            
            totalTime += time;
            initialPhaseComplete(getLocalNodeID(), nodes, totalTime);
            
            try {
                // Begin with PHASE 2 of bootstrapping where we
                // refresh the furthest away buckets...
                routeTable.refreshBuckets(false, this);
            } catch (IOException err) {
                LOG.error(err);
                secondPhaseComplete(getLocalNodeID(), false, 0);
            }
        }

        public void initialPhaseComplete(final KUID nodeId, final Collection nodes, final long time) {
            if (listener != null) {
                fireEvent(new Runnable() {
                    public void run() {
                        listener.initialPhaseComplete(nodeId, nodes, time);
                    }
                });
            }
        }

        public void secondPhaseComplete(final KUID nodeId, final boolean foundNodes, final long time) {
            setBootstrapped(true);
            totalTime += time;
            networkStats.BOOTSTRAP_TIME.addData((int)totalTime);
            if (listener != null) {
                fireEvent(new Runnable() {
                    public void run() {
                        listener.secondPhaseComplete(nodeId, foundNodes, totalTime);
                    }
                });
            }
        }
    }
    
    private class StoreManager implements FindNodeListener {
        
        private KeyValue keyValue;
        private StoreListener listener;
        
        public StoreManager(KeyValue keyValue, StoreListener listener) {
            this.keyValue = keyValue;
            this.listener = listener;
        }
        
        public void foundNodes(KUID lookup, Collection nodes, Map queryKeys, long time) {
            try {
                List keyValues = Arrays.asList(new KeyValue[]{keyValue});
                
                // List of ContactNodes where we stored the KeyValues.
                List targets = new ArrayList(nodes.size());
                
                for(Iterator it = nodes.iterator(); it.hasNext(); ) {
                    ContactNode node = (ContactNode)it.next();
                    QueryKey queryKey = (QueryKey)queryKeys.get(node);
                    
                    if(node.getNodeID().equals(getLocalNodeID())) {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Skipping local Node as KeyValue is already stored at this Node");
                        }
                        continue;
                    }
                    
                    if (queryKey == null) {
                        if (LOG.isErrorEnabled()) {
                            LOG.error("Cannot store " + keyValues + " at " 
                                    + node + " because we have no QueryKey for it");
                        }
                        continue;
                    }
                    
                    store(node, queryKey, keyValues);
                    targets.add(node);
                }
                
                if (listener != null) {
                    listener.store(keyValues, targets);
                }
            } catch (IOException err) {
                LOG.error(err);
            }
        }
    }
    
    protected abstract class AbstractManager {
        
        private List listeners = new ArrayList();
        
        public void addResponseListener(ResponseListener listener) {
            synchronized (listeners) {
                listeners.add(listener);
            }
        }
        
        public void removePingListener(ResponseListener listener) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }

        public ResponseListener[] getAllResponseListeners() {
            synchronized (listeners) {
                return (ResponseListener[])listeners.toArray(new ResponseListener[0]);
            }
        }
        
        protected void fireResponse(final KUID nodeId, final SocketAddress address, final long time) {
            fireEvent(new Runnable() {
                public void run() {
                    synchronized (listeners) {
                        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                            ResponseListener listener = (ResponseListener)it.next();
                            listener.response(nodeId, address, time);
                        }
                    }
                }
            });
        }
        
        protected void fireTimeout(final KUID nodeId, final SocketAddress address, final long time) {
            fireEvent(new Runnable() {
                public void run() {
                    synchronized (listeners) {
                        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                            ResponseListener listener = (ResponseListener)it.next();
                            listener.timeout(nodeId, address, time);
                        }
                    }
                }
            });
        }
    }
    
    /**
     * The PingContext takes care of concurrent Pings and makes sure
     * a single Node cannot be pinged multiple times.
     */
    public class PingManager extends AbstractManager {
        
        private Map attachmentMap = new HashMap();
        private List listeners = new ArrayList();
        
        private PingManager() {
            
        }
        
        public void init() {
            synchronized (attachmentMap) {
                attachmentMap.clear();
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

        public PingListener[] getAllPingListeners() {
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
            synchronized (attachmentMap) {
                Attachment attachment = (Attachment)attachmentMap.get(address);
                if (attachment == null) {
                    
                    PingRequest request = messageFactory.createPingRequest(address);
                    ResponseHandler responseHandler = new PingResponseHandler(Context.this, this);
                    
                    attachment = new Attachment(responseHandler);
                    attachmentMap.put(address, attachment);
                    messageDispatcher.send(nodeId, address, request, responseHandler);
                    
                    networkStats.PINGS_SENT.incrementStat();
                }
                
                if (listener != null) {
                    attachment.listeners.add(listener);
                }
            }
        }
        
        public boolean cancel(ContactNode node) {
            return cancel(node.getSocketAddress());
        }
        
        public boolean cancel(SocketAddress address) {
            Attachment attachment = null;
            
            synchronized (attachmentMap) {
                attachment = (Attachment)attachmentMap.remove(address);
            }
            
            if (attachment != null) {
                // TODO implement cancel!
                return true;
            }
            return false;
        }
        
        public void handleSuccess(KUID nodeId, SocketAddress address, long time) {
            Attachment attachment = null;
            synchronized (attachmentMap) {
                attachment = (Attachment)attachmentMap.remove(address);
            }
            
            fireResponse(nodeId, address, time);
            firePingSuccess(attachment, nodeId, address, time);
        }
        
        public void handleTimeout(KUID nodeId, SocketAddress address, Message message, long time) {
            Attachment attachment = null;
            synchronized (attachmentMap) {
                attachment = (Attachment)attachmentMap.remove(address);
            }
            
            fireTimeout(nodeId, address, time);
            firePingTimeout(attachment, nodeId, address, time);
        }
        
        private void firePingSuccess(final Attachment attachment, 
                final KUID nodeId, final SocketAddress address, final long time) {
            
            fireEvent(new Runnable() {
                public void run() {
                    synchronized (listeners) {
                        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                            PingListener listener = (PingListener)it.next();
                            listener.pingSuccess(nodeId, address, time);
                        }
                    }
                    
                    if (attachment != null) {
                        for(Iterator it = attachment.listeners.iterator(); it.hasNext(); ) {
                            PingListener listener = (PingListener)it.next();
                            listener.pingSuccess(nodeId, address, time);
                        }
                    }
                }
            });
        }
        
        private void firePingTimeout(final Attachment attachment, 
                final KUID nodeId, final SocketAddress address, final long time) {
            
            fireEvent(new Runnable() {
                public void run() {
                    synchronized (listeners) {
                        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                            PingListener listener = (PingListener)it.next();
                            listener.pingTimeout(nodeId, address, time);
                        }
                    }
                    
                    if (attachment != null) {
                        for(Iterator it = attachment.listeners.iterator(); it.hasNext(); ) {
                            PingListener listener = (PingListener)it.next();
                            listener.pingTimeout(nodeId, address, time);
                        }
                    }
                }
            });
        }
    }
    
    public class LookupManager extends AbstractManager {
        
        private Map attachmentMap = new HashMap();
        
        private List listeners = new ArrayList();
        
        private LookupManager() {}
        
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

        public LookupListener[] getAllLookupListeners() {
            synchronized (listeners) {
                return (LookupListener[])listeners.toArray(new LookupListener[0]);
            }
        }
        
        public void lookup(KUID lookup) throws IOException {
            lookup(lookup, null);
        }
        
        public void lookup(KUID lookup, LookupListener listener) throws IOException {
            if (!lookup.isNodeID() && !lookup.isValueID()) {
                throw new IllegalArgumentException();
            }
            
            synchronized (attachmentMap) {
                Attachment attachment = (Attachment)attachmentMap.get(lookup);
                if (attachment == null) {
                    LookupResponseHandler2 handler = new LookupResponseHandler2(lookup, Context.this, this);
                    
                    attachment = new Attachment(handler);
                    handler.start();
                    attachmentMap.put(lookup, attachment);
                }
                
                if (listener != null) {
                    attachment.listeners.add(listener);
                }
            }
        }
        
        public boolean cancel(KUID lookup) {
            // TODO implement cancel
            return false;
        }

        public void handleSuccess(KUID nodeId, SocketAddress address, long time) {
            fireResponse(nodeId, address, time);
        }
        
        public void handleTimeout(KUID nodeId, SocketAddress address, long time) {
            fireTimeout(nodeId, address, time);
        }

        public void fireFinishLookup(KUID lookup, Collection results, long time) {
            Attachment attachment = null;
            synchronized (attachmentMap) {
                attachment = (Attachment)attachmentMap.remove(lookup);
            }
            
            fireFinishLookup(attachment, lookup, results, time);
        }
        
        private void fireFinishLookup(final Attachment attachment, 
                final KUID lookup, final Collection results, final long time) {
            
            fireEvent(new Runnable() {
                public void run() {
                    synchronized (listeners) {
                        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                            LookupListener listener = (LookupListener)it.next();
                            listener.finishLookup(lookup, results, time);
                        }
                    }
                    
                    if (attachment != null) {
                        for(Iterator it = attachment.listeners.iterator(); it.hasNext(); ) {
                            LookupListener listener = (LookupListener)it.next();
                            listener.finishLookup(lookup, results, time);
                        }
                    }
                }
            });
        }
    }
    
    
    /**
     * 
     */
    private static class Attachment {
        
        private ResponseHandler responseHandler;
        
        private List listeners = new ArrayList();
        
        private Attachment(ResponseHandler responeHandler) {
            this.responseHandler = responeHandler;
        }
    }
}
