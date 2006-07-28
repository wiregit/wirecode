package com.limegroup.gnutella.dht.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.LifecycleListener;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.DHTBootstrapper;
import com.limegroup.gnutella.dht.DHTController;
import com.limegroup.gnutella.dht.DHTNodeFetcher;
import com.limegroup.gnutella.dht.LimeMessageDispatcherImpl;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.event.BootstrapEvent.Type;

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
 * The current implementation is dependant on the MojitoDHT. 
 */
abstract class AbstractDHTController implements DHTController, LifecycleListener {
    
    private static final Log LOG = LogFactory.getLog(AbstractDHTController.class);
    
    /**
     * The instance of the DHT
     */
    protected MojitoDHT dht;

    private volatile boolean running = false;
    
    protected final DHTBootstrapper dhtBootstrapper;
    
    public AbstractDHTController() {
        dhtBootstrapper = new LimeDHTBootstrapper(this);
        //delegate
        init();
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
    public synchronized void start() {
        if (running || (!DHTSettings.FORCE_DHT_CONNECT.getValue() 
                && !RouterService.isConnected())) {
            return;
        }
        
        if(DHTSettings.DISABLE_DHT_NETWORK.getValue() 
                || DHTSettings.DISABLE_DHT_USER.getValue()) { 
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
            
            dhtBootstrapper.bootstrap(dht);
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
    public synchronized void stop(){
        if (!running) {
            return;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Shutting down DHT");
        }
        
        dhtBootstrapper.stop();
        
        if(dht != null) {
            dht.stop();
        }
        
        running = false;
    }
    
    /**
     * Adds a host to the head of a list of boostrap hosts ordered by Most Recently Seen.
     * If the manager is waiting for hosts, this method tries to bootstrap 
     * immediately after.
     * 
     * @param hostAddress The SocketAddress of the new bootstrap host.
     */
    public void addBootstrapHost(SocketAddress hostAddress) {
        dhtBootstrapper.addBootstrapHost(hostAddress);
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public boolean isWaitingForNodes() {
        return dhtBootstrapper.isWaitingForNodes();
    }
    
    /**
     * Shuts the DHT down if we got disconnected from the network.
     * The nodeAssigner will take care of restarting this DHT node if 
     * it still qualifies.
     * 
     */
    public void handleLifecycleEvent(LifecycleEvent evt) {
        if(evt.isDisconnectedEvent() || evt.isNoInternetEvent()) {
            if(running && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
                stop();
            }
        }
    }
    
    public MojitoDHT getMojitoDHT() {
        return dht;
    }
    
    public int getDHTVersion() {
        return dht.getVersion();
    }

    public void setLimeMessageDispatcher() {
        dht.setMessageDispatcher(LimeMessageDispatcherImpl.class);
        dht.setThreadFactory(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                return new ManagedThread(runnable);
            }
        });
    }
    
    /*** Abstract methods: ***/
    
    public abstract List<IpPort> getActiveDHTNodes(int maxNodes);
    
    public abstract void init();
    
    public abstract boolean isActiveNode();

    /**
     * Sends the updated capabilities to our ultrapeers -- only if we are an active node!
     */
    public abstract void sendUpdatedCapabilities();
    
    /*** End abstract methods ***/
    
    protected static class IpPortContactNode implements IpPort {
        
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
}