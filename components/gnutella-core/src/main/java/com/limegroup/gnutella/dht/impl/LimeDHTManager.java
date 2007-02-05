package com.limegroup.gnutella.dht.impl;

import java.net.SocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.concurrent.SchedulingThreadPool;
import org.limewire.io.IpPort;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.ContextSettings;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.AltLocDHTValueImpl;
import com.limegroup.gnutella.dht.DHTController;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.PushProxiesDHTValueImpl;
import com.limegroup.gnutella.settings.DHTSettings;

/**
 * This DHT manager starts either an active or a passive DHT controller.
 * It also handles switching from one mode to the other.
 * 
 * This class offloads blocking operations to a threadpool
 * so that it never blocks on critical threads such as MessageDispatcher.
 * 
 */
public class LimeDHTManager implements DHTManager {
	
    /**
     * The Vendor code of this DHT Node
     */
    private Vendor vendor = ContextSettings.getVendor();
    
    /**
     * The Version of this DHT Node
     */
    private Version version = ContextSettings.getVersion();
    
    /**
     * The DHTController instance
     */
    private DHTController controller = new NullDHTController();
    
    /**
     * List of event listeners for ConnectionLifeCycleEvents.
     */
    private final CopyOnWriteArrayList<DHTEventListener> dhtEventListeners = 
        new CopyOnWriteArrayList<DHTEventListener>();
    
    /** 
     * Thread pool used to execute blocking DHT methods, such
     * as stopping or starting a Mojito instance (which perform 
     * network and disk I/O). 
     * */
    private SchedulingThreadPool threadPool;
    
    private volatile boolean stopped;
    
    public LimeDHTManager(SchedulingThreadPool threadPool) {
        this.threadPool = threadPool;
    }
    
    public void start(final boolean activeMode) {
        
        stopped = false;
        Runnable r = new Runnable() {
            public void run() {
                
                synchronized(LimeDHTManager.this) {
                    //could have been stopped before this gets executed
                    if(stopped) {
                        return;
                    }
                    
                    //controller already running in the correct mode?
                    if(controller.isRunning() 
                            && (controller.isActiveNode() == activeMode)) {
                        return;
                    }
                    
                    controller.stop();

                    if (activeMode) {
                        controller = new ActiveDHTNodeController(vendor, version, LimeDHTManager.this);
                    } else {
                        controller = new PassiveDHTNodeController(vendor, version, LimeDHTManager.this);
                    }

                    controller.start();
                }
            }
        };
        threadPool.invokeLater(r);
    }
    
    public void addActiveDHTNode(final SocketAddress hostAddress) {
        threadPool.invokeLater(new Runnable() {
            public void run() {
                synchronized(LimeDHTManager.this) {
                    controller.addActiveDHTNode(hostAddress);
                }
            }
        });
    }
    
    public void addPassiveDHTNode(final SocketAddress hostAddress) {
        threadPool.invokeLater(new Runnable() {
            public void run() {
                synchronized(LimeDHTManager.this) {
                    controller.addPassiveDHTNode(hostAddress);
                }
            }
        });
    }

    public void addressChanged() {
        // Do this in a different thread as there are some blocking
        //disk and network ops.
        threadPool.invokeLater(new Runnable() {
            public void run() {
                synchronized(LimeDHTManager.this) {
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
    
    public synchronized boolean isActiveNode() {
        return (controller.isActiveNode() && controller.isRunning());
    }
    
    /**
     * This method has to be synchronized to make sure the
     * DHT actually gets stopped and persisted when it is called.
     */
    public synchronized void stop(){
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
                    synchronized(LimeDHTManager.this) {
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
                    synchronized(LimeDHTManager.this) {
                        controller.handleConnectionLifecycleEvent(evt);
                    }
                }
            };
        }
        threadPool.invokeLater(r);
    }
    
    public Vendor getVendor() {
        return vendor;
    }
    
    public Version getVersion() {
        return version;
    }
    
    public DHTFuture<FindValueResult> getAltLocs(URN urn) {
        return getMojitoDHT().get(toKUID(urn));
    }
    
    public DHTFuture<FindValueResult> getPushProxies(GUID guid) {
        return getMojitoDHT().get(toKUID(guid));
    }
    
    public DHTFuture<StoreResult> putAltLoc(FileDesc fd) {
        KUID key = toKUID(fd.getSHA1Urn());
        return getMojitoDHT().put(key, AltLocDHTValueImpl.LOCAL_HOST);
    }
    
    public DHTFuture<StoreResult> putAltLoc(URN urn, GUID guid, IpPort ipp, int features, int fwtVersion) {
        if (!RouterService.getConnectionManager().isActiveSupernode()) {
            throw new IllegalStateException("This method works only if we are an Ultrapeer");
        }
        
        KUID key = toKUID(urn);
        return getMojitoDHT().put(key, AltLocDHTValueImpl.createProxyValue(guid, ipp, features, fwtVersion));
    }
    
    public DHTFuture<StoreResult> putPushProxy(GUID guid, Set<? extends IpPort> proxies) {
        KUID key = toKUID(guid);
        return getMojitoDHT().put(key, PushProxiesDHTValueImpl.createProxyValue(proxies));
    }
    
    private static KUID toKUID(URN urn) {
        if (!urn.isSHA1()) {
            throw new IllegalArgumentException("Expected a SHA-1 URN: " + urn);
        }
        return KUID.createWithBytes(urn.getBytes());
    }
    
    private static KUID toKUID(GUID guid) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(guid.bytes());
            byte[] digest = md.digest();
            return KUID.createWithBytes(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
