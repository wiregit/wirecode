package com.limegroup.gnutella.dht;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class DHTManagerImpl implements DHTManager {
    
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
    
    /** reference to a bunch of inspectables */
    public final DHTInspectables inspectables = new DHTInspectables();
    
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

    /** a bunch of inspectables */
    private class DHTInspectables {
        
        public Inspectable general = new Inspectable() {
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                DHTMode mode = getDHTMode();
                Version version = getVersion();
                data.put("mode", Byte.valueOf(mode.byteValue())); // 4
                data.put("v", Integer.valueOf(version.shortValue())); // 4
                MojitoDHT dht = getMojitoDHT();
                if (dht != null) {
                    data.put("s", dht.size().toByteArray()); // 3
                    RouteTable routeTable = dht.getRouteTable();
                    Contact localNode = routeTable.getLocalNode();
                    data.put("id", localNode.getNodeID().getBytes()); // 20
                }
                return data;
            }
        };
        
        public Inspectable contacts = new Inspectable() {
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                MojitoDHT dht = getMojitoDHT();
                if (dht != null) {
                    RouteTable routeTable = dht.getRouteTable();
                    synchronized(routeTable) {
                        BigInteger local = routeTable.getLocalNode().getNodeID().toBigInteger();
                        List<BigInteger> activeContacts = getBigInts(routeTable.getActiveContacts());
                        data.put("acc",quickStats(activeContacts)); // 5*20 + 4
                        data.put("accx",quickStats(getXorDistances(local, activeContacts))); // 5*20 + 4
                        List<BigInteger> cachedContacts = getBigInts(routeTable.getCachedContacts());
                        data.put("ccc", quickStats(cachedContacts)); // 5*20 + 4
                        data.put("cccx", quickStats(getXorDistances(local, cachedContacts))); // 5*20 + 4
                    }
                }
                return data;
            }
        };
        
        
        public Inspectable buckets = new Inspectable() {
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                MojitoDHT dht = getMojitoDHT();
                if (dht != null) {
                    RouteTable routeTable = dht.getRouteTable();
                    synchronized(routeTable) {
                        BigInteger local = routeTable.getLocalNode().getNodeID().toBigInteger();
                        Collection<Bucket> buckets = routeTable.getBuckets();
                        
                        List<BigInteger> depths = new ArrayList<BigInteger>(buckets.size());
                        List<BigInteger> sizes = new ArrayList<BigInteger>(buckets.size());
                        List<BigInteger> kuids = new ArrayList<BigInteger>(buckets.size());
                        List<BigInteger> times = new ArrayList<BigInteger>(buckets.size());
                        
                        double fresh = 0;
                        long now = System.currentTimeMillis(); 
                        for (Bucket bucket : buckets) {
                            depths.add(BigInteger.valueOf(bucket.getDepth())); 
                            sizes.add(BigInteger.valueOf(bucket.size()));
                            kuids.add(bucket.getBucketID().toBigInteger());
                            times.add(BigInteger.valueOf(now - bucket.getTimeStamp()));
                            if (!bucket.isRefreshRequired())
                                fresh++;
                        }
                        
                        // bucket kuid distribution *should* be similar to the others, but is it?
                        data.put("bk", quickStats(kuids)); // 5*20 + 4
                        data.put("bkx", quickStats(getXorDistances(local, kuids))); // 5*20 + 4
                        data.put("bd", quickStats(depths)); // 5*(should be one byte) + 4
                        data.put("bs", quickStats(sizes)); // 5*(should be one byte) + 4
                        data.put("bt", quickStats(times)); // 5*(should be one byte) + 4
                        data.put("bfr", (int)(100 * fresh / buckets.size())); // fresh buckets %
                    }
                }
                return data;
            }
        };
        
        public Inspectable bucketDetail = new Inspectable() {
            public Object inspect() {
                List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
                MojitoDHT dht = getMojitoDHT();
                if (dht != null) {
                    RouteTable routeTable = dht.getRouteTable();
                    synchronized(routeTable) {
                        Collection<Bucket> buckets = routeTable.getBuckets();
                        for (Bucket bucket : buckets) {
                            Map<String, Object> detail = new HashMap<String, Object>();
                            detail.put("i", bucket.getBucketID().getBytes());
                            detail.put("d", bucket.getDepth());
                            detail.put("t", System.currentTimeMillis() - bucket.getTimeStamp());
                            detail.put("a", bucket.getActiveSize());
                            detail.put("c", bucket.getCacheSize());
                            detail.put("f", !bucket.isRefreshRequired());
                            
                            data.add(detail);
                        }
                    }
                }
                return data;
            }
        };
        
        public Inspectable database = new Inspectable() {
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                MojitoDHT dht = getMojitoDHT();
                if (dht != null) {
                    Database database = dht.getDatabase();
                    List<BigInteger> stored;
                    BigInteger local = dht.getLocalNodeID().toBigInteger();
                    synchronized (database) {
                        data.put("dvc", Integer.valueOf(database.getValueCount())); // 4
                        Set<KUID> keys = database.keySet();
                        
                        stored = new ArrayList<BigInteger>(keys.size());
                        for (KUID k : keys)
                            stored.add(k.toBigInteger());
                    }

                    List<BigInteger> storedXorDistances = getXorDistances(local, stored);
                    data.put("dsk", quickStats(stored)); // 5*20 + 4
                    data.put("dskx", quickStats(storedXorDistances)); // 5*20 + 4
                }
                return data;
            }
        };
    }
    
    /**
     * @return a list of XOR distances from a provided bigint
     */
    private static List<BigInteger> getXorDistances(BigInteger local, List<BigInteger> others) {
        List<BigInteger> distances = new ArrayList<BigInteger>(others.size());
        for (BigInteger bi : others) {
            // Skip the local Node!
            if (!local.equals(bi)) 
                distances.add(local.xor(bi));
        }
        return distances;
    }
    
    /**
     * @return a list of big integers from a collection of contacts
     */
    private static List<BigInteger> getBigInts(Collection <? extends Contact> nodes) {
        List<BigInteger> bigints = new ArrayList<BigInteger>(nodes.size());
        for (Contact node : nodes) 
            bigints.add(node.getNodeID().toBigInteger());
        return bigints;
    }
    
    /**
     * @return the number, average, variance, min, median and max of a
     * list of numbers
     */
    private static Map<String, Object> quickStats(List<BigInteger> l) {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("num",l.size());
        
        if (l.size() < 2) // too small for stats
            return ret;
        
        Collections.sort(l);
        
        ret.put("min",l.get(0).toByteArray());
        ret.put("max",l.get(l.size() -1).toByteArray());
        ret.put("med", getQuartile(2, l).toByteArray());
        
        if (l.size() > 6) {
            // big enough to find outliers 
            ret.put("Q1", getQuartile(1, l).toByteArray());
            ret.put("Q3", getQuartile(3, l).toByteArray());
        }
        
        BigInteger sum = BigInteger.valueOf(0);
        for (BigInteger bi : l) 
            sum = sum.add(bi);
        
        BigInteger avg = sum.divide(BigInteger.valueOf(l.size()));
        ret.put("avg",avg.toByteArray());
        
        sum = BigInteger.valueOf(0);
        for (BigInteger bi : l) {
            BigInteger dist = bi.subtract(avg);
            dist = dist.multiply(dist);
            sum = sum.add(dist);
        }
        BigInteger variance = sum.divide(BigInteger.valueOf(l.size() - 1));
        ret.put("var",variance.toByteArray());
        return ret;
    }
    
    /**
     * the a specified quartile of a list of BigIntegers. 
     */
    private static BigInteger getQuartile(int quartile, List<BigInteger> l) {
        double q1 = (l.size()+1) * (quartile / 4.0);
        int q1i = (int)q1;
        if (q1 - q1i == 0) 
            return l.get(q1i - 1);
        int quart = (int)(1 / (q1 - q1i));
        BigInteger q1a = l.get(q1i - 1);
        BigInteger q1b = l.get(q1i);
        q1b = q1b.subtract(q1a);
        q1b = q1b.divide(BigInteger.valueOf(4));
        q1b = q1b.multiply(BigInteger.valueOf(quart));
        q1a = q1a.add(q1b);
        return q1a;
    }
}
