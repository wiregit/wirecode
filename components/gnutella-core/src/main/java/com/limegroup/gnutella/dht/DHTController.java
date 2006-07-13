package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.LifecycleListener;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;

public interface DHTController extends LifecycleListener{
    
    public void init();
    
    public void start();
    
    public void shutdown();

    public List<IpPort> getActiveDHTNodes(int maxNodes);
    
    public void addBootstrapHost(SocketAddress hostAddress);
    
    public boolean isActiveNode();
    
    public boolean isRunning();
    
    public boolean isWaiting();
    
    public int getDHTVersion();
    
    //TODO: remove! for testing only 
    public MojitoDHT getMojitoDHT();
}
