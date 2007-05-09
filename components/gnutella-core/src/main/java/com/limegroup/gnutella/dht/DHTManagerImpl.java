package com.limegroup.gnutella.dht;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import org.limewire.collection.Comparators;
import org.limewire.inspection.Inspectable;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
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
import com.limegroup.gnutella.util.StatsUtils;

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
        
        /*
         * 1 - initial version, doubles reported as long * Integer.MAX_VALUE
         * 2 - doubles reported as Double.doubleToLongBits
         */
        private static final int VERSION = 2;
        
        private void addVersion(Map<String, Object> m) {
            m.put("sv",VERSION);
        }
        
        public Inspectable general = new Inspectable() {
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                addVersion(data);
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
                addVersion(data);
                MojitoDHT dht = getMojitoDHT();
                if (dht != null) {
                    RouteTable routeTable = dht.getRouteTable();
                    synchronized(routeTable) {
                        BigInteger local = routeTable.getLocalNode().getNodeID().toBigInteger();
                        List<BigInteger> activeContacts = getBigInts(routeTable.getActiveContacts());
                        data.put("acc", StatsUtils.quickStatsBigInt(activeContacts).getMap()); // 5*20 + 4
                        data.put("accx",StatsUtils.quickStatsBigInt(getXorDistances(local, activeContacts)).getMap()); // 5*20 + 4
                        List<BigInteger> cachedContacts = getBigInts(routeTable.getCachedContacts());
                        data.put("ccc", StatsUtils.quickStatsBigInt(cachedContacts).getMap()); // 5*20 + 4
                        data.put("cccx", StatsUtils.quickStatsBigInt(getXorDistances(local, cachedContacts)).getMap()); // 5*20 + 4
                        
                        List<Double> activeIps = new ArrayList<Double>();
                        List<Double> cachedIps = new ArrayList<Double>();
                        List<Double> allIps = new ArrayList<Double>();
                        
                        for (Contact node : routeTable.getActiveContacts()) {
                            double masked = getUnsignedMaskedAddress(node);
                            activeIps.add(masked);
                            allIps.add(masked);
                        }
                        
                        for (Contact node : routeTable.getCachedContacts()) {
                            double masked = getUnsignedMaskedAddress(node);
                            cachedIps.add(masked);
                            allIps.add(masked);
                        }
                        
                        data.put("aips", StatsUtils.quickStatsDouble(activeIps).getMap());
                        data.put("cips", StatsUtils.quickStatsDouble(cachedIps).getMap());
                        data.put("allips", StatsUtils.quickStatsDouble(allIps).getMap());
                    }
                }
                return data;
            }
        };
        
        public Inspectable buckets = new Inspectable() {
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                addVersion(data);
                MojitoDHT dht = getMojitoDHT();
                if (dht != null) {
                    RouteTable routeTable = dht.getRouteTable();
                    synchronized(routeTable) {
                        BigInteger local = routeTable.getLocalNode().getNodeID().toBigInteger();
                        Collection<Bucket> buckets = routeTable.getBuckets();
                        
                        List<Double> depths = new ArrayList<Double>(buckets.size());
                        List<Double> sizes = new ArrayList<Double>(buckets.size());
                        List<BigInteger> kuids = new ArrayList<BigInteger>(buckets.size());
                        List<Double> times = new ArrayList<Double>(buckets.size());
                        
                        double fresh = 0;
                        long now = System.currentTimeMillis(); 
                        for (Bucket bucket : buckets) {
                            depths.add((double)bucket.getDepth()); 
                            sizes.add((double)bucket.size());
                            kuids.add(bucket.getBucketID().toBigInteger());
                            times.add((double)(now - bucket.getTimeStamp()));
                            if (!bucket.isRefreshRequired())
                                fresh++;
                        }
                        
                        // bucket kuid distribution *should* be similar to the others, but is it?
                        data.put("bk", StatsUtils.quickStatsBigInt(kuids).getMap()); // 5*20 + 4
                        data.put("bkx", StatsUtils.quickStatsBigInt(getXorDistances(local, kuids)).getMap()); // 5*20 + 4
                        data.put("bd", StatsUtils.quickStatsDouble(depths).getMap()); // 5*(should be one byte) + 4
                        data.put("bs", StatsUtils.quickStatsDouble(sizes).getMap()); // 5*(should be one byte) + 4
                        data.put("bt", StatsUtils.quickStatsDouble(times).getMap()); // 5*(should be one byte) + 4
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
                addVersion(data);
                MojitoDHT dht = getMojitoDHT();
                if (dht != null) {
                    Database database = dht.getDatabase();
                    BigInteger local = dht.getLocalNodeID().toBigInteger();
                    
                    List<BigInteger> primaryKeys = null;
                    List<Double> requestLoads = null;
                    List<BigInteger> distanceToLoad = null;
                    synchronized (database) {
                        data.put("dvc", Integer.valueOf(database.getValueCount())); // 4
                        Set<KUID> keys = database.keySet();
                        
                        primaryKeys = new ArrayList<BigInteger>(keys.size());
                        requestLoads = new ArrayList<Double>(keys.size());
                        distanceToLoad = new ArrayList<BigInteger>(keys.size());
                        
                        for (KUID primaryKey : keys) {
                            BigInteger big = primaryKey.toBigInteger();
                            double load = database.getRequestLoad(primaryKey, false);
                            primaryKeys.add(big);
                            requestLoads.add(load);
                            if (local.equals(big))
                                continue;
                            big = local.xor(big);
                            long bigLoad = (long) (load * Integer.MAX_VALUE);
                            distanceToLoad.add(big.subtract(BigInteger.valueOf(bigLoad)));
                        }
                    }

                    List<BigInteger> storedXorDistances = getXorDistances(local, primaryKeys);
                    data.put("dsk", StatsUtils.quickStatsBigInt(primaryKeys).getMap()); // 5*20 + 4
                    data.put("drl", StatsUtils.quickStatsDouble(requestLoads).getMap()); // 5*4 + 4
                    data.put("dskx", StatsUtils.quickStatsBigInt(storedXorDistances).getMap()); // 5*20 + 4
                    data.put("dxlt", StatsUtils.quickStatsBigInt(distanceToLoad).getTTestMap());
                }
                return data;
            }
        };
        
        public Inspectable databaseTop10Keys = new Inspectable() {
          public Object inspect() {
              List<byte[]>ret = new ArrayList<byte[]>();
              MojitoDHT dht = getMojitoDHT();
              if (dht != null) {
                  Database database = dht.getDatabase();
                  Map<Double, KUID> popularKeys = 
                      new TreeMap<Double,KUID>(Comparators.inverseDoubleComparator());
                  synchronized(database) {
                      Set<KUID> keys = database.keySet();
                      for (KUID primaryKey : keys) {
                          popularKeys.put((double)database.getRequestLoad(primaryKey, false), 
                                  primaryKey);
                      }
                  }
                  for(double load : popularKeys.keySet()) {
                      ret.add(BigInteger.valueOf(Double.doubleToLongBits(load)).toByteArray());
                      ret.add(popularKeys.get(load).getBytes());
                      if (ret.size() >= 20)
                          break;
                  }
              }
              return ret;
          }
        };
        
        /** Histograms of the stored keys with various detail */
        public Inspectable database10StoredHist = new DBHist(10);
        public Inspectable database100StoredHist = new DBHist(100); // ~ 400 bytes uncompressed
        public Inspectable database500StoredHist = new DBHist(500); // ~ 2kb uncompressed
    }
    
    /**
     * Inspectable that returns a histogram of the stored keys in the
     * database with specified accuracy.
     */
    private class DBHist implements Inspectable {
        private final int breaks;
        /**
         * @param breaks how many breaks should the histogram have.
         */
        DBHist(int breaks) {
            this.breaks = breaks;
        }
        public Object inspect() {
            MojitoDHT dht = getMojitoDHT();
            if (dht != null) {
                Database database = dht.getDatabase();
                List<BigInteger> primaryKeys;
                synchronized(database) {
                    Set<KUID> keys = database.keySet();
                    primaryKeys = new ArrayList<BigInteger>(keys.size());
                    for (KUID primaryKey : keys) 
                        primaryKeys.add(primaryKey.toBigInteger());
                }
                return StatsUtils.getHistogramBigInt(primaryKeys, breaks);
            }
            return Collections.emptyList();
        }
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
     * Returns the masked contact address of the given Contact as an
     * unsigned int
     */
    private static double getUnsignedMaskedAddress(Contact node) {
        InetSocketAddress addr = (InetSocketAddress)node.getContactAddress();
        long masked = NetworkUtils.getClassC(addr.getAddress()) & 0xFFFFFFFFL;
        return masked;
    }
}
