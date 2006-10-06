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
 * This DHT manager starts either an active or a passive DHT controller.
 * It also handles switching from one mode to the other
 * 
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
    private DHTController controller = null;
    
    public synchronized void start(boolean activeMode) {
        
        if (controller == null || (controller.isActiveNode() != activeMode)) {
            if (controller != null) {
                controller.stop();
            }
            
            if (activeMode) {
                controller = new ActiveDHTNodeController(vendor, version);
            } else {
                controller = new PassiveDHTNodeController(vendor, version);
            }
        }
        controller.start();
    }
    
    public synchronized void addActiveDHTNode(SocketAddress hostAddress) {
        if (controller != null) {
            controller.addActiveDHTNode(hostAddress);
        }
    }
    
    public synchronized void addPassiveDHTNode(SocketAddress hostAddress) {
        if (controller != null) {
            controller.addPassiveDHTNode(hostAddress);
        }
    }

    public synchronized void addressChanged() {
        if(controller == null || !controller.isRunning()) {
            return;
        }
        
        // Restart the DHT (will get the new adress from RouterService)
        controller.stop();
        controller.start();
    }
    
    public synchronized void sendUpdatedCapabilities() {
        if(controller == null || !controller.isRunning()) {
            return;
        }
        
        controller.sendUpdatedCapabilities();
    }
    
    public synchronized List<IpPort> getActiveDHTNodes(int maxNodes){
        if(controller == null) {
            return Collections.emptyList();
        }
        
        return controller.getActiveDHTNodes(maxNodes);
    }
    
    public synchronized boolean isActiveNode() {
        if(controller != null) {
            return (controller.isActiveNode() 
                    && controller.isRunning());
        }
     
        return false;
    }
    
    public synchronized void stop(){
        if (controller != null) {
            controller.stop();
            controller = null;
        }
    }
    
    public synchronized boolean isRunning() {
        if(controller != null) {
            return controller.isRunning();
        }
        return false;
    }
    
    public synchronized boolean isBootstrapped() {
        if(controller != null) {
            return controller.getMojitoDHT().isBootstrapped();
        }
        return false;
    }
    
    public synchronized boolean isWaitingForNodes() {
        if(controller != null) {
            return controller.isWaitingForNodes();
        }
        return false;
    }

    /**
     * This getter is for internal use only. The Mojito DHT is not meant to
     * be handled or passed around independently, as only the DHT controllers 
     * know how to interact correctly with it.
     * 
     */
    public synchronized MojitoDHT getMojitoDHT() {
        if(controller != null) {
            return controller.getMojitoDHT();
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
        if(controller == null) {
            return;
        }
        
        if(evt.isDisconnectedEvent() || evt.isNoInternetEvent()) {
            if(controller.isRunning() 
                    && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
                controller.stop();
            }
            return;
        } 

        if(evt.isConnectionLifecycleEvent()) {
            controller.handleConnectionLifecycleEvent(evt);
        }
    }
    
    public int getVendor() {
        return vendor;
    }
    
    public int getVersion() {
        return version;
    }
}
