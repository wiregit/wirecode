package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;

public interface DHTController {
    
    public void init();
    
    public void start();
    
    public void stop();

    public List<IpPort> getActiveDHTNodes(int maxNodes);
    
    public void addActiveDHTNode(SocketAddress hostAddress);
    
    public void addPassiveDHTNode(SocketAddress hostAddress);
    
    public void sendUpdatedCapabilities();
    
    public boolean isActiveNode();
    
    public boolean isRunning();
    
    public boolean isWaitingForNodes();
    
    public int getDHTVersion();
    
    //TODO: remove! for testing only 
    MojitoDHT getMojitoDHT();
    
    public void handleConnectionLifecycleEvent(LifecycleEvent evt);
    
}
