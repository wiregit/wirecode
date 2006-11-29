package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;

/**
 * Controls this DHT node. 
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
     * If this node is not bootstrapped, passes the given hostAddress
     * on to the DHT bootstrapper. If the node is already bootstrapped, 
     * the controller randomly tries to add the node to the DHT routing table.
     * 
     * @param hostAddress The SocketAddress of the DHT host.
     */
    public void addActiveDHTNode(SocketAddress hostAddress);
    
    /**
     * If this node is not bootstrapped, the controller requests
     * active DHT nodes from the given <tt>SocketAddress</tt>.
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
     * Returns whether or not this node is bootstrapped
     */
    public boolean isBootstrapped();
    
    /**
     * Returns whether this Node is waiting for Nodes or not
     */
    public boolean isWaitingForNodes();
    
    /**
     * A callback method to notify the DHTController about 
     * ConnectionLifecycleEvents
     */
    public void handleConnectionLifecycleEvent(LifecycleEvent evt);
    
    /**
     * Returns this controller's Mojito DHT instance.
     * NOTE: This is for internal use only and should be used only
     * within the dht.impl package. The DHT should not be handled directly
     * by external classes.
     * 
     */
    public MojitoDHT getMojitoDHT();
}
