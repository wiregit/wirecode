package com.limegroup.gnutella.dht.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.DHTBootstrapper;
import com.limegroup.gnutella.dht.DHTController;
import com.limegroup.gnutella.dht.DHTFilterDelegate;
import com.limegroup.gnutella.dht.LimeMessageDispatcherImpl;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.FixedSizeLIFOSet;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.util.BucketUtils;

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
abstract class AbstractDHTController implements DHTController {
    
    protected final Log LOG = LogFactory.getLog(getClass());
    
    /**
     * The instance of the DHT
     */
    private MojitoDHT dht;
    
    /**
     * Whether or not the DHT controlled by this controller is running.
     */
    private boolean running = false;

    /**
     * The DHT bootstrapper instance.
     */
    protected final DHTBootstrapper bootstrapper;
    
    /**
     * The random node adder.
     */
    private final RandomNodeAdder dhtNodeAdder;
    
    public AbstractDHTController() {
        bootstrapper = new LimeDHTBootstrapper(this);
        dhtNodeAdder = new RandomNodeAdder();
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
                && !RouterService.isConnected())) {
            return;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Initializing the DHT");
        }
        
        try {
            InetAddress addr = InetAddress.getByAddress(RouterService.getAddress());
            int port = RouterService.getPort();
            dht.bind(new InetSocketAddress(addr, port));
            dht.start();
            running = true;
            bootstrapper.bootstrap();
        } catch (IOException err) {
            LOG.error(err);
        }
    }
    
    /**
     * Shuts down the dht. If this is an active node, it sends the updated capabilities
     * to its ultrapeers and persists the DHT. Otherwise, it just saves a list of MRS nodes
     * to bootstrap from for the next session.
     * 
     */
    public void stop(){
        if (!isRunning()) {
            return;
        }
        
        running = false;
        
        LOG.debug("Shutting down DHT Controller");
        
        if(LOG.isTraceEnabled()) {
            LOG.trace("Shut down trace: ", new Exception());
        }
        
        bootstrapper.stop();
        dhtNodeAdder.stop();
        
        if (dht != null) {
            dht.stop();
        }
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
        if (dht.isBootstrapped()) {
            return;
        }
        
        bootstrapper.addPassiveNode(hostAddress);
    }
    
    /**
     * Returns a list of the Most Recently Seen nodes from the Mojito 
     * routing table.
     * 
     * @param numNodes The number of nodes to return
     * @param excludeLocal true to exclude the local node
     * @return A list of DHT <tt>IpPorts</tt>
     */
    protected List<IpPort> getMRSNodes(int numNodes, boolean excludeLocal){
        Context dhtContext = (Context)dht; 
        List<Contact> nodes = BucketUtils.getMostRecentlySeenContacts(
                dhtContext.getRouteTable().getActiveContacts(), numNodes + 1); //it will add the local node!
        
        KUID localNode = dhtContext.getLocalNodeID();
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
        return running;
    }
    
    public boolean isBootstrapped() {
		return dht.isBootstrapped();
	}

	public boolean isWaitingForNodes() {
        return bootstrapper.isWaitingForNodes();
    }
    
    /**
     * Sets the MojitoDHT instance. Meant to be called once!
     */
    protected void setMojitoDHT(MojitoDHT dht) {
        
        assert (this.dht == null);
        
        dht.setMessageDispatcher(LimeMessageDispatcherImpl.class);
        dht.setThreadFactory(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                return new ManagedThread(runnable);
            }
        });
        dht.setHostFilter(new DHTFilterDelegate());
        
        this.dht = dht;
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
        
        CapabilitiesVM.reconstructInstance();
        RouterService.getConnectionManager().sendUpdatedCapabilities();
    }
    
    /**
     * Returns the RandomNodeAdder instance. For testing only!
     */
    RandomNodeAdder getRandomNodeAdder() {
        return dhtNodeAdder;
    }
    
    /**
     * A helper class to easily go back and forth 
     * from the DHT's RemoteContact to Gnutella's IpPort
     */
    private static class IpPortRemoteContact implements IpPort {
        
        private final InetAddress nodeAddress;
        
        private final int port;
        
        public IpPortRemoteContact(Contact node) {
            
            if(!(node.getContactAddress() instanceof InetSocketAddress)) {
                throw new IllegalArgumentException("Contact not instance of InetSocketAddress");
            }
            
            InetSocketAddress addr = (InetSocketAddress) node.getContactAddress();
            this.nodeAddress = addr.getAddress();
            this.port = addr.getPort();
        }
        
        public String getAddress() {
            return nodeAddress.getHostAddress();
        }

        public InetAddress getInetAddress() {
            return nodeAddress;
        }

        public int getPort() {
            return port;
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
        
        private TimerTask timerTask;
        
        private boolean isRunning;
        
        public RandomNodeAdder() {
            dhtNodes = new FixedSizeLIFOSet<SocketAddress>(MAX_SIZE);
        }
        
        public synchronized void start() {
            if(isRunning) {
                return;
            }
            long delay = DHTSettings.DHT_NODE_ADDER_DELAY.getValue();
            timerTask = RouterService.schedule(this, delay, delay);
            isRunning = true;
        }
        
        synchronized void addDHTNode(SocketAddress address) {
            dhtNodes.add(address);
        }
        
        public void run() {
            
            List<SocketAddress> nodes = null;
            synchronized (this) {
                
                if(!running) {
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
            timerTask.cancel();
            dhtNodes.clear();
            isRunning = false;
        }
    }
}