/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.tests.DHTNodeStat;
import com.limegroup.gnutella.dht.tests.DHTStats;

import de.kapsi.net.kademlia.db.Database;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.db.KeyValuePublisher;
import de.kapsi.net.kademlia.event.BootstrapListener;
import de.kapsi.net.kademlia.event.EventDispatcher;
import de.kapsi.net.kademlia.event.FindNodeListener;
import de.kapsi.net.kademlia.event.FindValueListener;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.event.StoreListener;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.handler.ResponseHandler;
import de.kapsi.net.kademlia.handler.response.FindNodeResponseHandler;
import de.kapsi.net.kademlia.handler.response.FindValueResponseHandler;
import de.kapsi.net.kademlia.handler.response.PingResponseHandler;
import de.kapsi.net.kademlia.handler.response.StoreResponseHandler;
import de.kapsi.net.kademlia.io.MessageDispatcher;
import de.kapsi.net.kademlia.messages.MessageFactory;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.routing.PatriciaRouteTable;
import de.kapsi.net.kademlia.routing.RandomBucketRefresher;
import de.kapsi.net.kademlia.routing.RoutingTable;
import de.kapsi.net.kademlia.security.CryptoHelper;
import de.kapsi.net.kademlia.security.QueryKey;
import de.kapsi.net.kademlia.settings.RouteTableSettings;

public class Context implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(Context.class);
    
    private static final String MESSAGE_DISPATCHER_THREAD = "MessageDispatcherThread";
    
    private static final long EVENT_DISPATCHER_INTERVAL = 100L;
    
    private static final long BUCKET_REFRESH_TIME = RouteTableSettings.getBucketRefreshTime();
    
    private static final int VENDOR = 0xDEADBEEF;
    private static final int VERSION = 0;
    
    private PublicKey masterKey;
    
    private KUID nodeId;
    private SocketAddress localAddress;
    private SocketAddress externalAddress;
    
    private KeyPair keyPair;
    
    private Database database;
    private RoutingTable routeTable;
    private MessageDispatcher messageDispatcher;
    private EventDispatcher eventDispatcher;
    private MessageFactory messageFactory;
    private KeyValuePublisher publisher;
    private RandomBucketRefresher bucketRefresher;
    
    private boolean running = false;
    private final Timer scheduler = new Timer(true);
    
    private DHTStats stats = null;
    
    public Context() {
        
        try {
            File file = new File("public.key");
            if (file.exists() && file.isFile()) {
                masterKey = CryptoHelper.loadMasterKey(file);
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        
        keyPair = CryptoHelper.createKeyPair();

        database = new Database(this);
        routeTable = new PatriciaRouteTable(this);
        messageDispatcher = new MessageDispatcher(this);
        eventDispatcher = new EventDispatcher();
        messageFactory = new MessageFactory(this);
        publisher = new KeyValuePublisher(this);
        bucketRefresher = new RandomBucketRefresher(this);
        
        stats = new DHTNodeStat(this);
    }
    
    public DHTStats getDHTStats() {
        return stats;
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
    
    public ContactNode getLocalNode() {
        ContactNode local = (ContactNode)routeTable.get(nodeId);
        if (local == null) {
            if (externalAddress != null) {
                local = new ContactNode(nodeId, externalAddress);
            } else {
                local = new ContactNode(nodeId, localAddress);
            }
        }
        return local;
    }
    
    public KUID getLocalNodeID() {
        return nodeId;
    }
    
    public SocketAddress getLocalSocketAddress() {
        return localAddress;
    }
    
    public SocketAddress getExternalSocketAddress() {
        return externalAddress;
    }
    
    public void setExternalSocketAddress(SocketAddress externalAddress) {
        if (this.externalAddress == null) {
            this.externalAddress = externalAddress;
            
            ContactNode localNode = (ContactNode)routeTable.get(nodeId);
            localNode.setSocketAddress(externalAddress);
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
        return publisher;
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
    
    public void bind(SocketAddress address) throws IOException {
        if (isOpen()) {
            throw new IOException("DHT is already bound");
        }
        
        messageDispatcher.bind(address);
        this.localAddress = address;
        
        nodeId = KUID.createRandomNodeID(address);
        
        /*byte[] id = ContextSettings.getLocalNodeID(address);
        if (id == null) {
            nodeId = KUID.createRandomNodeID(address);
            id = nodeId.getBytes();
            ContextSettings.setLocalNodeID(address, id);
        } else {
            nodeId = KUID.createNodeID(id);
        }*/
        //add ourselve to the routing table
        routeTable.add(getLocalNode(), true);
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
    }
    
    public void run() {
        if (!isOpen()) {
            throw new RuntimeException("DHT is not bound");
        }
        
        if (isRunning()) {
            LOG.error("DHT is already running!");
            return;
        }
        
        running = true;
        try {
            
            Thread publisherThread 
                = new Thread(publisher, "PublishThread");
            publisherThread.setDaemon(true);
            
            scheduler.scheduleAtFixedRate(eventDispatcher, 0, EVENT_DISPATCHER_INTERVAL);
            scheduler.scheduleAtFixedRate(bucketRefresher, BUCKET_REFRESH_TIME , BUCKET_REFRESH_TIME);
            publisherThread.start();

            messageDispatcher.run();
            
        } finally {
            publisher.stop();
            scheduler.cancel();
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
        
        if (!Thread.currentThread().getName().equals(MESSAGE_DISPATCHER_THREAD)) {
            event.run();
        } else {
            eventDispatcher.add(event);
        }
    }
    
    public void ping(SocketAddress address, PingListener l) throws IOException {
        RequestMessage ping = messageFactory.createPingRequest();
        AbstractResponseHandler handler = new PingResponseHandler(this, l);
        messageDispatcher.send(null, address, ping, handler);
    }
    
    public void ping(ContactNode node, PingListener l) throws IOException {
        RequestMessage ping = messageFactory.createPingRequest();
        AbstractResponseHandler handler = new PingResponseHandler(this, l);
        messageDispatcher.send(node.getNodeID(), node.getSocketAddress(), ping, handler);
    }
    
    public void lookup(KUID lookup, FindNodeListener l) throws IOException {
        
        FindNodeResponseHandler handler 
            = FindNodeResponseHandler.createDefaultHandler(this, lookup, l);
        
        handler.lookup();
    }
    
    public void bootstrap(SocketAddress address, final BootstrapListener l) throws IOException {
        // Ping the ContactNode
        ping(address, new PingListener() {
            public void pingResponse(KUID nodeId, SocketAddress address, final long time1) {
                
                // Ping was successful, bootstrap!
                if (time1 >= 0L) {
                    try {
                        lookup(getLocalNodeID(), new FindNodeListener() {
                            public void foundNodes(final KUID lookup, Collection nodes, Map queryKeys, final long time2) {
                                if (l != null) {
                                    l.initialPhaseComplete(getLocalNodeID(), nodes, time1+time2);
                                }
                                //now refresh furthest buckets too! 
                                try {
                                    routeTable.refreshBuckets(false,l);
                                } catch (IOException err) {
                                    LOG.error(err);
                                    if (l != null) {
                                        fireEvent(new Runnable() {
                                            public void run() {
                                                l.secondPhaseComplete(time2,false);
                                            }
                                        });
                                    }
                                }
                            }
                        });
                    } catch (IOException err) {
                        LOG.error(err);
                        if (l != null) {
                            fireEvent(new Runnable() {
                                public void run() {
                                    l.initialPhaseComplete(getLocalNodeID(), Collections.EMPTY_LIST, time1);
                                }
                            });
                        }
                    }
                    
                // Ping failed, notify listener!
                } else {
                    if (l != null) {
                        fireEvent(new Runnable() {
                            public void run() {
                                l.initialPhaseComplete(getLocalNodeID(), Collections.EMPTY_LIST, time1);
                            }
                        });
                    }
                }
            }
        });
    }
    
    /** store */
    public void store(final KeyValue keyValue, final StoreListener l) throws IOException {
        lookup(keyValue.getKey(), new FindNodeListener() {
            public void foundNodes(KUID lookup, Collection nodes, Map queryKeys, long time) {
                try {
                    List keyValues = Arrays.asList(new KeyValue[]{keyValue});
                    
                    List targets = new ArrayList(nodes.size());
                    for(Iterator it = nodes.iterator(); it.hasNext(); ) {
                        ContactNode node = (ContactNode)it.next();
                        QueryKey queryKey = (QueryKey)queryKeys.get(node);
                        
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
                    
                    if (l != null) {
                        l.store(keyValues, targets);
                    }
                } catch (IOException err) {
                    LOG.error(err);
                }
            }
        });
    }
    
    public void store(ContactNode node, QueryKey queryKey, List keyValues) throws IOException {
        if (queryKey == null) {
            throw new NullPointerException();
        }
        
        ResponseHandler handler = new StoreResponseHandler(this, queryKey, keyValues);
        
        RequestMessage request 
            = messageFactory.createStoreRequest(keyValues.size(), 
                    queryKey, Collections.EMPTY_LIST);
        
        messageDispatcher.send(node, request, handler);
    }
    
    public void get(KUID key, FindValueListener l) throws IOException {
        FindValueResponseHandler handler 
            = new FindValueResponseHandler(this, key, l);
        
        handler.lookup();
    }
}
