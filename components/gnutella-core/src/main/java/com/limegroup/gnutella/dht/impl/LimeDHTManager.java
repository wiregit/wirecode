package com.limegroup.gnutella.dht.impl;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.dht.DHTController;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.settings.ContextSettings;

/**
 * 
 */
public class LimeDHTManager implements DHTManager {
    
    /**
     * The Vendor code of this DHT Node
     */
    private int vendor = ContextSettings.VENDOR.getValue();
    
    /**
     * The Version of this DHT Node
     */
    private int version = ContextSettings.VERSION.getValue();
    
    /**
     * The DHTController instance
     */
    private DHTController dhtController = null;
    
    
    public synchronized void start(boolean activeMode) {
        
        if (dhtController == null || (dhtController.isActiveNode() != activeMode)) {
            if (dhtController != null) {
                dhtController.stop();
            }
            
            if (activeMode) {
                dhtController = new ActiveDHTNodeController(vendor, version);
            } else {
                dhtController = new PassiveDHTNodeController(vendor, version);
            }
        }
        dhtController.start();
    }
    
    public synchronized void addActiveDHTNode(SocketAddress hostAddress) {
        if (dhtController != null) {
            dhtController.addActiveDHTNode(hostAddress);
        }
    }
    
    public synchronized void addPassiveDHTNode(SocketAddress hostAddress) {
        if (dhtController != null) {
            dhtController.addPassiveDHTNode(hostAddress);
        }
    }

    public synchronized void addressChanged() {
        if(dhtController == null || !dhtController.isRunning()) {
            return;
        }
        //restart dht (will get the new adress from RouterService)
        dhtController.stop();
        dhtController.start();
    }
    
    public synchronized List<IpPort> getActiveDHTNodes(int maxNodes){
        if(dhtController == null) {
            return Collections.emptyList();
        }
        
        return dhtController.getActiveDHTNodes(maxNodes);
    }
    
    public synchronized boolean isActiveNode() {
        if(dhtController != null) {
            return (dhtController.isActiveNode() 
                    && dhtController.isRunning());
        }
     
        return false;
    }
    
    public synchronized void stop(){
        if (dhtController != null) {
            dhtController.stop();
            dhtController = null;
        }
    }
    
    public synchronized boolean isRunning() {
        if(dhtController != null) {
            return dhtController.isRunning();
        }
        return false;
    }
    
    public synchronized boolean isBootstrapped() {
        if(dhtController != null) {
            return dhtController.getMojitoDHT().isBootstrapped();
        }
        return false;
    }
    
    public synchronized boolean isWaitingForNodes() {
        if(dhtController != null) {
            return dhtController.isWaitingForNodes();
        }
        return false;
    }

    public synchronized MojitoDHT getMojitoDHT() {
        if(dhtController != null) {
            return dhtController.getMojitoDHT();
        }
        return null;
    }

    /**
     * Shuts the DHT down if we got disconnected from the network.
     * The nodeAssigner will take care of restarting this DHT node if 
     * it still qualifies.
     * 
     */
    public synchronized void handleLifecycleEvent(LifecycleEvent evt) {
        if(dhtController == null) {
            return;
        }
        
        if(evt.isDisconnectedEvent() || evt.isNoInternetEvent()) {
            if(dhtController.isRunning() 
                    && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
                dhtController.stop();
            }
            return;
        } 

        if(evt.isConnectionLifecycleEvent()) {
            dhtController.handleConnectionLifecycleEvent(evt);
        }
    }
    
    public int getVendor() {
        return vendor;
    }
    
    public int getVersion() {
        return version;
    }
}
