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
        INACTIVE(0x00),
        ACTIVE(0x01),
        PASSIVE(0x02);
        
        public static final byte DHT_MODE_MASK = 0x0F;
        
        private byte mode;
        
        private DHTMode(int state) {
            this.mode = (byte)(state & 0xFF);
        }
        
        public byte toByte() {
            return mode;
        }
        
        public boolean isInactive() {
            return (this == INACTIVE);
        }
        
        public boolean isActive() {
            return (this == ACTIVE);
        }
        
        public boolean isPassive() {
            return (this == PASSIVE);
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
