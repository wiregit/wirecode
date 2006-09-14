package com.limegroup.gnutella.dht.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.dht.DHTBootstrapper;
import com.limegroup.gnutella.dht.DHTController;
import com.limegroup.gnutella.dht.DHTNodeFetcher;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.FixedSizeLIFOSet;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.event.BootstrapEvent.EventType;

/**
 * 
 */
class LimeDHTBootstrapper implements DHTBootstrapper{
    
    private static final Log LOG = LogFactory.getLog(LimeDHTBootstrapper.class);
    
    /**
     * The DHT controller
     */
    private DHTController controller;
    
    /**
     * The Gnutella DHT node fetcher 
     */
    private DHTNodeFetcher dhtNodeFetcher;
    
    /**
     * A flag set to true when we are bootstraping from our persisted Routing Table
     */
    private AtomicBoolean bootstrappingFromRT = new AtomicBoolean(false);
    
    private AtomicBoolean waiting = new AtomicBoolean(false);
    
    /**
     * A list of DHT bootstrap hosts comming from the Gnutella network. 
     * Limit size to 50 for now.
     */
    private volatile Set<SocketAddress> bootstrapHosts = new FixedSizeLIFOSet<SocketAddress>(50);
    
    /**
     * The bootstrap's <tt>Future</tt> object
     */
    private DHTFuture<BootstrapEvent> bootstrapFuture;
    
    public LimeDHTBootstrapper(DHTController controller) {
        this.controller = controller;
    }
    
    /**
     * Boostraps in the following order:
     * 1) If we have received hosts from the Gnutella network, try them.
     * 2) Else try the persisted routing table.
     * 3) Else try the SIMPP list.
     * 4) Else start node fetcher and wait for hosts coming from the network.
     * 
     * If at any moment while bootstraping from the routing table 
     * we receive nodes from the network, it should pre-empt the existing
     * bootstrap and start with them. This is achieved by calling cancel on 
     * the future.
     * 
     */
    public void bootstrap() {
        
        MojitoDHT dht = controller.getMojitoDHT();
        
        if(dht.isBootstrapped()) {
            return;
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Starting bootstrap, bootstrapHosts: "+bootstrapHosts);
        }
        
        bootstrappingFromRT.set(bootstrapHosts.isEmpty());
        bootstrapFuture = dht.bootstrap(bootstrapHosts);
        bootstrapFuture.addDHTEventListener(new InitialBootstrapListener());
    }
    
    /**
     * Adds a host to the head of a list of boostrap hosts ordered by Most Recently Seen.
     * If this bootstrapper is waiting for hosts or is bootstrapping from the persisted RT, 
     * this method tries to bootstrap immediately after.
     * 
     * @param hostAddress The SocketAddress of the new bootstrap host.
     */
    public void addBootstrapHost(SocketAddress hostAddress) {
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Adding host: "+hostAddress);
        }
        
        synchronized(bootstrapHosts) {
            //Keep bootstrap list small because it should be updated often
            bootstrapHosts.add(hostAddress);
        }
        
        if(waiting.getAndSet(false) || bootstrappingFromRT.getAndSet(false)) {
            MojitoDHT dht = controller.getMojitoDHT();
            
            //we may have bootstrapped (Mojito internal -- see RandomBucketRefresher.run()) 
            //in the meanwhile
            if(dht.isBootstrapped()) {
                return;
            }
            
            LOG.debug("Cancelling bootstrap and starting over");

            bootstrapFuture.cancel(true);
            bootstrapFuture = dht.bootstrap(bootstrapHosts);
            bootstrapFuture.addDHTEventListener(new WaitingBootstrapListener());
        }
    }
    
    public void addPassiveNode(SocketAddress hostAddress) {
        if(dhtNodeFetcher == null || !waiting.get()) {
            return;
        }
        dhtNodeFetcher.requestDHTHosts(hostAddress);
    }

    public void stop() {
        
        LOG.debug("Stopping");
        
        if(bootstrapFuture != null) {
            bootstrapFuture.cancel(true);
        }
        
        waiting.set(false);
        bootstrappingFromRT.set(false);
        
    }
    
    public boolean isWaitingForNodes() {
        return waiting.get();
    }
    
    public boolean isBootstrappingFromRT() {
        return bootstrappingFromRT.get();
    }
    
    /**
     * Gets the SIMPP host responsible for the keyspace containing the local node ID
     * 
     * TODO: public for testing only
     * 
     * @return The SocketAddress of a SIMPP bootstrap host, or null if we don't have any.
     */
    public SocketAddress getSIMPPHost(){
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
                SocketAddress addr = new InetSocketAddress(host, port);
                hostList.add(addr);
            } catch(NumberFormatException nfe) {
                LOG.error(new UnknownHostException("invalid host: " + hostString));
            }
        }

        if(hostList.isEmpty()) {
            return null;
        }
        
        MojitoDHT dht = controller.getMojitoDHT();
        
        //each host in the list is responsible for a subspace of the keyspace
        //first 4 bits responsible for dividing the keyspace
        int localPrefix = (int)((dht.getLocalNodeID().getBytes()[0] & 0xF0) >> 4);
        //now map to hostlist size
        int index = (int)(((float)hostList.size()/16f) * localPrefix);
        return hostList.get(index);
    }
    
    private void handleSuccess() {
        // Notify our connections that we are now a bootstrapped DHT node 
        controller.sendUpdatedCapabilities();
    }
    
    private class InitialBootstrapListener implements BootstrapListener{
        public void handleResult(BootstrapEvent result) {
            
            if(EventType.BOOTSTRAP_PING_SUCCEEDED.equals(result.getEventType())) {
                LOG.debug("Initial bootstrap ping succeded");
                
                waiting.set(false);
                bootstrappingFromRT.set(false);
                return;
                
            } else if(EventType.BOOTSTRAP_SUCCEEDED.equals(result.getEventType())) {
                LOG.debug("Initial bootstrap completed");

                handleSuccess();
                return;
            } 

            MojitoDHT dht = controller.getMojitoDHT();
            
            //failure!
            if(bootstrappingFromRT.getAndSet(false)) {
                //we were bootstrapping from the RT AND failed: try SIMPP hosts
                SocketAddress simppBootstrapHost = getSIMPPHost();
                if(simppBootstrapHost != null) {
                    
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Bootstraping from SIMPP host: "+simppBootstrapHost);
                    }
                    
                    bootstrapFuture = dht.bootstrap(simppBootstrapHost);
                } else {
                    synchronized(bootstrapHosts) {
                        if(bootstrapHosts.isEmpty()) { //put ourselve in waiting mode and return
                            
                            LOG.debug("Starting Node fetcher and going to wait mode");
                            
                            waiting.set(true);
                            dhtNodeFetcher = new DHTNodeFetcher(LimeDHTBootstrapper.this);
                            dhtNodeFetcher.startTimerTask();
                            return;
                        } 
                    }
                    
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Retrying bootstrap with host list: "+bootstrapHosts);
                    }
                    
                    //otherwise try again
                    bootstrapFuture = dht.bootstrap(bootstrapHosts);
                }
                bootstrapFuture.addDHTEventListener(new WaitingBootstrapListener());
                return;
            }
            //we were not bootstrapping from RT. Try again!
            synchronized(bootstrapHosts) {
                
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Retrying bootstrap with host list: "+bootstrapHosts);
                }
                
                bootstrapHosts.removeAll(result.getFailedHosts());
                bootstrappingFromRT.set(bootstrapHosts.isEmpty());
                bootstrapFuture = dht.bootstrap(bootstrapHosts);
                bootstrapFuture.addDHTEventListener(this);
            }
        }
        
        public void handleThrowable(Throwable ex) {
            LOG.error("Throwable", ex);
            stop();
        }
    }
    
    private class WaitingBootstrapListener implements BootstrapListener{
        public void handleResult(BootstrapEvent result) {
            
            //this listener should never be called when bootstrapping from RT
            Assert.that(!bootstrappingFromRT.get());
            
            if(EventType.BOOTSTRAP_PING_SUCCEEDED.equals(result.getEventType())) {
                LOG.debug("Ping succeded in waitingBootstrapListener");
                
                waiting.set(false); //this will also stop the node fetcher
                return;
                
            } else if(EventType.BOOTSTRAP_SUCCEEDED.equals(result.getEventType())) {
                LOG.debug("Bootstrap succeded in waitingBootstrapListener");
                
                handleSuccess();
                return;
                
            }
                
            LOG.debug("hostlist: "+result.getFailedHosts()+" failed in waitingBootstrapListener");
            
            synchronized(bootstrapHosts) {
                bootstrapHosts.removeAll(result.getFailedHosts());
                if(bootstrapHosts.isEmpty()) {
                    waiting.set(true);
                    return;
                } 
            }
            
            MojitoDHT dht = controller.getMojitoDHT();
            bootstrapFuture = dht.bootstrap(bootstrapHosts);
            bootstrapFuture.addDHTEventListener(this);
        }
        
        public void handleThrowable(Throwable ex) {
            LOG.debug(ex);
            stop();
        }
    }
}
