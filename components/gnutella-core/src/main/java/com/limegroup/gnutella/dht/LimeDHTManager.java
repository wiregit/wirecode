package com.limegroup.gnutella.dht;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.LifecycleListener;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.ThreadFactory;
import com.limegroup.mojito.event.BootstrapListener;

/**
 * The manager for the LimeWire Gnutella DHT. 
 * A node should connect to the DHT only if it has been designated as capable by the <tt>NodeAssigner</tt> 
 * or if it is forced to. Once the node is a DHT node, if <tt>EXCLUDE_ULTRAPEERS</tt> is set to true, 
 * it should no try to connect as an ultrapeer (@see RouterService.isExclusiveDHTNode()). 
 *
 * The current implementation is dependant on the MojitoDHT. TODO: create a more general DHT interface
 */
public class LimeDHTManager implements LifecycleListener {
    
    private static final Log LOG = LogFactory.getLog(LimeDHTManager.class);
    
    /**
     * The file to persist this Mojito DHT
     */
    private static final File FILE = new File(CommonUtils.getUserSettingsDir(), "mojito.dat");
    
    private MojitoDHT dht;

    private volatile boolean running = false;
    
    /**
     * A boolean to represent the state when we have failed last bootstrap
     * and are waiting for new bootstrap hosts
     */
    private volatile boolean waiting = false;
    
    /**
     * A list of DHT bootstrap hosts comming from the Gnutella network
     */
    private volatile LinkedList bootstrapHosts = new LinkedList();
    
    /**
     * A list of bootstrap hosts used in the last bootstrap attempt
     */
    private ArrayList previousBootstrapHosts;
    
    
    public LimeDHTManager() {
        
        if (FILE.exists() && FILE.isFile()) {
            try {
                FileInputStream in = new FileInputStream(FILE);
                dht = MojitoDHT.load(in);
                in.close();
            } catch (FileNotFoundException e) {
                LOG.error("FileNotFoundException", e);
            } catch (ClassNotFoundException e) {
                LOG.error("ClassNotFoundException", e);
            } catch (IOException e) {
                LOG.error("IOException", e);
            }
        }
        
        if (dht == null) {
            dht = new MojitoDHT("LimeMojitoDHT");
        }
        
        dht.setMessageDispatcher(LimeMessageDispatcherImpl.class);
        dht.setThreadFactory(new ThreadFactory() {
            public Thread createThread(Runnable runnable, String name) {
                return new ManagedThread(runnable, name);
            }
        });
    }
    
    /**
     * Initializes the Mojito DHT and connects it to the network in either passive mode
     * or active mode if we are not firewalled.
     * The initialization preconditions are the following:
     * 1) We are DHT_CAPABLE OR FORCE_DHT_CONNECT is true 
     * 2) We have stable Gnutella connections
     * 3) We are not an ultrapeer while excluding ultrapeers from the network
     * 4) We are not already connected or trying to bootstrap
     * 
     * @param forcePassive true to connect to the DHT in passive mode
     */
    public synchronized void init(boolean forcePassive) {
        if (running) {
            return;
        }

        if (!DHTSettings.DHT_CAPABLE.getValue() 
                && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Cannot initialize DHT - node is not DHT capable");
            }
            return;
        }
        
        if(!RouterService.isConnected()) {//TODO Replace with: RouterService.isStableState();
            if(LOG.isDebugEnabled()) {
                LOG.debug("Cannot initialize DHT - node is not connected to the Gnutella network");
            }
            return;
        }
        
        if (DHTSettings.EXCLUDE_ULTRAPEERS.getValue() 
                && RouterService.isSupernode()) {
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Cannot initialize DHT - Node is allready an ultrapeer");
            }
            return;
        }
        
        running = true;
        //set firewalled status
        if (forcePassive){
            dht.setFirewalled(true);
        } else {
            dht.setFirewalled(RouterService.acceptedIncomingConnection());
        }
        
        try {
            InetAddress addr = InetAddress.getByAddress(RouterService.getAddress());
            int port = RouterService.getPort();
            dht.bind(new InetSocketAddress(addr, port));
            dht.start();
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
        synchronized (bootstrapHosts) {
            previousBootstrapHosts = new ArrayList(bootstrapHosts);
        }
        try {
            dht.bootstrap(previousBootstrapHosts, new BootstrapListener() {
                public synchronized void noBootstrapHost() {
                    synchronized (bootstrapHosts) {
                        bootstrapHosts.removeAll(previousBootstrapHosts);
                        waiting = true;
                    }
                }

                public void phaseOneComplete(long time) {
                    //notify listeners
                }

                public void phaseTwoComplete(boolean foundNodes, long time) {
                    //notify listeners
                }
            });
        } catch (IOException err) {
            LOG.error(err);
        }
    }
    
    /**
     * Shuts down the dht and persists it
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
        try {
            FileOutputStream out = new FileOutputStream(FILE);
            dht.store(out);
            out.close();
        } catch (IOException err) {
            LOG.error("IOException", err);
        }
    }
    
    /**
     * Adds a host to the head of an ordered list of boostrap hosts.
     * If the manager is waiting for hosts, this method tries to bootstrap 
     * immediately after.
     * 
     * @param hostAddress The SocketAddress of the new bootstrap host.
     */
    public synchronized void addBootstrapHost(SocketAddress hostAddress) {
        synchronized (bootstrapHosts) {
            // TODO: as a param? Keep bootstrap list small because it should be updated often
            if(bootstrapHosts.size() > 10) {
                bootstrapHosts.removeLast();
            }
            //always put/replace the host to the head of the list
            bootstrapHosts.remove(hostAddress);
            bootstrapHosts.addFirst(hostAddress);
        }
        if(waiting) {
            waiting = false;
            bootstrap();
        }
    }
    
    public void removeBootstrapHost(SocketAddress hostAddress) {
        synchronized (bootstrapHosts) {
            bootstrapHosts.remove(hostAddress);
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void setFirewalled(boolean firewalled) {
        dht.setFirewalled(firewalled);
    }

    /**
     * Initializes the DHT when LW is stably connected to the Gnutella network. 
     * Shuts the DHT down if we got promoted to ultrapeer and we want to exclude them
     * or if we got disconnected from the network.
     */
    public void handleLifecycleEvent(LifecycleEvent evt) {
        //TODO: replace isConnectedEvent with isStableEvent
        if(evt.isConnectedEvent()) {
            if(!running) {
                init(false);
            } else {
                //protect against change of state
                if(DHTSettings.EXCLUDE_ULTRAPEERS.getValue() 
                        && RouterService.isSupernode()) {
                    shutdown();
                }
                return;
            }
        } else if(evt.isDisconnectedEvent() || evt.isNoInternetEvent()) {
            if(running) {
                shutdown();
            }
        } else {
            return;
        }
    }
    
    public MojitoDHT getMojitoDHT() {
        return dht;
    }
    
    public void addressChanged() {
        if(!running) return;
        dht.stop();
        init(false);
   }
}
