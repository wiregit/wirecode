package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.FixedSizeLIFOSet;
import org.limewire.collection.FixedSizeLIFOSet.EjectionPolicy;
import org.limewire.concurrent.ManagedThread;
import org.limewire.io.IpPort;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.RouteTable.RouteTableEvent;
import org.limewire.mojito.routing.RouteTable.RouteTableListener;
import org.limewire.mojito.statistics.DHTStatsManager;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.CryptoUtils;
import org.limewire.mojito.util.HostFilter;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.dht.DHTEvent.Type;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.dht.db.AbstractAltLocValue;
import com.limegroup.gnutella.dht.db.AbstractPushProxiesValue;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.EventDispatcher;

/**
 * The controller for the LimeWire Gnutella DHT. 
 * A node should connect to the DHT only if it has previously been designated as capable 
 * by the <tt>NodeAssigner</tt> or if it is forced to. 
 * Once the node is a DHT node, if <tt>EXCLUDE_ULTRAPEERS</tt> is set to true, 
 * it should no try to connect as an ultrapeer (@see RouterService.isExclusiveDHTNode()). 
 *
 * The NodeAssigner should be the only class to have the authority to 
 * initialize the DHT and connect to the network.
 * 
 * This controller can be in one of the four following states:
 * 1) not running.
 * 2) running and bootstrapping: the dht is trying to bootstrap.
 * 3) running and waiting: the dht has failed the bootstrap and is waiting for additional bootstrap hosts.
 * 3) running and bootstrapped.
 * 
 * <b>Warning:</b> The methods in this class are NOT synchronized.
 * 
 * The current implementation is specific to the Mojito DHT. 
 */
public abstract class AbstractDHTController implements DHTController {
    
    private static final String PUBLIC_KEY 
        = "D6FQQAAAAAAAAAAAAHYQCDX6AAAAALRQFQBBIR75ZTOX67BIDUQXP25SBSH"
        + "UMQL6YVG56AQUDYKJOBYQENLDNC5IG5EFTOBSTGBALX6FAAAADOZQQIA3OME"
        + "CAEWAMBZKQZEM4OAEAEYIEAI7AKAYCAH5P5JYCHLVCIUVFX2KTQXOZZHH6YI3"
        + "OUR455CABQY6H6ALMUJGNFCV2QBCKH5VSPMNLD5L7RPVXIYPNS43KVWNPAJ3Q"
        + "AOTI37SMZQLO24ZKCS2JH475ACHWEBCYJH3XKOX7234MG7YHNL6PRVIUYKQ6BH"
        + "3QP3NHRI6YMBDKVATLILJCMXWOXZ24K3B24VO74RCAMMZ3UKIAHDQEFIAS5QFB"
        + "DYVEMF4ZMUSXGBKF24EBPYFQHHVAKAYCAHX4GQILVU3HXPMXPFLLQ3LQV5ZPGK"
        + "K7O72HLVIF6KXJQFT2B4CM5IVSV4OXLKFST7GOEDRBAMAWRERM4JD5BGCQFQTW"
        + "7HQSMUMZCTOCPAWPKFVI7ENFDQKHLQ6FOZ2M5MRN2RX6C72EE2WF4P3MJ5ACJB"
        + "3ZSSPDPVIKGIITKED37QVVZM7A2JIWZS6QB5VKJLEAFGDX7WPJEVAHAMEAABID"
        + "AAFYBH7XPUVWNHIHXBRRG6ZDLCYU2NUZG22VULUBG6IUXXCQYBA5R2XANSCQ3L"
        + "OU5HUD7UYUG2WHTRGA4RHQQ2ORWPPFGFBP246YW2P7B4OPKUR2ADVCOKNTX4TE"
        + "3XSD7ALYDOTBJJK57VRAQGZADLNAHXY6YYFAI7D65EX4IFUH5ZITPP4MLQKSUY"
        + "YLFJEKYDWDPADVSPP5Y4SZON2YHYQCAAA";
    
    protected final Log LOG = LogFactory.getLog(getClass());
    
    /**
     * The instance of the DHT
     */
    protected final MojitoDHT dht;

    /**
     * The DHT bootstrapper instance.
     */
    protected final DHTBootstrapper bootstrapper;
    
    /**
     * The random node adder.
     */
    private final RandomNodeAdder dhtNodeAdder = new RandomNodeAdder();
    
    /**
     * The DHT event dispatcher
     */
    private final EventDispatcher<DHTEvent, DHTEventListener> dispatcher;
    
    /**
     * The mode of this DHTController
     */
    private final DHTMode mode;
    
    /**
     * Get and save the current RouteTable version
     */
    private final int routeTableVersion;
    
    private final DHTControllerFacade dhtControllerFacade;
    
    public AbstractDHTController(Vendor vendor, Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher,
            final DHTMode mode, DHTControllerFacade dhtControllerFacade) {

        this.dhtControllerFacade = dhtControllerFacade;
        
        switch(mode) {
            case ACTIVE:
                routeTableVersion = DHTSettings.ACTIVE_DHT_ROUTETABLE_VERSION.getValue();
                break;
            case PASSIVE:
                routeTableVersion = DHTSettings.PASSIVE_DHT_ROUTETABLE_VERSION.getValue();
                break;
            default:
                routeTableVersion = -1;
        }
        
        this.dispatcher = dispatcher;
        this.mode = mode;
        
        this.dht = createMojitoDHT(vendor, version);
        
        assert (dht != null);
        dht.setMessageDispatcher(dhtControllerFacade.getMessageDispatcherFactory());
        dht.setMACCalculatorRepositoryManager(dhtControllerFacade.getMACCalculatorRespositoryManager());
        dht.setSecurityTokenProvider(dhtControllerFacade.getSecurityTokenProvider());                
        dht.getDHTExecutorService().setThreadFactory(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                return new ManagedThread(runnable);
            }
        });
        
        dht.setHostFilter(new FilterDelegate());
        
        dht.getDHTValueFactoryManager().addValueFactory(
                AbstractAltLocValue.ALT_LOC, dhtControllerFacade.getAltLocValueFactory());
        
        dht.getDHTValueFactoryManager().addValueFactory(
                AbstractPushProxiesValue.PUSH_PROXIES, dhtControllerFacade.getPushProxyValueFactory());
        
        try {
            PublicKey publicKey = CryptoUtils.loadPublicKey(PUBLIC_KEY);
            KeyPair keyPair = new KeyPair(publicKey, null);
            dht.setKeyPair(keyPair);
        } catch (InvalidKeyException e) {
            LOG.error("InvalidKeyException", e);
        } catch (SignatureException e) {
            LOG.error("SignatureException", e);
        } catch (IOException e) {
            LOG.error("IOException", e);
        }

        dht.getStorableModelManager().addStorableModel(
                AbstractAltLocValue.ALT_LOC, dhtControllerFacade.getAltLocModel());
        
        // There's no point in publishing my push proxies if I'm
        // not a passive leaf Node (ultrapeers and active nodes
        // do not push proxies as they're not firewalled).
        if (mode == DHTMode.PASSIVE_LEAF) {
            dht.getStorableModelManager().addStorableModel(
                    AbstractPushProxiesValue.PUSH_PROXIES, dhtControllerFacade.getPushProxyModel());
        }
        
        this.bootstrapper = dhtControllerFacade.getDHTBootstrapper(this);
        
        // If we're an Ultrapeer we want to notify our firewalled
        // leafs about every new Contact
        if (dhtControllerFacade.isActiveSupernode()) {
            dht.getRouteTable().addRouteTableListener(new RouteTableListener() {
                public void handleRouteTableEvent(RouteTableEvent event) {
                    switch(event.getEventType()) {
                        case ADD_ACTIVE_CONTACT:
                        case ADD_CACHED_CONTACT:
                        case UPDATE_CONTACT:
                            Contact node = event.getContact();
                            if (mode == DHTMode.ACTIVE || !dht.getLocalNodeID().equals(node.getNodeID()))
                                forwardContact(node);
                            break;
                    }
                }
            });
        }
        
        DHTSettings.DHT_NODE_ID.setValue(dht.getLocalNodeID().toHexString());
        DHTStatsManager.clear();
    }

    /**
     * Returns the current RouteTable version
     */
    protected final int getRouteTableVersion() {
        return routeTableVersion;
    }
    
    /**
     * A factory method to create MojitoDHTs
     */
    protected abstract MojitoDHT createMojitoDHT(Vendor vendor, Version version);
    
    private void forwardContact(Contact node) {
        if (!DHTSettings.ENABLE_PASSIVE_LEAF_DHT_MODE.getValue()) {
            return;
        }
        
        DHTContactsMessage msg = new DHTContactsMessage(node);
        List<RoutedConnection> list = dhtControllerFacade.getInitializedClientConnections();
        for (RoutedConnection mc : list) {
            if (mc.isPushProxyFor()
                    && mc.getConnectionCapabilities().remoteHostIsPassiveLeafNode() > -1) {
                mc.send(msg);
            }
        }
    }
    
    public DHTMode getDHTMode() {
        return mode;
    }

    public List<IpPort> getActiveDHTNodes(int maxNodes) {
        return Collections.emptyList();
    }

    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
    }
    
    /**
     * Start the Mojito DHT and connects it to the network in either passive mode
     * or active mode if we are not firewalled.
     * The start preconditions are the following:
     * 1) We are not already connected AND we have at least one initialized Gnutella connection
     * 2) if we want to actively connect: We are DHT_CAPABLE OR FORCE_DHT_CONNECT is true 
     * 
     * @param activeMode true to connect to the DHT in active mode
     */
    public void start() {
        if (isRunning() || (!DHTSettings.FORCE_DHT_CONNECT.getValue() 
                && !dhtControllerFacade.isConnected())) {
            return;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Initializing the DHT");
        }
        
        try {
            InetAddress addr = InetAddress.getByAddress(dhtControllerFacade.getAddress());
            int port = dhtControllerFacade.getPort();
            dht.bind(new InetSocketAddress(addr, port));
            dht.start();
            
            // Bootstrap only if we're not a passive leaf node
            if (getDHTMode() != DHTMode.PASSIVE_LEAF) {
                bootstrapper.bootstrap();
            }
            
            dispatcher.dispatchEvent(new DHTEvent(this, Type.STARTING));
        } catch (IOException err) {
            LOG.error("IOException", err);
            ErrorService.error(err);
        }
    }
    
    /**
     * Shuts down the dht. If this is an active node, it sends the updated capabilities
     * to its ultrapeers and persists the DHT. Otherwise, it just saves a list of MRS nodes
     * to bootstrap from for the next session.
     * 
     */
    public void stop() {
        LOG.debug("Shutting down DHT Controller");
        
        bootstrapper.stop();
        dhtNodeAdder.stop();
        dht.close();
        
        dispatcher.dispatchEvent(new DHTEvent(this, Type.STOPPED));
    }
    
    /**
     * If this node is not bootstrapped, passes the given hostAddress
     * to the DHT bootstrapper. 
     * If it is already bootstrapped, this randomly tries to add the node
     * to the DHT routing table.
     * 
     * @param hostAddress The SocketAddress of the DHT host.
     */
    protected void addActiveDHTNode(SocketAddress hostAddress, boolean addToDHTNodeAdder) {
        if(!dht.isBootstrapped()){
            bootstrapper.addBootstrapHost(hostAddress);
        } else if(addToDHTNodeAdder){
            dhtNodeAdder.addDHTNode(hostAddress);
            dhtNodeAdder.start();
        }
    }
    
    public void addActiveDHTNode(SocketAddress hostAddress) {
        addActiveDHTNode(hostAddress, true);
    }
    
    public void addPassiveDHTNode(SocketAddress hostAddress) {
        if (!dht.isBootstrapped()) {
            bootstrapper.addPassiveNode(hostAddress);
        }
    }
    
    public void addContact(Contact node) {
        if (getDHTMode() == DHTMode.PASSIVE_LEAF) {
            getMojitoDHT().getRouteTable().add(node);
        }
    }
    
    /**
     * Returns a list of the Most Recently Seen nodes from the Mojito 
     * routing table.
     * 
     * @param numNodes The number of nodes to return
     * @param excludeLocal true to exclude the local node
     * @return A list of DHT <tt>IpPorts</tt>
     */
    protected List<IpPort> getMRSNodes(int numNodes, boolean excludeLocal) {
        Collection<Contact> nodes = ContactUtils.sort(
                dht.getRouteTable().getActiveContacts(), numNodes + 1); //it will add the local node!
        
        KUID localNode = dht.getLocalNodeID();
        List<IpPort> ipps = new ArrayList<IpPort>();
        for(Contact cn : nodes) {
            if(excludeLocal && cn.getNodeID().equals(localNode)) {
                continue;
            }
            ipps.add(new IpPortRemoteContact(cn));
        }
        return ipps;
    }
    
    public boolean isRunning() {
        return dht.isRunning();
    }
    
    public boolean isBootstrapped() {
        return dht.isBootstrapped();
    }

    public boolean isWaitingForNodes() {
        return bootstrapper.isWaitingForNodes();
    }
    
    public MojitoDHT getMojitoDHT() {
        return dht;
    }
    
    /**
     * Sends the updated CapabilitiesVM to our connections. This is used
     * when a node has successfully bootstrapped to the network and wants to notify
     * its Gnutella peers that they can now bootstrap off of him.
     * 
     */
    public void sendUpdatedCapabilities() {
        
        LOG.debug("Sending updated capabilities to our connections");
        
        dhtControllerFacade.updateCapabilities();
        dhtControllerFacade.sendUpdatedCapabilities();
        
        if (isRunning())
            dispatcher.dispatchEvent(new DHTEvent(this, Type.CONNECTED));
    }
    
    /**
     * A helper class to easily go back and forth 
     * from the DHT's RemoteContact to Gnutella's IpPort
     */
    private static class IpPortRemoteContact implements IpPort {
        
        private InetSocketAddress addr;
        
        public IpPortRemoteContact(Contact node) {
            
            if(!(node.getContactAddress() instanceof InetSocketAddress)) {
                throw new IllegalArgumentException("Contact not instance of InetSocketAddress");
            }
            
            addr = (InetSocketAddress) node.getContactAddress();
        }
        
        public String getAddress() {
            return getInetAddress().getHostAddress();
        }

        public InetAddress getInetAddress() {
            return addr.getAddress();
        }

        public int getPort() {
            return addr.getPort();
        }
        
        public SocketAddress getSocketAddress() {
            return addr;
        }

        public InetSocketAddress getInetSocketAddress() {
            return addr;
        }
    }
    
    /**
     * This class is used to fight against possible DHT clusters 
     * by periodicaly sending a Mojito ping to the last 30 DHT nodes seen in the Gnutella
     * network. It is effectively randomly adding them to the DHT routing table.
     * 
     */
    class RandomNodeAdder implements Runnable {
        
        private static final int MAX_SIZE = 30;
        
        private final Set<SocketAddress> dhtNodes;
        
        private ScheduledFuture<?> timerTask;
        
        private boolean isRunning;
        
        public RandomNodeAdder() {
            dhtNodes = new FixedSizeLIFOSet<SocketAddress>(MAX_SIZE, EjectionPolicy.FIFO);
        }
        
        public synchronized void start() {
            if(isRunning) {
                return;
            }
            long delay = DHTSettings.DHT_NODE_ADDER_DELAY.getValue();
            timerTask = dhtControllerFacade.scheduleWithFixedDelay(this, delay, delay, TimeUnit.MILLISECONDS);
            isRunning = true;
        }
        
        synchronized void addDHTNode(SocketAddress address) {
            dhtNodes.add(address);
        }
        
        public void run() {
            
            List<SocketAddress> nodes = null;
            synchronized (this) {
                
                if(!isRunning()) {
                    return;
                }
                
                nodes = new ArrayList<SocketAddress>(dhtNodes);
                dhtNodes.clear();
            }
            
            synchronized(dht) {
                for(SocketAddress addr : nodes) {
                    
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("RandomNodeAdder pinging: "+ addr);
                    }
                    
                    dht.ping(addr);
                }
            }
                
        }
        
        synchronized boolean isRunning() {
            return isRunning;
        }
        
        synchronized void stop() {
            if(timerTask != null) {
                // TODO: should this attempt to cancel the running task, or not?
                timerTask.cancel(true);
            }
            dhtNodes.clear();
            isRunning = false;
        }
    }
    
    /**
     * A Host Filter that delegates to RouterService's filter
     */
    private class FilterDelegate implements HostFilter {
        
        public boolean allow(SocketAddress addr) {
            return dhtControllerFacade.allow(addr);
        }
    }

}
