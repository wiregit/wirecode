package com.limegroup.gnutella.dht;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import org.limewire.inspection.Inspectable;
import org.limewire.io.IpPort;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.routing.Bucket;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.ContextSettings;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.settings.DHTSettings;

/**
 * This DHT manager starts either an active or a passive DHT controller.
 * It also handles switching from one mode to the other.
 * 
 * This class offloads blocking operations to a threadpool
 * so that it never blocks on critical threads such as MessageDispatcher.
 */
public class DHTManagerImpl implements DHTManager, Inspectable {
    
    /**
     * The Vendor code of this DHT Node
     */
    private final Vendor vendor = ContextSettings.getVendor();
    
    /**
     * The Version of this DHT Node
     */
    private final Version version = ContextSettings.getVersion();
    
    /**
     * The DHTController instance
     */
    private DHTController controller = new NullDHTController();
    
    /**
     * List of event listeners for ConnectionLifeCycleEvents.
     */
    private final CopyOnWriteArrayList<DHTEventListener> dhtEventListeners 
        = new CopyOnWriteArrayList<DHTEventListener>();
    
    /** 
     * The executor to use to execute blocking DHT methods, such
     * as stopping or starting a Mojito instance (which perform 
     * network and disk I/O). 
     * */
    private final Executor executor;
    
    private boolean stopped = false;
    
    /**
     * Constructs the LimeDHTManager, using the given Executor
     * to invoke blocking methods.  The executor MUST be single-threaded,
     * otherwise there will be failures.
     * 
     * @param service
     */
    public DHTManagerImpl(Executor service) {
        this.executor = service;
    }
    
    public synchronized void start(final DHTMode mode) {
        
        stopped = false;
        Runnable task = new Runnable() {
            public void run() {
                synchronized(DHTManagerImpl.this) {
                    //could have been stopped before this gets executed
                    if(stopped) {
                        return;
                    }
                    
                    //controller already running in the correct mode?
                    if(controller.isRunning() 
                            && (controller.getDHTMode() == mode)) {
                        return;
                    }
                    
                    controller.stop();

                    if (mode == DHTMode.ACTIVE) {
                        controller = new ActiveDHTNodeController(vendor, version, DHTManagerImpl.this);
                    } else if (mode == DHTMode.PASSIVE) {
                        controller = new PassiveDHTNodeController(vendor, version, DHTManagerImpl.this);
                    } else if (mode == DHTMode.PASSIVE_LEAF) {
                        controller = new PassiveLeafController(vendor, version, DHTManagerImpl.this);
                    } else {
                        controller = new NullDHTController();
                    }
                    
                    controller.start();
                }
            }
        };
        executor.execute(task);
    }
    
    public void addActiveDHTNode(final SocketAddress hostAddress) {
        executor.execute(new Runnable() {
            public void run() {
                synchronized(DHTManagerImpl.this) {
                    controller.addActiveDHTNode(hostAddress);
                }
            }
        });
    }
    
    public void addPassiveDHTNode(final SocketAddress hostAddress) {
        executor.execute(new Runnable() {
            public void run() {
                synchronized(DHTManagerImpl.this) {
                    controller.addPassiveDHTNode(hostAddress);
                }
            }
        });
    }

    public void addressChanged() {
        // Do this in a different thread as there are some blocking
        //disk and network ops.
        executor.execute(new Runnable() {
            public void run() {
                synchronized(DHTManagerImpl.this) {
                    if(!controller.isRunning()) {
                        return;
                    }
                    controller.stop();
                    controller.start();
                }
            }
        });
    }
    
    public synchronized List<IpPort> getActiveDHTNodes(int maxNodes){
        return controller.getActiveDHTNodes(maxNodes);
    }
    
    public synchronized DHTMode getDHTMode() {
        return controller.getDHTMode();
    }
    
    /**
     * This method has to be synchronized to make sure the
     * DHT actually gets stopped and persisted when it is called.
     */
    public synchronized void stop() {
        stopped = true;
        controller.stop();
        controller = new NullDHTController();
    }
    
    public synchronized boolean isRunning() {
        return controller.isRunning();
    }
    
    public synchronized boolean isBootstrapped() {
        return controller.isBootstrapped();
    }
    
    public synchronized boolean isMemberOfDHT() {
        return isRunning() && isBootstrapped();
    }

    public synchronized boolean isWaitingForNodes() {
        return controller.isWaitingForNodes();
    }
    
    public void addEventListener(DHTEventListener listener) {
        if(!dhtEventListeners.addIfAbsent(listener)) {
            throw new IllegalArgumentException("Listener " + listener + " already registered");
        }
    }

    public void dispatchEvent(DHTEvent event) {
        for(DHTEventListener listener : dhtEventListeners) {
            listener.handleDHTEvent(event);
        }
    }

    public void removeEventListener(DHTEventListener listener) {
        dhtEventListeners.remove(listener);
    }

    /**
     * This getter is for internal use only. The Mojito DHT is not meant to
     * be handled or passed around independently, as only the DHT controllers 
     * know how to interact correctly with it.
     * 
     */
    public synchronized MojitoDHT getMojitoDHT() {
        return controller.getMojitoDHT();
    }

    /**
     * Shuts the DHT down if we got disconnected from the network.
     * The nodeAssigner will take care of restarting this DHT node if 
     * it still qualifies.
     * 
     * If this event is not related to disconnection from the network, it
     * is forwarded to the controller for proper handling.
     * 
     */
    public void handleConnectionLifecycleEvent(final ConnectionLifecycleEvent evt) {
        Runnable r;
        if(evt.isDisconnectedEvent() || evt.isNoInternetEvent()) {
            r = new Runnable() {
                public void run() {
                    synchronized(DHTManagerImpl.this) {
                        if(controller.isRunning() 
                                && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
                            controller.stop();
                            controller = new NullDHTController();
                        }
                    }
                }
            };
        } else {
            r = new Runnable() {
                public void run() {
                    synchronized(DHTManagerImpl.this) {
                        controller.handleConnectionLifecycleEvent(evt);
                    }
                }
            };
        }
        executor.execute(r);
    }
    
    public Vendor getVendor() {
        return vendor;
    }
    
    public Version getVersion() {
        return version;
    }
    
    public void handleDHTContactsMessage(final DHTContactsMessage msg) {
        executor.execute(new Runnable() {
            public void run() {
                synchronized(DHTManagerImpl.this) {
                    for (Contact node : msg.getContacts()) {
                        controller.addContact(node);
                    }
                }
            }
        });
    }

    public Object inspect() {
        Map<String, Object> data = new HashMap<String, Object>();
        
        DHTMode mode = getDHTMode();
        Version version = getVersion();
        MojitoDHT dht = getMojitoDHT();
        
        data.put("mode", Byte.valueOf(mode.byteValue()));
        data.put("v", Integer.valueOf(version.shortValue()));
        
        if (dht != null) {
            data.put("s", dht.size().toByteArray());
            
            RouteTable routeTable = dht.getRouteTable();
            synchronized (routeTable) {
                Contact localNode = routeTable.getLocalNode();
                KUID nodeId = localNode.getNodeID();
                data.put("id", nodeId.getBytes());
                
                Collection<Contact> activeContacts = routeTable.getActiveContacts();
                List<BigInteger> activeDistacnes = getDistances(localNode, activeContacts);
                data.put("acc", Integer.valueOf(activeContacts.size()));
                data.put("accs",quickStats(activeDistacnes));
                
                
                Collection<Contact> cachedContacts = routeTable.getCachedContacts();
                List<BigInteger> cachedDistacnes = getDistances(localNode, cachedContacts);
                data.put("ccc", Integer.valueOf(cachedContacts.size()));
                data.put("cccs", quickStats(cachedDistacnes));
                
                Collection<Bucket> buckets = routeTable.getBuckets();
                data.put("bc", Integer.valueOf(buckets.size()));
                
                List<BigInteger> depths = new ArrayList<BigInteger>(buckets.size());
                for (Bucket bucket : buckets) {
                    depths.add(BigInteger.valueOf(bucket.getDepth()));
                }
                data.put("bcd",quickStats(depths));
                
                List<BigInteger> sizes = new ArrayList<BigInteger>(buckets.size());
                for (Bucket bucket : buckets) {
                    sizes.add(BigInteger.valueOf(bucket.size()));
                }
                data.put("bcs",quickStats(sizes));
            }
            
            Database database = dht.getDatabase();
            synchronized (database) {
                data.put("kc", Integer.valueOf(database.getKeyCount()));
                data.put("vc", Integer.valueOf(database.getValueCount()));
            }
        }
        return data;
    }
    
    /**
     * @return a list of distances from a provided node
     */
    private static List<BigInteger> getDistances(Contact localNode, Collection<? extends Contact> nodes) {
        KUID nodeId = localNode.getNodeID();
        List<BigInteger> distacnes = new ArrayList<BigInteger>(nodes.size()-1);
        for (Contact node : nodes) {
            // Skip the local Node!
            if (localNode != node) {
                KUID xor = nodeId.xor(node.getNodeID());
                distacnes.add(xor.toBigInteger());
            }
        }
        return distacnes;
    }
    
    /**
     * @return the average, variance, min, median and max of a
     * list of numbers
     */
    private static List<byte []> quickStats(List<BigInteger> l) {
        if (l.size() < 2)
            return Collections.emptyList();
        
        Collections.sort(l);
        
        BigInteger min = l.get(0);
        BigInteger max = l.get(l.size() - 1);
        BigInteger median = l.get(l.size() / 2);
        
        BigInteger sum = BigInteger.valueOf(0);
        for (BigInteger bi : l) 
            sum = sum.add(bi);
        
        BigInteger avg = sum.divide(BigInteger.valueOf(l.size()));
        
        sum = BigInteger.valueOf(0);
        for (BigInteger bi : l) {
            BigInteger dist = bi.subtract(avg);
            dist = dist.multiply(dist);
            sum = sum.add(dist);
        }
        BigInteger variance = sum.divide(BigInteger.valueOf(l.size() - 1));
        
        List<byte []> ret = new ArrayList<byte []>(5);
        ret.add(avg.toByteArray());
        ret.add(variance.toByteArray());
        ret.add(median.toByteArray());
        ret.add(min.toByteArray());
        ret.add(max.toByteArray());
        return ret;
    }
}
