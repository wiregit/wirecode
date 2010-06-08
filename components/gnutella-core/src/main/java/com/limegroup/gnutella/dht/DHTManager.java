package com.limegroup.gnutella.dht;

import java.io.Closeable;
import java.net.SocketAddress;

import org.limewire.io.IpPort;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.ValueKey;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.Value;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.ContextSettings;

import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.RouteTable;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.util.EventDispatcher;

/**
 * The {@link DHTManager} is managing the various types of modes
 * in which nodes may connect to the DHT.
 */
public interface DHTManager extends Closeable, ConnectionLifecycleListener, 
        EventDispatcher<DHTEvent, DHTEventListener> {

    public static final Vendor VENDOR = ContextSettings.getVendor();

    public static final Version VERSION = ContextSettings.getVersion();

    /**
     * Defines the modes of a DHT Node (inactive, active, passive and passive leaf).
     */
    public static enum DHTMode {
        
        /**
         * A DHT Node is in #INACTIVE mode if it supports the DHT
         * but is currently not capable of joining it.
         * 
         * @see NodeAssigner
         */
        INACTIVE(0x00, new byte[]{ 'I', 'D', 'H', 'T' }),
        
        /**
         * A DHT Node is #ACTIVE mode if it's a full participant
         * of the DHT, e.g. a non-firewalled Gnutella leave node
         * with a sufficiently stable connection.
         */
        ACTIVE(0x01, new byte[]{ 'A', 'D', 'H', 'T' }),
        
        /**
         * A DHT Node is in #PASSIVE mode if it's connected to
         * the DHT but is not part of the global DHT routing table. 
         * Thus, a passive node never receives requests from the DHT 
         * and does necessarily have an accurate knowledge of the DHT
         * structure. However, it can perform queries and requests stores.
         */
        PASSIVE(0x02, new byte[]{ 'P', 'D', 'H', 'T' }),
        
        /**
         * The #PASSIVE_LEAF mode is very similar to #PASSIVE mode with
         * two major differences:
         * <pre>
         * 1) A passive leaf has a fixed size LRU Map as its RouteTable.
         *    That means it has almost no knowledge of the global DHT
         *    RouteTable and depends entirely on an another peer (Ultrapeer)
         *    that feeds it continuously with fresh contacts.
         * 
         * 2) A passive leaf node does not perform any Kademlia maintenance
         *    operations!</pre>
         */
        PASSIVE_LEAF(0x03, new byte[]{ 'L', 'D', 'H', 'T' });
        
        public static final byte DHT_MODE_MASK = 0x0F;
        
        private final int mode;
        
        private final byte[] capabilityName;
        
        private DHTMode(int mode, byte[] capabilityName) {
            assert (capabilityName.length == 4);
            this.mode = mode;
            this.capabilityName = capabilityName;
        }
        
        /**
         * Returns the mode as byte.
         */
        public byte byteValue() {
            return (byte)(mode & 0xFF);
        }
        
        /**
         * Returns the VM capability name.
         */
        public byte[] getCapabilityName() {
            return capabilityName.clone();
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
        public static DHTMode valueOf(int mode) {
            int index = (mode & 0xFF) % MODES.length;
            DHTMode s = MODES[index];
            if (s.mode == mode) {
                return s;
            }
            return null;
        }
    }
    
    /**
     * Returns the {@link Vendor}.
     */
    public Vendor getVendor();
    
    /**
     * Returns the {@link Version}.
     */
    public Version getVersion();
    
    /**
     * Starts the DHT in the given {@link DHTMode}.
     */
    public boolean start(DHTMode mode);

    /**
     * Stops the DHT.
     */
    public void stop();

    /**
     * Returns the {@link Controller}.
     */
    public Controller getController();

    /**
     * Returns {@code true} if the DHT is running.
     */
    public boolean isRunning();

    /**
     * Sets whether or not the DHT is enabled.
     */
    public void setEnabled(boolean enabled);

    /**
     * Returns {@code true} if the DHT is enabled.
     */
    public boolean isEnabled();

    /**
     * Returns the current {@link DHTMode}.
     */
    public DHTMode getMode();

    /**
     * Returns true if the DHT is running in the given {@link DHTMode}.
     */
    public boolean isMode(DHTMode mode);

    /**
     * A callback method that's being called by Gnutella to indicate
     * that the host's address has changed.
     */
    public void addressChanged();

    /**
     * Returns {@code true} if the DHT is currently booting.
     */
    public boolean isBooting();
    
    /**
     * Returns {@code true} if the DHT is ready.
     */
    public boolean isReady();

    /**
     * Returns up to the given number of {@link Contact}s 
     * from the {@link RouteTable}.
     */
    public Contact[] getActiveContacts(int max);

    /**
     * Returns up to the given number of {@link IpPort}s 
     * from the {@link RouteTable}.
     */
    public IpPort[] getActiveIpPort(int max);

    /**
     * A callback method for Gnutella.
     */
    public void handleContactsMessage(DHTContactsMessage msg);

    /**
     * Stores the given key-value pair in the DHT
     */
    public DHTFuture<StoreEntity> put(KUID key, Value value);

    /**
     * Stores the given key-value pair in the DHT
     */
    public DHTFuture<StoreEntity> enqueue(KUID key, Value value);
    
    /**
     * Retrieves a value from the DHT.
     */
    public DHTFuture<ValueEntity> get(ValueKey key);

    /**
     * Retrieves a value from the DHT.
     */
    public DHTFuture<ValueEntity[]> getAll(ValueKey key);
    
    /**
     * Adds an ACTIVE node's {@link SocketAddress}.
     */
    public void addActiveNode(SocketAddress address);

    /**
     * Adds a PASSIVE node's {@link SocketAddress}.
     */
    public void addPassiveNode(SocketAddress address);

    /**
     * Adds an {@link DHTEventListener}.
     */
    public void addEventListener(DHTEventListener listener);

    /**
     * Removes a {@link DHTEventListener}.
     */
    public void removeEventListener(DHTEventListener listener);
    
    /**
     * Equivalent to {@link #getController()#getMojitoDHT()}
     */
    public MojitoDHT getMojitoDHT();
}