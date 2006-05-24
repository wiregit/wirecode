package com.limegroup.gnutella.dht;

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
    
    private MojitoDHT dht;

    private volatile boolean running = false;
    
    private volatile LinkedList bootstrapHosts = new LinkedList();
    
    public LimeDHTManager() {
        dht = new MojitoDHT("LimeMojitoDHT");
        
        dht.setMessageDispatcher(LimeMessageDispatcherImpl.class);
        dht.setThreadFactory(new ThreadFactory() {
            public Thread createThread(Runnable runnable, String name) {
                return new ManagedThread(runnable, name);
            }
        });
    }
    
    public synchronized void init(boolean passive) {
        if(running) {
            return;
        }

        if(!DHTSettings.DHT_CAPABLE.getValue() && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Cannot initialize DHT - node is not DHT capable");
            }
            return;
        }
        
        if(RouterService.isConnecting()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Cannot initialize DHT - node is not connected to the Gnutella network");
            }
            return;
        }
        
        if(DHTSettings.EXCLUDE_ULTRAPEERS.getValue() && RouterService.isSupernode()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Cannot initialize DHT - Node is allready an ultrapeer");
            }
            return;
        }
        
        running = true;
        //set firewalled status
        if(passive){
            dht.setFirewalled(true);
        } else {
            dht.setFirewalled(RouterService.acceptedIncomingConnection());
        }
        
        try {
            InetAddress addr = InetAddress.getByAddress(RouterService.getAddress());
            int port = RouterService.getPort();
            dht.bind(new InetSocketAddress(addr, port));
            
            //TODO initialize the DHT here: bind, set Address, bootstrap etc.
            dht.start();
        } catch (IOException err) {
            LOG.error(err);
        }
    }
    
    public synchronized void shutdown(){
        if(!running) {
            return;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Shutting down DHT");
        }
        dht.stop();
        running = false;
    }
    
    public void addBootstrapHost(SocketAddress hostAddress) {
        synchronized (bootstrapHosts) {
            if(bootstrapHosts.size() > 10) {//TODO: as a param? Keep bootstrap list small because it should be updated often
                bootstrapHosts.removeLast();
            }
            bootstrapHosts.addFirst(hostAddress);
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

    public void handleLifecycleEvent(LifecycleEvent evt) {
        if(evt.isConnectedEvent()) {
            if(!running) {
                init(false);
            } else {
                //protect against change of state
                if(RouterService.isSupernode()) {
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

    /** we need a timer task to see: 
    1. We weren't able to bootstrap ==> check if we have new bootstrap nodes
    2. We were firewalled ==> see if that changed (see acceptedIncomingChanged)
        **/
}
