package com.limegroup.gnutella.dht.impl;

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.DHTController;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.settings.ContextSettings;

/**
 * This DHT manager starts either an active or a passive DHT controller.
 * It also handles switching from one mode to the other.
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
    private DHTController controller = new NullDHTController();
    
    public synchronized void start(boolean activeMode) {
    	
    	//controller already running in the correct mode?
    	if(controller.isRunning() 
    			&& (controller.isActiveNode() == activeMode)) {
    		return;
    	}
        
    	controller.stop();

    	if (activeMode) {
            controller = new ActiveDHTNodeController(vendor, version);
        } else {
            controller = new PassiveDHTNodeController(vendor, version);
        }

        controller.start();
    }
    
    public synchronized void addActiveDHTNode(SocketAddress hostAddress) {
        controller.addActiveDHTNode(hostAddress);
    }
    
    public synchronized void addPassiveDHTNode(SocketAddress hostAddress) {
        controller.addPassiveDHTNode(hostAddress);
    }

    public synchronized void addressChanged() {
        if(!controller.isRunning()) {
            return;
        }
        
        // Restart the DHT (will get the new adress from RouterService)
        controller.stop();
        controller.start();
    }
    
    public synchronized void sendUpdatedCapabilities() {
        if(!controller.isRunning()) {
            return;
        }
        
        controller.sendUpdatedCapabilities();
    }
    
    public synchronized List<IpPort> getActiveDHTNodes(int maxNodes){
        return controller.getActiveDHTNodes(maxNodes);
    }
    
    public synchronized boolean isActiveNode() {
        return (controller.isActiveNode() && controller.isRunning());
    }
    
    public synchronized void stop(){
        controller.stop();
        controller = new NullDHTController();
    }
    
    public synchronized boolean isRunning() {
        return controller.isRunning();
    }
    
    public synchronized boolean isBootstrapped() {
        return controller.isBootstrapped();
    }
    
    public synchronized boolean isWaitingForNodes() {
        return controller.isWaitingForNodes();
    }

    /**
     * This getter is for internal use only. The Mojito DHT is not meant to
     * be handled or passed around independently, as only the DHT controllers 
     * know how to interact correctly with it.
     * 
     */
    public synchronized MojitoDHT getMojitoDHT() {
        return controller.getMojitoDHT();
    }

    /**
     * Shuts the DHT down if we got disconnected from the network.
     * The nodeAssigner will take care of restarting this DHT node if 
     * it still qualifies.
     * 
     */
    public synchronized void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        if(evt.isDisconnectedEvent() || evt.isNoInternetEvent()) {
            if(controller.isRunning() 
                    && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
                controller.stop();
            }
            return;
        } 

        controller.handleConnectionLifecycleEvent(evt);
    }
    
    public int getVendor() {
        return vendor;
    }
    
    public int getVersion() {
        return version;
    }
}
