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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import de.kapsi.net.kademlia.handler.response.FindNodeResponseHandler;
import de.kapsi.net.kademlia.handler.response.FindValueResponseHandler;
import de.kapsi.net.kademlia.handler.response.PingResponseHandler;
import de.kapsi.net.kademlia.io.MessageDispatcher;
import de.kapsi.net.kademlia.messages.MessageFactory;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.routing.RouteTable;
import de.kapsi.net.kademlia.security.CryptoHelper;
import de.kapsi.net.kademlia.settings.KademliaSettings;

public class Context implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(Context.class);
    
    private static final String MESSAGE_DISPATCHER_THREAD = "MessageDispatcherThread";
    
    private static final long EVENT_DISPATCHER_DELAY = 100L;
    private static final long EVENT_DISPATCHER_INTERVAL = 100L;
    
    private static final int VENDOR = 0xDEADBEEF;
    private static final int VERSION = 0;
    
    private PublicKey masterKey;
    
    private KUID nodeId;
    private SocketAddress address;
    
    private KeyPair keyPair;
    
    private Database database;
    private RouteTable routeTable;
    private MessageDispatcher messageDispatcher;
    private EventDispatcher eventDispatcher;
    private MessageFactory messageFactory;
    private KeyValuePublisher publisher;
    
    private boolean running = false;
    private final Timer scheduler = new Timer(true);
    
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
        routeTable = new RouteTable(this);
        messageDispatcher = new MessageDispatcher(this);
        eventDispatcher = new EventDispatcher();
        messageFactory = new MessageFactory(this);
        publisher = new KeyValuePublisher(this);
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
    
    public Node getLocalNode() {
        return new Node(nodeId, address);
    }
    
    public KUID getLocalNodeID() {
        return nodeId;
    }
    
    public SocketAddress getSocketAddress() {
        return address;
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
        this.address = address;
        nodeId = KUID.createRandomNodeID(address);
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
            
            scheduler.scheduleAtFixedRate(eventDispatcher, EVENT_DISPATCHER_DELAY, 
                    EVENT_DISPATCHER_INTERVAL);
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
    
    public void ping(Node node, PingListener l) throws IOException {
        RequestMessage ping = messageFactory.createPingRequest();
        AbstractResponseHandler handler = new PingResponseHandler(this, l);
        messageDispatcher.send(node.getNodeID(), node.getSocketAddress(), ping, handler);
    }
    
    public void lookup(KUID lookup, FindNodeListener l) throws IOException {
        
        FindNodeResponseHandler handler 
            = new FindNodeResponseHandler(this, lookup, l);
        
        handler.lookup();
    }
    
    public void bootstrap(SocketAddress address, final BootstrapListener l) throws IOException {
        // Ping the Node
        ping(address, new PingListener() {
            public void ping(KUID nodeId, SocketAddress address, final long time1) {
                
                // Ping was successful, bootstrap!
                if (time1 >= 0L) {
                    try {
                        lookup(getLocalNodeID(), new FindNodeListener() {
                            public void foundNodes(KUID lookup, Collection nodes, long time2) {
                                if (l != null) {
                                    l.bootstrap(nodes.size() > 0, time1+time2);
                                }
                            }
                        });
                    } catch (IOException err) {
                        LOG.error(err);
                        if (l != null) {
                            fireEvent(new Runnable() {
                                public void run() {
                                    l.bootstrap(false, time1);
                                }
                            });
                        }
                    }
                    
                // Ping failed, notify listener!
                } else {
                    if (l != null) {
                        fireEvent(new Runnable() {
                            public void run() {
                                l.bootstrap(false, time1);
                            }
                        });
                    }
                }
            }
        });
    }
    
    /** store */
    public void store(final KeyValue value, final StoreListener l) throws IOException {
        lookup(value.getKey(), new FindNodeListener() {
            public void foundNodes(KUID lookup, Collection nodes, long time) {
                try {
                    
                    Collection values = Arrays.asList(new KeyValue[]{value});
                    
                    Iterator it = nodes.iterator();
                    int k = KademliaSettings.getReplicationParameter();
                    
                    for(int i = 0; i < k && it.hasNext(); i++) {
                        Node node = (Node)it.next();
                        
                        // TODO: Don't just store one KeyValue!
                        // Store the n-closest values to Node!
                        messageDispatcher.send(node, messageFactory.createStoreRequest(values), null);
                    }
                    
                    if (l != null) {
                        l.store(value, nodes);
                    }
                } catch (IOException err) {
                    LOG.error(err);
                }
            }
        });
    }
    
    public void get(KUID key, FindValueListener l) throws IOException {
        FindValueResponseHandler handler 
            = new FindValueResponseHandler(this, key, l);
        
        handler.lookup();
    }
}
