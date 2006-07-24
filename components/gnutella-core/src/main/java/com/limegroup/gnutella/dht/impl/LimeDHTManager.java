package com.limegroup.gnutella.dht.impl;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.dht.DHTController;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;

public class LimeDHTManager implements DHTManager {

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
            
            dhtController.start();
        }
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

    public synchronized void addBootstrapHost(SocketAddress hostAddress) {
        if (dhtController != null) {
            dhtController.addBootstrapHost(hostAddress);
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
    
    public synchronized List<IpPort> getActiveDHTNodes(int maxNodes){
        if(dhtController == null) {
            return Collections.emptyList();
        }
        
        return dhtController.getActiveDHTNodes(maxNodes);
    }
    
    public synchronized boolean isActiveNode() {
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
    
    public synchronized boolean isRunning() {
        if(dhtController != null) {
            return dhtController.isRunning();
        }
        return false;
    }
    
    public synchronized boolean isWaiting() {
        if(dhtController != null) {
            return dhtController.isWaiting();
        }
        return false;
    }
    
    public synchronized int getDHTVersion() {
        if(dhtController != null) {
            return dhtController.getDHTVersion();
        }
        return 0;
    }
    
    public synchronized MojitoDHT getMojitoDHT() {
        if(dhtController != null) {
            return dhtController.getMojitoDHT();
        }
        return null;
    }

    public synchronized void handleLifecycleEvent(LifecycleEvent evt) {
        if(dhtController != null) {
            dhtController.handleLifecycleEvent(evt);
        }
    }
}
