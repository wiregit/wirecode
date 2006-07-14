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
import com.limegroup.gnutella.dht.DHTController;
import com.limegroup.gnutella.dht.DHTNodeFetcher;
import com.limegroup.gnutella.dht.LimeMessageDispatcherImpl;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.manager.BootstrapManager.BootstrapException;

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
     * The Gnutella DHT node fetcher 
     */
    private DHTNodeFetcher dhtNodeFetcher;

    /**
     * The instance of the DHT
     */
    protected MojitoDHT dht;

    protected volatile boolean running = false;
    
    /**
     * A boolean to represent the state when we have failed last bootstrap
     * and are waiting for new bootstrap hosts
     */
    protected volatile boolean waiting = false;
    
    /**
     * A list of DHT bootstrap hosts comming from the Gnutella network
     */
    private volatile LinkedList<SocketAddress> bootstrapHosts 
            = new LinkedList<SocketAddress>();
    
    public AbstractDHTController() {
        init();
    }
    
    /**
     * Start the Mojito DHT and connects it to the network in either passive mode
     * or active mode if we are not firewalled.
     * The start preconditions are the following:
     * 1) if we want to actively connect: We are DHT_CAPABLE OR FORCE_DHT_CONNECT is true 
     * 2) We are not an ultrapeer while excluding ultrapeers from the active network
     * 3) We are not already connected or trying to bootstrap
     * 
     * @param activeMode true to connect to the DHT in active mode
     */
    protected void start(boolean activeMode) {
        if (running) {
            return;
        }
        
        if(DHTSettings.DISABLE_DHT_NETWORK.getValue() 
                || DHTSettings.DISABLE_DHT_USER.getValue()) { 
            return;
        }
        
        //set firewalled status
        dht.setFirewalled(!activeMode);
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Initializing the DHT");
        }
        
        try {
            InetAddress addr = InetAddress.getByAddress(RouterService.getAddress());
            int port = RouterService.getPort();
            dht.bind(new InetSocketAddress(addr, port));
            dht.start();
            running = true;
            
            bootstrap();
        } catch (IOException err) {
            LOG.error(err);
        }
    }
    
    /**
     * Tries to bootstrap of a list of bootstrap hosts. 
     * If bootstraping fails, the manager clears the list and 
     * puts itself in a waiting state until new hosts are added. 
     * 
     */
    private void bootstrap() {
        
        //append SIMPP host to the end of the list if we have any
        SocketAddress simppBootstrapHost = getSIMPPHost();
        if(simppBootstrapHost != null) {
            bootstrapHosts.add(simppBootstrapHost);
        }
        
        dht.bootstrap(bootstrapHosts, new BootstrapListener() {
            public void handleResult(BootstrapEvent result) {
                waiting = false;
                
                // Notify our connections that we are now a full DHT node 
                sendUpdatedCapabilities();
                //TODO here we should also cancel the DHTNodeFetcher because it's not used anymore
            }
            
            public void handleException(Exception ex) {
                if (ex instanceof BootstrapException) {
                    BootstrapException bex = (BootstrapException)ex;
                    List<SocketAddress> failedHosts = bex.getFailedHostList();
                    synchronized(bootstrapHosts) {
                        bootstrapHosts.removeAll(failedHosts);
                        if(!bootstrapHosts.isEmpty()) {
                            //hosts were added --> try again
                            bootstrap();
                        } else {
                            if(dhtNodeFetcher == null) {
                                dhtNodeFetcher = new DHTNodeFetcher(AbstractDHTController.this);
                            }
                            waiting = true;
                            //send UDPPings -- non blocking
                            dhtNodeFetcher.requestDHTHosts();
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Shuts down the dht. If this is an active node, it sends the updated capabilities
     * to its ultrapeers and persists the DHT. Otherwise, it just saves a list of MRS nodes
     * to bootstrap from for the next session.
     * 
     */
    public synchronized void shutdown(){
        if (!running) {
            return;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Shutting down DHT");
        }
        
        dht.stop();
        running = false;
        waiting = false;
    }
    
    /**
     * Adds a host to the head of a list of boostrap hosts ordered by Most Recently Seen.
     * If the manager is waiting for hosts, this method tries to bootstrap 
     * immediately after.
     * 
     * @param hostAddress The SocketAddress of the new bootstrap host.
     */
    public synchronized void addBootstrapHost(SocketAddress hostAddress) {
        boolean bootstrap = false;
        synchronized (bootstrapHosts) {
            //Keep bootstrap list small because it should be updated often
            if(bootstrapHosts.size() >= 20) {
                bootstrapHosts.removeLast();
            }
            
            //always put/replace the host to the head of the list
            bootstrapHosts.remove(hostAddress);
            bootstrapHosts.addFirst(hostAddress);

            System.out.println("adding: "+ hostAddress);
            System.out.println("waiting: "+ waiting);

            bootstrap = waiting;
            waiting = false;
        }
        
        if(bootstrap) {
            bootstrap();
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Sets the mode 
     * 
     * @param passive
     */
    public void setPassive(boolean passive) {
        boolean wasPassive = dht.isFirewalled();
        if((passive && wasPassive) || (!passive && !wasPassive)) {
            return; //no change
        }
        
        boolean wasRunning = running;
        shutdown();
        
        //we are becoming active: load dht from last active session
        if(wasPassive && !passive) {
            DHTSettings.PERSIST_DHT.setValue(true);
            init();
        } 
        //we are becoming passive: start new DHT with new nodeID so that 
        //the node is not part of the DHT anymore
        else if(!wasPassive && passive) {
            DHTSettings.PERSIST_DHT.setValue(false);
            init();
        } 
        
        if(wasRunning) {
            start(!passive);
        }
    }

    /**
     * Shuts the DHT down if we got disconnected from the network.
     */
    public void handleLifecycleEvent(LifecycleEvent evt) {
        if(evt.isConnectedEvent()) {
            //protect against change of state
            if(DHTSettings.EXCLUDE_ULTRAPEERS.getValue() 
                    && RouterService.isSupernode()) {
                setPassive(true);
            }
            
            //we were waiting and had no connection to the gnutella network
            //now we do --> start sending UDP pings to request bootstrap nodes
            if(waiting) {
                dhtNodeFetcher.requestDHTHosts();
            }
        } else if(evt.isDisconnectedEvent() || evt.isNoInternetEvent()) {
            if(running && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
                shutdown();
            }
        }
    }
    
    /**
     * Gets the SIMPP host responsible for the keyspace containing the local node ID
     * 
     * @return
     */
    private SocketAddress getSIMPPHost(){
        String[] simppHosts = DHTSettings.DHT_BOOTSTRAP_HOSTS.getValue();
        List<SocketAddress> hostList = new ArrayList<SocketAddress>(simppHosts.length);

        for (String hostString : simppHosts) {
            int index = hostString.indexOf(":");
            if(index < 0 || index == hostString.length()-1) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(new UnknownHostException("invalid host: " + hostString));
                }
                
                continue;
            }
            
            try {
                String host = hostString.substring(0, index);
                int port = Integer.parseInt(hostString.substring(index+1).trim());
                InetSocketAddress addr = new InetSocketAddress(host, port);
                hostList.add(addr);
            } catch(NumberFormatException nfe) {
                LOG.error(new UnknownHostException("invalid host: " + hostString));
            }
        }

        if(hostList.isEmpty()) {
            return null;
        }
        
        //each host in the list is responsible for a subspace of the keyspace
        int localPrefix = (int)((dht.getLocalNodeID().getBytes()[0] & 0xF0) >> 4);
        
        int index = (int)((float)hostList.size()/15f * localPrefix) % hostList.size();
        return hostList.get(index);
    }
    
    public MojitoDHT getMojitoDHT() {
        return dht;
    }
    
    public int getDHTVersion() {
        return dht.getVersion();
    }

    public boolean isWaiting() {
        return waiting;
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
    
    public abstract void start();
    
    public abstract boolean isActiveNode();

    /**
     * Sends the updated capabilities to our ultrapeers -- only if we are an active node!
     */
    protected abstract void sendUpdatedCapabilities();
    
    /*** End abstract methods ***/
    
    protected class IpPortContactNode implements IpPort {
        
        private final InetAddress nodeAddress;
        
        private final int port;
        
        public IpPortContactNode(Contact node) {
            InetSocketAddress addr = (InetSocketAddress) node.getSocketAddress();
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