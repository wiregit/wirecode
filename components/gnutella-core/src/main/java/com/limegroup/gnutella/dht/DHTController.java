package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;

/**
 * 
 */
public interface DHTController {
    
    /**
     * Starts the DHT Node
     */
    public void start();
    
    /**
     * Stops the DHT Node
     */
    public void stop();

    /**
     * Returns maxNodes number of active Node IP:Ports
     */
    public List<IpPort> getActiveDHTNodes(int maxNodes);
    
    /**
     * 
     */
    public void addActiveDHTNode(SocketAddress hostAddress);
    
    /**
     * 
     */
    public void addPassiveDHTNode(SocketAddress hostAddress);
    
    /**
     * Sends the updated capabilities to our connections (Gnutella)
     */
    public void sendUpdatedCapabilities();
    
    /**
     * Returns whether this Node is an active Node or not
     */
    public boolean isActiveNode();
    
    /**
     * Returns whether this Node is running or not
     */
    public boolean isRunning();
    
    /**
     * Returns whether this Node is waiting for Nodes or not
     */
    public boolean isWaitingForNodes();
    
    //TODO: remove! for testing only 
    MojitoDHT getMojitoDHT();
    
    /**
     * A callback method to notify the DHTController about 
     * ConnectionLifecycleEvents
     */
    public void handleConnectionLifecycleEvent(LifecycleEvent evt);
}
