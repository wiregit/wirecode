package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.LifecycleListener;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.util.IpPort;

/**
 * The DHT Manager interface currently defines method to start, stop and perform
 * operations related to the maintenance of the DHT (bootstrapping, etc.).
 * It also takes care of switching an active DHT node to a passive DHT node 
 * and vice versa.
 * 
 * TODO: The manager will later expose the methods to use the DHT, i.e. store 
 * and retrieve values.
 */
public interface DHTManager extends LifecycleListener {
    
    /**
     * Various modes a DHT Node may have
     */
    public static enum DHTMode {
        
        /**
         * A DHT Node is in INACTIVE mode if it supports the DHT
         * but is currently not capable of joining it.
         * 
         * @see NodeAssigner.java
         */
        INACTIVE(0x00),
        
        /**
         * A DHT Node is ACTIVE mode if it's a full participant
         * of the DHT, e.g. a non-firewalled Gnutella leave node
         * with a sufficiently stable connection.
         * 
         */
        ACTIVE(0x01),
        
        /**
         * A DHT Node is in PASSIVE mode if it's connected to
         * the DHT but is not part of the global DHT routing table. 
         * Thus, a passive node never receives requests from the DHT 
         * and does necessarily have an accurate knowledge of the DHT
         * structure. However, it can perform queries and requests stores.
         * 
         */
        PASSIVE(0x02);
        
        public static final byte DHT_MODE_MASK = 0x0F;
        
        private byte mode;
        
        private DHTMode(int state) {
            this.mode = (byte)(state & 0xFF);
        }
        
        /**
         * Returns the mode as byte
         */
        public byte toByte() {
            return mode;
        }
        
        private static final DHTMode[] MODES;
        
        static {
            DHTMode[] modes = values();
            MODES = new DHTMode[modes.length];
            for (DHTMode m : modes) {
                int index = (m.mode & 0xFF) % MODES.length;
                assert (MODES[index] == null);
                MODES[index] = m;
            }
        }
        
        /**
         * Returns a DHTMode enum for the given mode and null
         * if no such DHTMode exists.
         */
        public static DHTMode valueOf(byte mode) {
            int index = (mode & 0xFF) % MODES.length;
            DHTMode s = MODES[index];
            if (s.mode == mode) {
                return s;
            }
            return null;
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
     * Passes the given active DHT node to the DHT controller 
     * in order to bootstrap or perform other maintenance operations. 
     */
    public void addActiveDHTNode(SocketAddress hostAddress);
    
    /**
     * Passes the given passive DHT node to the DHT controller 
     * in order to bootstrap or perform other maintenance operations. 
     */
    public void addPassiveDHTNode(SocketAddress hostAddress);
    
    /**
     * Notifies the DHT controller that our external Address has changed
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
     * Returns the Vendor code of this Node
     */
    public int getVendor();
    
    /**
     * Returns the Vendor code of this Node
     */
    public int getVersion();
}
