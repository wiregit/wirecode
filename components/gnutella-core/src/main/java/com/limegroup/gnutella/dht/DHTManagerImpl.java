package com.limegroup.gnutella.dht;

import java.math.BigInteger;
import java.net.InetAddress;
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
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Comparators;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
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
import org.limewire.statistic.StatsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.ClassCNetworks;

/**
 * This DHT manager starts either an active or a passive DHT controller.
 * It also handles switching from one mode to the other.
 * 
 * This class offloads blocking operations to a threadpool
 * so that it never blocks on critical threads such as MessageDispatcher.
 */
@Singleton
public class DHTManagerImpl implements DHTManager {
    
    private static final Log LOG = LogFactory.getLog(DHTManagerImpl.class);
    
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
    private final List<DHTEventListener> dhtEventListeners = new ArrayList<DHTEventListener>(1);
    
    /** 
     * The executor to use to execute blocking DHT methods, such
     * as stopping or starting a Mojito instance (which perform 
     * network and disk I/O). 
     * */
    private final Executor executor;
    
    /**
     * The executor to use for dispatching events.
     */
    private final Executor dispatchExecutor;
    
    private volatile boolean enabled = true;
    
    private final DHTControllerFactory dhtControllerFactory;
    
    @InspectionPoint("time for dht bootstrap")
    private final BootstrapTimer bootstrapTimer = new BootstrapTimer();
    
    /**
     * Constructs the DHTManager, using the given Executor to invoke blocking 
     * methods. The executor MUST be single-threaded, otherwise there will be 
     * failures.
     * 
     * @param service
     */
    @Inject
    public DHTManagerImpl(@Named("dhtExecutor") Executor service, DHTControllerFactory dhtControllerFactory) {
        this.executor = service;
        this.dispatchExecutor = ExecutorsHelper.newProcessingQueue("DHT-EventDispatch");
        this.dhtControllerFactory = dhtControllerFactory;
        addEventListener(bootstrapTimer);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTManager#setEnabled(boolean)
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTManager#isEnabled()
     */
    public boolean isEnabled() {
        if (!DHTSettings.DISABLE_DHT_NETWORK.getValue() 
                && !DHTSettings.DISABLE_DHT_USER.getValue()
                && enabled) {
            return true;
        }
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTManager#start(com.limegroup.gnutella.dht.DHTManager.DHTMode)
     */
    public synchronized void start(DHTMode mode) {
        executor.execute(createSwitchModeCommand(mode));
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTManager#stop()
     */
    public synchronized void stop() {
        Runnable command = new Runnable() {
            public void run() {
                synchronized (DHTManagerImpl.this) {
                    try {
                        createSwitchModeCommand(DHTMode.INACTIVE).run();
                    } finally {
                        DHTManagerImpl.this.notifyAll();
                    }
                }
            }
        };
        
        executor.execute(command);
        
        try {
            this.wait(10000);
        } catch (InterruptedException err) {
            LOG.error("InterruptedException", err);
        }
    }
    
    /**
     * Creates and returns a Runnable that switches the DHT from
     * the current DHTMode to the given DHTMode.
     * 
     * @param mode The new Mode of the DHT
     * @return Runnable that switches the Mode
     */
    private Runnable createSwitchModeCommand(final DHTMode mode) {
        Runnable command = new Runnable() {
            public void run() {
                synchronized (DHTManagerImpl.this) {
                    // Controller already running in the current mode?
                    if (controller.getDHTMode() == mode) {
                        return;
                    }
                    
                    controller.stop();

                    if (mode == DHTMode.ACTIVE) {
                        controller = dhtControllerFactory.createActiveDHTNodeController(
                                vendor, version, DHTManagerImpl.this);
                    } else if (mode == DHTMode.PASSIVE) {
                        controller = dhtControllerFactory
                                .createPassiveDHTNodeController(vendor,
                                        version, DHTManagerImpl.this);
                    } else if (mode == DHTMode.PASSIVE_LEAF) {
                        controller = dhtControllerFactory.createPassiveLeafController(
                                vendor, version, DHTManagerImpl.this);
                    } else {
                        controller = new NullDHTController();
                    }
                    
                    controller.start();
                }
            }
        };
        
        return command;
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
                    if (controller.isRunning()) {
                        controller.stop();
                        controller.start();
                    }
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
    
    /**
     * Adds a listener to DHT Events.
     * 
     * Be aware that listeners will receive events after
     * after the DHT has dispatched them.  It is possible that
     * the DHT's status may have changed between the time the 
     * event was dispatched and the time the event is received
     * by a listener.
     */
    public synchronized void addEventListener(DHTEventListener listener) {
        if(dhtEventListeners.contains(listener))
            throw new IllegalArgumentException("Listener " + listener + " already registered");
        
        dhtEventListeners.add(listener);
    }

    /**
     * Sends an event to all listeners.
     * 
     * Be aware that to prevent deadlock, listeners may receive
     * the event long after the DHT's status has changed, and the
     * current status may be very different.
     * 
     * No events will be received in a different order than they were
     * dispatched, though.
     */
    public synchronized void dispatchEvent(final DHTEvent event) {
        if(!dhtEventListeners.isEmpty()) {
            final List<DHTEventListener> listeners = new ArrayList<DHTEventListener>(dhtEventListeners);
            dispatchExecutor.execute(new Runnable() {
                public void run() {
                    for(DHTEventListener listener : listeners) {
                        listener.handleDHTEvent(event);
                    }        
                }
            });
        }
    }

    public synchronized void removeEventListener(DHTEventListener listener) {
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
     */
    public void handleConnectionLifecycleEvent(final ConnectionLifecycleEvent evt) {
        Runnable command = null;
        if (evt.isDisconnectedEvent() || evt.isNoInternetEvent()) {
            command = new Runnable() {
                public void run() {
                    synchronized(DHTManagerImpl.this) {
                        if (controller.isRunning() 
                                && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
                            controller.stop();
                            controller = new NullDHTController();
                        }
                    }
                }
            };
        } else {
            command = new Runnable() {
                public void run() {
                    synchronized(DHTManagerImpl.this) {
                        controller.handleConnectionLifecycleEvent(evt);
                    }
                }
            };
        }
        executor.execute(command);
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
    @SuppressWarnings("unused")
    @InspectableContainer
    private class DHTInspectables {
        
        /*
         * 1 - initial version, doubles reported as long * Integer.MAX_VALUE
         * 2 - doubles reported as Double.doubleToLongBits
         */
        private static final int VERSION = 2;
        
        private void addVersion(Map<String, Object> m) {
            m.put("sv",VERSION);
        }
        @InspectionPoint("general dht stats")
        public Inspectable general = new Inspectable() {
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                addVersion(data);
                DHTMode mode = getDHTMode();
                boolean running = isRunning();
                boolean bootstrapped = isBootstrapped();
                boolean waiting = isWaitingForNodes();
                boolean enabled= isEnabled();
                Version version = getVersion();
                data.put("mode", Byte.valueOf(mode.byteValue())); // 4
                data.put("v", Integer.valueOf(version.shortValue())); // 4
                data.put("r", running);
                data.put("b", bootstrapped);
                data.put("w", waiting);
                data.put("e", enabled);
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
        
        @InspectionPoint("dht contacts")
        public Inspectable contacts = new Inspectable() {
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                addVersion(data);
                MojitoDHT dht = getMojitoDHT();
                if (dht != null) {
                    RouteTable routeTable = dht.getRouteTable();
                    synchronized(routeTable) {
                        double local = getDoubleKUID(routeTable.getLocalNode().getNodeID());
                        List<Double> activeContacts = getDouble(routeTable.getActiveContacts());
                        data.put("acc", StatsUtils.quickStatsDouble(activeContacts).getMap()); // 5*20 + 4
                        data.put("accx",StatsUtils.quickStatsDouble(getXorDistances(local, activeContacts)).getMap()); // 5*20 + 4
                        List<Double> cachedContacts = getDouble(routeTable.getCachedContacts());
                        data.put("ccc", StatsUtils.quickStatsDouble(cachedContacts).getMap()); // 5*20 + 4
                        data.put("cccx", StatsUtils.quickStatsDouble(getXorDistances(local, cachedContacts)).getMap()); // 5*20 + 4
                        
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
        
        @InspectionPoint("dht route table class C networks")
        public Inspectable routeTableTop10Networks = new Inspectable() {
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                addVersion(data);
                MojitoDHT dht = getMojitoDHT();
                if (dht != null) {
                    RouteTable routeTable = dht.getRouteTable();
                    synchronized(routeTable) {
                        data.put("ta", getTopNetworks(routeTable.getActiveContacts(), 10));
                        data.put("tc", getTopNetworks(routeTable.getCachedContacts(), 10));
                    }
                }
                return data;
            }
            
            private byte [] getTopNetworks(Collection<? extends Contact> nodes, int count) {
                // Masked IP -> Count
                ClassCNetworks classCNetworks = new ClassCNetworks();
                for (Contact node : nodes) {
                    InetAddress addr = ((InetSocketAddress)node.getContactAddress()).getAddress();
                    classCNetworks.add(addr, 1);
                }
                
                // Return the Top IPs and their count
                return classCNetworks.getTopInspectable(10);
            }
        };
        
        @InspectionPoint("dht buckets")
        public Inspectable buckets = new Inspectable() {
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                addVersion(data);
                MojitoDHT dht = getMojitoDHT();
                if (dht != null) {
                    RouteTable routeTable = dht.getRouteTable();
                    synchronized(routeTable) {
                        double local = getDoubleKUID(routeTable.getLocalNode().getNodeID());
                        Collection<Bucket> buckets = routeTable.getBuckets();
                        
                        List<Double> depths = new ArrayList<Double>(buckets.size());
                        List<Double> sizes = new ArrayList<Double>(buckets.size());
                        List<Double> kuids = new ArrayList<Double>(buckets.size());
                        List<Double> times = new ArrayList<Double>(buckets.size());
                        
                        double fresh = 0;
                        long now = System.currentTimeMillis(); 
                        for (Bucket bucket : buckets) {
                            depths.add((double)bucket.getDepth()); 
                            sizes.add((double)bucket.size());
                            kuids.add(getDoubleKUID(bucket.getBucketID()));
                            times.add((double)(now - bucket.getTimeStamp()));
                            if (!bucket.isRefreshRequired())
                                fresh++;
                        }
                        
                        // bucket kuid distribution *should* be similar to the others, but is it?
                        data.put("bk", StatsUtils.quickStatsDouble(kuids).getMap()); // 5*20 + 4
                        data.put("bkx", StatsUtils.quickStatsDouble(getXorDistances(local, kuids)).getMap()); // 5*20 + 4
                        data.put("bd", StatsUtils.quickStatsDouble(depths).getMap()); // 5*(should be one byte) + 4
                        data.put("bs", StatsUtils.quickStatsDouble(sizes).getMap()); // 5*(should be one byte) + 4
                        data.put("bt", StatsUtils.quickStatsDouble(times).getMap()); // 5*(should be one byte) + 4
                        data.put("bfr", (int)(100 * fresh / buckets.size())); // fresh buckets %
                    }
                }
                return data;
            }
        };
        
        @InspectionPoint("dht buckets detailed")
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
        
        @InspectionPoint("dht database")
        public Inspectable database = new Inspectable() {
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                addVersion(data);
                MojitoDHT dht = getMojitoDHT();
                if (dht != null) {
                    Database database = dht.getDatabase();
                    Double local = getDoubleKUID(dht.getLocalNodeID());
                    
                    List<Double> primaryKeys = null;
                    List<Double> requestLoads = null;
                    List<Double> distanceToLoad = null;
                    synchronized (database) {
                        data.put("dvc", Integer.valueOf(database.getValueCount())); // 4
                        Set<KUID> keys = database.keySet();
                        
                        primaryKeys = new ArrayList<Double>(keys.size());
                        requestLoads = new ArrayList<Double>(keys.size());
                        distanceToLoad = new ArrayList<Double>(keys.size());
                        
                        for (KUID primaryKey : keys) {
                            Double big = getDoubleKUID(primaryKey);
                            double load = database.getRequestLoad(primaryKey, false);
                            primaryKeys.add(big);
                            requestLoads.add(load);
                            if (local == big)
                                continue;
                            big = (double)(local.longValue() ^ big.longValue());
                            distanceToLoad.add(big - load * Integer.MAX_VALUE);
                        }
                    }

                    List<Double> storedXorDistances = getXorDistances(local, primaryKeys);
                    data.put("dsk", StatsUtils.quickStatsDouble(primaryKeys).getMap()); // 5*20 + 4
                    data.put("drl", StatsUtils.quickStatsDouble(requestLoads).getMap()); // 5*4 + 4
                    data.put("dskx", StatsUtils.quickStatsDouble(storedXorDistances).getMap()); // 5*20 + 4
                    data.put("dxlt", StatsUtils.quickStatsDouble(distanceToLoad).getTTestMap());
                }
                return data;
            }
        };
        
        @InspectionPoint("dht database top 10 keys")
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
                  
                  // load -> key
                  for(double load : popularKeys.keySet()) {
                      if (ret.size() >= 20)
                          break;
                      
                      ret.add(BigInteger.valueOf(Double.doubleToLongBits(load)).toByteArray());
                      ret.add(popularKeys.get(load).getBytes());
                  }
              }
              return ret;
          }
        };
        
        /** Histograms of the stored keys with various detail */
        @InspectionPoint("dht database 10 histogram")
        public Inspectable database10StoredHist = new DBHist(10);
        @InspectionPoint("dht database 100 histogram")
        public Inspectable database100StoredHist = new DBHist(100); // ~ 400 bytes uncompressed
        @InspectionPoint("dht database 500 histogram")
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
     * @return a list of XOR distances from a provided node
     */
    private static List<Double> getXorDistances(Double local, List<Double> others) {
        List<Double> distances = new ArrayList<Double>(others.size());
        for (Double l : others) {
            // Skip the local Node!
            if (l != local)
                distances.add((double)((local.longValue() ^ l.longValue())));
        }
        return distances;
    }
    
    /**
     * @return a list of big integers from a collection of contacts
     */
    private static List<Double> getDouble(Collection <? extends Contact> nodes) {
        List<Double> doubles = new ArrayList<Double>(nodes.size());
        for (Contact node : nodes) 
            doubles.add(getDoubleKUID(node.getNodeID()));
        return doubles;
    }
    
    /**
     * @return the 32 most significant bits from a KUID as a double primitive.
     */
    private static double getDoubleKUID(KUID k) {
        byte [] b = k.getBytes();
        long x = b[0];
        for (int i = 1; i < 4; i++) {
            x >>>= 8;
            x |= b[i];
        }
        return x;
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
    
    /**
     * Inspection point that tells us how long did the last bootstrap take.
     */
    private class BootstrapTimer implements DHTEventListener, Inspectable {
        private long start, stop;

        public synchronized Object inspect() {
            Map<String,Object> ret = new HashMap<String,Object>();
            ret.put("ver",1);
            ret.put("start",start);
            ret.put("stop",stop);
            return ret;
        }

        public synchronized void handleDHTEvent(DHTEvent evt) {
            if (evt.getType() == DHTEvent.Type.STARTING) 
                start = System.currentTimeMillis();
            else if (evt.getType() == DHTEvent.Type.CONNECTED && start != 0)
                stop = System.currentTimeMillis();
        }
    }
}