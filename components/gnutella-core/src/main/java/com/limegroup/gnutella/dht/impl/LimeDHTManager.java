package com.limegroup.gnutella.dht.impl;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.dht.DHTController;
import com.limegroup.gnutella.dht.DHTManager;
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
    
    public synchronized void switchMode(boolean toActiveMode) {
        if(dhtController == null || !dhtController.isRunning()) {
            return;
        }
        
        if(dhtController.isActiveNode() == toActiveMode) {
            return; //no change
        }
        
        start(toActiveMode);
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
        
        boolean wasRunning = dhtController.isRunning();
        
        dhtController.stop();
        
        if (wasRunning) {
            dhtController.start();
        }
    }
    
    public List<IpPort> getActiveDHTNodes(int maxNodes){
        if(dhtController == null) {
            return Collections.emptyList();
        }
        
        return dhtController.getActiveDHTNodes(maxNodes);
    }
    
    public boolean isActiveNode() {
        if(dhtController != null) {
            return dhtController.isActiveNode();
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

    public void handleLifecycleEvent(LifecycleEvent evt) {
        if(dhtController != null) {
            dhtController.handleLifecycleEvent(evt);
        }
    }

    public int getVersion() {
        return version;
    }
}
