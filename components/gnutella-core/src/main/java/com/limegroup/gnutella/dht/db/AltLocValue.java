package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.limewire.io.NetworkUtils;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.util.ArrayUtils;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.security.TigerTree;

/**
 * An implementation of DHTValue for for Gnutella Alternate Locations
 */
public abstract class AltLocValue implements DHTValue, Serializable {
    
    /**
     * DHTValueType for AltLocs
     */
    public static final DHTValueType ALT_LOC = DHTValueType.valueOf("Gnutella Alternate Location", "ALOC");
    
    /*
     * AltLocValue version history
     * 
     * Version 0:
     * GUID
     * Port
     * Firewalled
     * 
     * Version 1:
     * File Length
     * TigerTree root hash (optional)
     */
    
    /**
     * 
     */
    public static final Version VERSION_ONE = Version.valueOf(1);
    
    /**
     * Version of AltLocDHTValue
     */
    public static final Version VERSION = VERSION_ONE;
    
    private static final String CLIENT_ID = "client-id";
    
    private static final String PORT = "port";
    
    private static final String FIREWALLED = "firewalled";
    
    private static final String LENGTH = "length";
    
    private static final String TTROOT = "ttroot";
    
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
    static AltLocValue createAltLocValue(Version version, byte[] guid, int port, 
            long length, byte[] ttroot, boolean firewalled) {
        return new AltLocValueImpl(version, guid, port, length, ttroot, firewalled);
    }
    
    /**
     * 
     * @param fileSize
     * @param ttroot
     * @return
     */
    public static AltLocValue createAltLocValueForSelf(long fileSize, byte[] ttroot) {
        return new AltLocForSelf(fileSize, ttroot);
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
     * @see org.limewire.mojito.db.DHTValue#size()
     */
    public int size() {
        return getValue().length;
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
     * The length of the file
     */
    public abstract long getFileSize();
    
    /**
     * The TigerTree root hash
     */
    public abstract byte[] getRootHash();
    
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
            .append(", firewalled=").append(isFirewalled())
            .append(", fileSize=").append(getFileSize())
            .append(", ttroot=").append(getRootHash() != null 
                    ? ArrayUtils.toHexString(getRootHash()) : "null");
        
        if (this instanceof AltLocForSelf) {
            buffer.append(", local=true");
        }
        
        return buffer.toString();
    }
    
    /**
     * A helper method to serialize AltLocValues
     */
    protected static byte[] serialize(AltLocValue value) {
        Version version = value.getVersion();
        
        GGEP ggep = new GGEP();
        
        ggep.put(CLIENT_ID, value.getGUID());
        
        byte[] port = new byte[2];
        ByteOrder.short2beb((short)value.getPort(), port, 0);
        ggep.put(PORT, port);
        
        byte[] firewalled = { (byte)(value.isFirewalled() ? 1 : 0) };
        ggep.put(FIREWALLED, firewalled);
        
        if (version.compareTo(VERSION_ONE) >= 0) {
            ggep.put(LENGTH, /* long */ value.getFileSize());
            
            byte[] ttroot = value.getRootHash();
            if (ttroot != null) {
                ggep.put(TTROOT, ttroot);
            }
        }
        
        return ggep.toByteArray();
    }
    
    /**
     * 
     */
    private static class AltLocValueImpl extends AltLocValue {
        
        private static final long serialVersionUID = -6975718782217170657L;

        private final byte[] guid;
        
        private final int port;
        
        private final long fileSize;
        
        private final byte[] ttroot;
        
        private final boolean firewalled;
        
        private final byte[] data;
        
        /**
         * Constructor for testing purposes
         */
        private AltLocValueImpl(Version version, byte[] guid, int port, 
                long fileSize, byte[] ttroot, boolean firewalled) {
            super(version);
            
            if (guid == null || guid.length != 16) {
                throw new IllegalArgumentException("Illegal GUID");
            }
            
            if (!NetworkUtils.isValidPort(port)) {
                throw new IllegalArgumentException("Illegal port: " + port);
            }
            
            if (version.compareTo(VERSION_ONE) >= 0) {
                if (fileSize < 0L) {
                    throw new IllegalArgumentException("Illegal fileSize: " + fileSize);
                }
                
                if (ttroot != null && ttroot.length != TigerTree.HASHSIZE) {
                    throw new IllegalArgumentException("Illegal ttroot length: " + ttroot.length);
                }
            }
            
            this.guid = guid;
            this.port = port;
            this.fileSize = fileSize;
            this.ttroot = ttroot;
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
                
                if (version.compareTo(VERSION_ONE) >= 0) {
                    
                    this.fileSize = ggep.getLong(LENGTH);
                    
                    if (ggep.hasKey(TTROOT)) {
                        byte[] ttroot = ggep.getBytes(TTROOT);
                        if (ttroot.length != TigerTree.HASHSIZE) {
                            throw new DHTValueException("Illegal ttroot length: " + ttroot.length);
                        }
                        this.ttroot = ttroot;
                        
                    } else {
                        this.ttroot = null;
                    }
                } else {
                    this.fileSize = -1L;
                    this.ttroot = null;
                }
                
            } catch (BadGGEPPropertyException err) {
                throw new DHTValueException(err);
                
            } catch (BadGGEPBlockException err) {
                throw new DHTValueException(err);
            }
        }
        
        @Override
        public int getPort() {
            return port;
        }
        
        @Override
        public byte[] getGUID() {
            return guid;
        }
        
        @Override
        public boolean isFirewalled() {
            return firewalled;
        }
        
        @Override
        public long getFileSize() {
            return fileSize;
        }
        
        @Override
        public byte[] getRootHash() {
            return ttroot;
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
        
        private final long fileSize;
        
        private final byte[] ttroot;
        
        public AltLocForSelf(long fileSize, byte[] ttroot) {
            super(VERSION);
            
            if (fileSize < 0L) {
                throw new IllegalArgumentException("Illegal fileSize: " + fileSize);
            }
            
            if (ttroot != null && ttroot.length != TigerTree.HASHSIZE) {
                throw new IllegalArgumentException("Illegal ttroot length: " + ttroot.length);
            }
            
            this.fileSize = fileSize;
            this.ttroot = ttroot;
        }
        
        /*
         * (non-Javadoc)
         * @see org.limewire.mojito.db.DHTValue#getValue()
         */
        public byte[] getValue() {
            return serialize(this);
        }
        
        /*
         * (non-Javadoc)
         * @see org.limewire.mojito.db.DHTValue#write(java.io.OutputStream)
         */
        public void write(OutputStream out) throws IOException {
            out.write(getValue());
        }

        @Override
        public int getPort() {
            return RouterService.getPort();
        }
        
        @Override
        public byte[] getGUID() {
            return RouterService.getMyGUID();
        }
        
        @Override
        public boolean isFirewalled() {
            return !RouterService.acceptedIncomingConnection();
        }

        @Override
        public long getFileSize() {
            return fileSize;
        }

        @Override
        public byte[] getRootHash() {
            return ttroot;
        }
    }
}
