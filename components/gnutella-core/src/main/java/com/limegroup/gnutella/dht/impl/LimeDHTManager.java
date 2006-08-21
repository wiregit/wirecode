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

public class LimeDHTManager implements DHTManager {
    
    //TODO: this will change
    private int version = ContextSettings.VERSION.getValue();
    
    private DHTController dhtController = null;
    
    public synchronized void start(boolean activeMode) {
        
        if (dhtController == null || (dhtController.isActiveNode() != activeMode)) {
            if (dhtController != null) {
                dhtController.stop();
            }
            
            if (activeMode) {
                dhtController = new ActiveDHTNodeController();
            } else {
                dhtController = new PassiveDHTNodeController();
            }
        }
        dhtController.start();
    }
    
    public void addDHTNode(SocketAddress hostAddress) {
        if (dhtController != null) {
            dhtController.addDHTNode(hostAddress);
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
    
    public List<IpPort> getActiveDHTNodes(int maxNodes){
        if(dhtController == null) {
            return Collections.emptyList();
        }
        
        return dhtController.getActiveDHTNodes(maxNodes);
    }
    
    public boolean isActiveNode() {
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
    
    public boolean isRunning() {
        if(dhtController != null) {
            return dhtController.isRunning();
        }
        return false;
    }
    
    public boolean isBootstrapped() {
        if(dhtController != null) {
            return dhtController.getMojitoDHT().isBootstrapped();
        }
        return false;
    }
    
    public boolean isWaitingForNodes() {
        if(dhtController != null) {
            return dhtController.isWaitingForNodes();
        }
        return false;
    }

    public MojitoDHT getMojitoDHT() {
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
    public void handleLifecycleEvent(LifecycleEvent evt) {
        if(dhtController == null) {
            return;
        }
        
        if(evt.isDisconnectedEvent() || evt.isNoInternetEvent()) {
            if(dhtController.isRunning() && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
                dhtController.stop();
            }
            return;
        } 

        if( evt.isConnectionLifecycleEvent() ) {
            dhtController.handleConnectionLifecycleEvent(evt);
        }
    }

    public int getVersion() {
        return version;
    }
}
