package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.limewire.io.NetworkUtils;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * An implementation of DHTValue for for Gnutella Alternate Locations
 */
public abstract class AltLocValue implements DHTValue, Serializable {
    
    /**
     * DHTValueType for AltLocs
     */
    public static final DHTValueType ALT_LOC = DHTValueType.valueOf("Gnutella Alternate Location", "ALOC");
    
    /**
     * Version of AltLocDHTValue
     */
    public static final Version VERSION = Version.valueOf(0);
    
    /**
     * An AltLocDHTValue for the localhost
     */
    public static final AltLocValue SELF = new AltLocForSelf();
    
    private static final String CLIENT_ID = "client-id";
    
    private static final String PORT = "port";
    
    private static final String FIREWALLED = "firewalled";
    
    protected final Version version;
    
    /**
     * Factory method to create AltLocValue
     */
    public static AltLocValue createFromData(Version version, byte[] data) throws DHTValueException {
        return new AltLocValueImpl(version, data);
    }
    
    /**
     * Factory method for testing purposes
     */
    static AltLocValue createAltLocValue(Version version, byte[] guid, int port, boolean firewalled) {
        return new AltLocValueImpl(version, guid, port, firewalled);
    }
    
    public AltLocValue(Version version) {
        this.version = version;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getVersion()
     */
    public Version getVersion() {
        return version;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getValueType()
     */
    public DHTValueType getValueType() {
        return ALT_LOC;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#isEmpty()
     */
    public boolean isEmpty() {
        return false;
    }
    
    /**
     * The GUID of the AltLoc
     */
    public abstract byte[] getGUID();
    
    /**
     * The (Gnutella) Port of the AltLoc
     */
    public abstract int getPort();
    
    /**
     * Returns true if the AltLoc is firewalled
     */
    public abstract boolean isFirewalled();
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("AltLoc: guid=").append(new GUID(getGUID()))
            .append(", port=").append(getPort())
            .append(", firewalled=").append(isFirewalled());
        
        if (this instanceof AltLocForSelf) {
            buffer.append(", local=true");
        }
        
        return buffer.toString();
    }
    
    /**
     * A helper method to serialize AltLocValues
     */
    protected static byte[] serialize(AltLocValue value) {
        GGEP ggep = new GGEP();
        
        ggep.put(CLIENT_ID, value.getGUID());
        
        byte[] port = new byte[2];
        ByteOrder.short2beb((short)value.getPort(), port, 0);
        ggep.put(PORT, port);
        
        byte[] firewalled = { (byte)(value.isFirewalled() ? 1 : 0) };
        ggep.put(FIREWALLED, firewalled);
        
        return ggep.toByteArray();
    }
    
    /**
     * 
     */
    private static class AltLocValueImpl extends AltLocValue {
        
        private static final long serialVersionUID = -6975718782217170657L;

        private final byte[] guid;
        
        private final int port;
        
        private final boolean firewalled;
        
        private final byte[] data;
        
        /**
         * Constructor for testing purposes
         */
        private AltLocValueImpl(Version version, byte[] guid, int port, boolean firewalled) {
            super(version);
            this.guid = guid;
            this.port = port;
            this.firewalled = firewalled;
            this.data = serialize(this);
        }
        
        public AltLocValueImpl(Version version, byte[] data) throws DHTValueException {
            super(version);
            
            if (version == null) {
                throw new DHTValueException("Version is null");
            }
            
            if (data == null) {
                throw new DHTValueException("Data is null");
            }
            
            this.data = data;
            
            try {
                GGEP ggep = new GGEP(data, 0);
                
                this.guid = ggep.getBytes(CLIENT_ID);
                if (guid.length != 16) {
                    throw new DHTValueException("Illegal GUID length: " + guid.length);
                }
                
                byte[] portBytes = ggep.getBytes(PORT);
                this.port = ByteOrder.beb2short(portBytes, 0) & 0xFFFF;
                if (!NetworkUtils.isValidPort(port)) {
                    throw new DHTValueException("Illegal port: " + port);
                }
                
                byte[] firewalled = ggep.getBytes(FIREWALLED);
                if (firewalled.length != 1) {
                    throw new DHTValueException("Illegal Firewalled length: " + firewalled.length);
                }
                
                this.firewalled = (firewalled[0] != 0);
                
            } catch (BadGGEPPropertyException err) {
                throw new DHTValueException(err);
                
            } catch (BadGGEPBlockException err) {
                throw new DHTValueException(err);
            }
        }
        
        /*
         * (non-Javadoc)
         * @see com.limegroup.gnutella.dht.db.AltLocValue#getPort()
         */
        public int getPort() {
            return port;
        }
        
        /*
         * (non-Javadoc)
         * @see com.limegroup.gnutella.dht.db.AltLocValue#getGUID()
         */
        public byte[] getGUID() {
            return guid;
        }
        
        /*
         * (non-Javadoc)
         * @see com.limegroup.gnutella.dht.db.AltLocValue#isFirewalled()
         */
        public boolean isFirewalled() {
            return firewalled;
        }
        
        /*
         * (non-Javadoc)
         * @see org.limewire.mojito.db.DHTValue#getValue()
         */
        public byte[] getValue() {
            byte[] copy = new byte[data.length];
            System.arraycopy(data, 0, copy, 0, data.length);
            return copy;
        }

        /*
         * (non-Javadoc)
         * @see org.limewire.mojito.db.DHTValue#write(java.io.OutputStream)
         */
        public void write(OutputStream out) throws IOException {
            out.write(data);
        }
    }
    
    /**
     * An AltLocValue for the localhost
     */
    private static class AltLocForSelf extends AltLocValue {
        
        private static final long serialVersionUID = 8101291047246461600L;
        
        public AltLocForSelf() {
            super(VERSION);
        }

        /*
         * (non-Javadoc)
         * @see org.limewire.mojito.db.DHTValue#getValue()
         */
        public byte[] getValue() {
            return serialize(this);
        }
        
        public void write(OutputStream out) throws IOException {
            out.write(getValue());
        }

        public int getPort() {
            return RouterService.getPort();
        }
        
        public byte[] getGUID() {
            return RouterService.getMyGUID();
        }
        
        public boolean isFirewalled() {
            return !RouterService.acceptedIncomingConnection();
        }
    }
}
