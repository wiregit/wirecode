package com.limegroup.gnutella.dht2;

import java.io.Closeable;
import java.net.SocketAddress;

import org.limewire.io.IpPort;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.Vendor;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.settings.ContextSettings;
import org.limewire.mojito2.storage.DHTValue;

import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

public interface DHTManager extends Closeable, ConnectionLifecycleListener {

    public static final Vendor VENDOR = ContextSettings.getVendor();

    public static final Version VERSION = ContextSettings.getVersion();

    /**
     * Defines the modes of a DHT Node (inactive, active, passive and passive leaf).
     */
    public static enum DHTMode {
        
        /**
         * A DHT Node is in INACTIVE mode if it supports the DHT
         * but is currently not capable of joining it.
         * 
         * @see NodeAssigner.java
         */
        INACTIVE(0x00, new byte[]{ 'I', 'D', 'H', 'T' }),
        
        /**
         * A DHT Node is ACTIVE mode if it's a full participant
         * of the DHT, e.g. a non-firewalled Gnutella leave node
         * with a sufficiently stable connection.
         */
        ACTIVE(0x01, new byte[]{ 'A', 'D', 'H', 'T' }),
        
        /**
         * A DHT Node is in PASSIVE mode if it's connected to
         * the DHT but is not part of the global DHT routing table. 
         * Thus, a passive node never receives requests from the DHT 
         * and does necessarily have an accurate knowledge of the DHT
         * structure. However, it can perform queries and requests stores.
         */
        PASSIVE(0x02, new byte[]{ 'P', 'D', 'H', 'T' }),
        
        /**
         * The PASSIVE_LEAF mode is very similar to PASSIVE mode with
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
    
    public boolean start(DHTMode mode);

    public void stop();

    public Controller getController();

    public boolean isRunning();

    public void setEnabled(boolean enabled);

    public boolean isEnabled();

    public DHTMode getMode();

    public boolean isMode(DHTMode mode);

    public void addressChanged();

    public boolean isReady();

    public Contact[] getActiveContacts(int max);

    public IpPort[] getActiveIpPort(int max);

    public void handleDHTContactsMessage(DHTContactsMessage msg);

    public DHTFuture<StoreEntity> put(KUID key, DHTValue value);

    public DHTFuture<ValueEntity> get(EntityKey key);

    public void addActiveDHTNode(SocketAddress address);

    public void addPassiveDHTNode(SocketAddress address);

    public void addEventListener(DHTEventListener listener);

    public void removeEventListener(DHTEventListener listener);

}