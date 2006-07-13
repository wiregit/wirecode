package com.limegroup.gnutella.dht.impl;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.LifecycleListener;
import com.limegroup.gnutella.dht.DHTController;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;

public class LimeDHTManager implements LifecycleListener{

    private DHTController dhtController;
    
    public LimeDHTManager() {}
    
    public void startDHT(boolean activeMode) {
        
        if(dhtController == null) {
            if(activeMode) {
                dhtController = new ActiveDHTNodeController();
            } else {
                dhtController = new PassiveDHTNodeController();
            }
        } else if(activeMode 
                && !dhtController.isActiveNode()) {
            dhtController.shutdown();
            dhtController = new ActiveDHTNodeController();
        } else if(!activeMode 
                && dhtController.isActiveNode()) {
            dhtController.shutdown();
            dhtController = new PassiveDHTNodeController();
        } 
        
        dhtController.start();
    }
    
    public void switchMode(boolean toActiveMode) {
        if(dhtController == null || !dhtController.isRunning()) {
            return;
        }
        
        boolean wasActive = dhtController.isActiveNode();
        if((toActiveMode && wasActive) || (!toActiveMode && !wasActive)) {
            return; //no change
        }
        
        startDHT(toActiveMode);
    }

    public synchronized void addBootstrapHost(SocketAddress hostAddress) {
        dhtController.addBootstrapHost(hostAddress);
    }
    
    public void addressChanged() {
        if(dhtController == null || !dhtController.isRunning()) {
            return;
        }
        
        boolean wasRunning = dhtController.isRunning();
        
        dhtController.shutdown();
        
        if(wasRunning) {
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
    
    public void shutdown(){
        if(dhtController != null) {
            dhtController.shutdown();
        }
    }
    
    public boolean isRunning() {
        if(dhtController != null) {
            return dhtController.isRunning();
        }
        return false;
    }
    
    public int getDHTVersion() {
        if(dhtController != null) {
            return dhtController.getDHTVersion();
        }
        return -1;
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

}
