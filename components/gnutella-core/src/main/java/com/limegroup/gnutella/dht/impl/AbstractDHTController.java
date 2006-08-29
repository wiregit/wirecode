package com.limegroup.gnutella.dht.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.DHTBootstrapper;
import com.limegroup.gnutella.dht.DHTController;
import com.limegroup.gnutella.dht.LimeMessageDispatcherImpl;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.FixedSizeLIFOSet;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.util.BucketUtils;

/**
 * The manager for the LimeWire Gnutella DHT. 
 * A node should connect to the DHT only if it has previously been designated as capable 
 * by the <tt>NodeAssigner</tt> or if it is forced to. 
 * Once the node is a DHT node, if <tt>EXCLUDE_ULTRAPEERS</tt> is set to true, 
 * it should no try to connect as an ultrapeer (@see RouterService.isExclusiveDHTNode()). 
 *
 * The NodeAssigner should be the only class to have the authority to 
 * initialize the DHT and connect to the network.
 * 
 * This manager can be in one of the four following states:
 * 1) not running.
 * 2) running and bootstrapping: the dht is trying to bootstrap.
 * 3) running and waiting: the dht has failed the bootstrap and is waiting for additional bootstrap hosts.
 * 3) running and bootstrapped.
 * 
 * The current implementation is specific to the Mojito DHT. 
 */
abstract class AbstractDHTController implements DHTController {
    
    protected final Log LOG = LogFactory.getLog(getClass());
    
    /**
     * The instance of the DHT
     */
    private MojitoDHT dht;

    protected final DHTBootstrapper bootstrapper;
    
    private RandomNodeAdder dhtNodeAdder;
    
    public AbstractDHTController() {
        bootstrapper = new LimeDHTBootstrapper(this);
    }

    /**
     * Start the Mojito DHT and connects it to the network in either passive mode
     * or active mode if we are not firewalled.
     * The start preconditions are the following:
     * 1) We are not already connected AND we have at least one initialized Gnutella connection
     * 1) if we want to actively connect: We are DHT_CAPABLE OR FORCE_DHT_CONNECT is true 
     * 2) We are not an ultrapeer while excluding ultrapeers from the active network
     * 3) We are not already connected or trying to bootstrap
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
        
        LOG.debug("Shutting down DHT Controller");
        
        if(LOG.isTraceEnabled()) {
            LOG.trace("Shut down trace: ", new Exception());
        }
        
        bootstrapper.stop();
        
        if (dhtNodeAdder != null) {
            dhtNodeAdder.stop();
        }
        
        if (dht != null) {
            dht.stop();
            dht = null;
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
        if (!isRunning()) {
            return;
        }
        
        if(!dht.isBootstrapped()){
            bootstrapper.addBootstrapHost(hostAddress);
        } else if(addToDHTNodeAdder){
            if(dhtNodeAdder == null) {
                dhtNodeAdder = new RandomNodeAdder();
            }
            
            dhtNodeAdder.start();
            dhtNodeAdder.addDHTNode(hostAddress);
        }
    }
    
    public void addActiveDHTNode(SocketAddress hostAddress) {
        addActiveDHTNode(hostAddress, true);
    }
    
    public void addPassiveDHTNode(SocketAddress hostAddress) {
        if (!isRunning() || dht.isBootstrapped()) {
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
                dhtContext.getRouteTable().getLiveContacts(), numNodes + 1); //it will add the local node!
        
        KUID localNode = dhtContext.getLocalNodeID();
        List<IpPort> ipps = new ArrayList<IpPort>();
        for(Contact cn : nodes) {
            if(excludeLocal && cn.getNodeID().equals(localNode)) {
                continue;
            }
            ipps.add(new IpPortContactNode(cn));
        }
        return ipps;
    }
    
    public boolean isRunning() {
        if (dht == null) {
            return false;
        }
        
        return dht.isRunning();
    }
    
    public boolean isWaitingForNodes() {
        return bootstrapper.isWaitingForNodes();
    }
    
    /**
     * Sets the MojitoDHT instance. Meant to be called once!
     */
    protected void setMojitoDHT(MojitoDHT dht) {
        assert (dht != null);
        
        dht.setMessageDispatcher(LimeMessageDispatcherImpl.class);
        dht.setThreadFactory(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                return new ManagedThread(runnable);
            }
        });
        
        this.dht = dht;
    }
    
    public MojitoDHT getMojitoDHT() {
        return dht;
    }
    
    public void sendUpdatedCapabilities() {
        CapabilitiesVM.reconstructInstance();
        RouterService.getConnectionManager().sendUpdatedCapabilities();
    }
    
    /**
     * 
     */
    private static class IpPortContactNode implements IpPort {
        
        private final InetAddress nodeAddress;
        
        private final int port;
        
        public IpPortContactNode(Contact node) {
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
     * by sending a Mojito ping to the last 20 DHT nodes seen in the Gnutella
     * network. It is effectively randomly adding them to the DHT routing table.
     * 
     */
    private class RandomNodeAdder implements Runnable {
        
        private Set<SocketAddress> dhtNodes;
        
        private boolean running;
        
        public RandomNodeAdder() {
            dhtNodes = new FixedSizeLIFOSet<SocketAddress>(30);
            long delay = DHTSettings.DHT_NODE_ADDER.getValue();
            RouterService.schedule(this, delay, delay);
        }
        
        public void start() {
            running = true;
        }
        
        public void addDHTNode(SocketAddress address) {
            synchronized(dhtNodes) {
                dhtNodes.add(address);
            }
        }
        
        public void run() {
            if(!running) {
                return;
            }
            
            synchronized(dhtNodes) {
                for(SocketAddress addr : dhtNodes) {
                    dht.ping(addr);
                }
            }
        }
        
        public boolean isRunning() {
            return running;
        }
        
        //TODO: Use zlati's cancellable timer task when merging back
        public void stop() {
            running = false;
        }
    }
}