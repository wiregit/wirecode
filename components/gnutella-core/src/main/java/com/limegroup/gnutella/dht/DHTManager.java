package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.LifecycleListener;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;

/**
 * 
 */
public interface DHTManager extends LifecycleListener {
    
    /**
     * Various modes a DHT Node may have
     */
    public static enum DHTMode {
        NONE((byte)0x00),
        ACTIVE((byte)0x01),
        PASSIVE((byte)0x02);
        
        /**
         * 
         */
        public static final byte DHT_MODE_MASK = 0x0F;
        
        private byte mode;
        
        private DHTMode(byte mode){
            this.mode = mode;
        }
        
        public byte getByte() {
            return mode;
        }
        
        public boolean isActive() {
            return ((mode & DHT_MODE_MASK) == ACTIVE.getByte());
        }
        
        public boolean isPassive() {
            return ((mode & DHT_MODE_MASK) == PASSIVE.getByte());
        }
        
        public boolean isNone() {
            return ((mode & DHT_MODE_MASK) == NONE.getByte());
        }
        
    }
    
    /**
     * Starts the DHT Node either in active or passive mode.
     */
    public void start(boolean activeMode);

    /**
     * Stops the DHT Node
     */
    public void stop();
    
    /**
     * 
     */
    public void addActiveDHTNode(SocketAddress hostAddress);
    
    /**
     * 
     */
    public void addPassiveDHTNode(SocketAddress hostAddress);
    
    /**
     * Called whenever our external Address has changed
     */
    public void addressChanged();

    /**
     * Returns maxNodes number of active Node's IP:Ports
     */
    public List<IpPort> getActiveDHTNodes(int maxNodes);

    /**
     * Returns whether this Node is an active Node or not
     */
    public boolean isActiveNode();
    
    /**
     * Returns whether this Node is running
     */
    public boolean isRunning();
    
    /**
     * Returns whether this Node is bootstrapped
     */
    public boolean isBootstrapped();
    
    /**
     * Returns whether this Node is waiting for Nodes or not
     */
    public boolean isWaitingForNodes();
    
    /**
     * Returns the MojitoDHT instance
     */
    public MojitoDHT getMojitoDHT();
    
    /**
     * Returns the Vendor code of this Node
     */
    public int getVendor();
    
    /**
     * Returns the Vendor code of this Node
     */
    public int getVersion();
}