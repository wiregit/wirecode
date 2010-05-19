package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.util.Arrays;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueImpl;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.mojito2.util.ArrayUtils;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.security.MerkleTree;

public abstract class AltLocValue2 implements SerializableValue {

    /**
     * DHTValueType for AltLocs.
     */
    public static final DHTValueType ALT_LOC 
        = DHTValueType.valueOf("Gnutella Alternate Location", "ALOC");
    
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
     * Incoming TLS support (optional)
     */
    
    public static final Version VERSION_ONE = Version.valueOf(1);
    
    /**
     * Version of AltLocDHTValue.
     */
    public static final Version VERSION = VERSION_ONE;
    
    static final String CLIENT_ID = "client-id";
    
    static final String PORT = "port";
    
    static final String FIREWALLED = "firewalled";
    
    static final String LENGTH = "length";
    
    static final String TTROOT = "ttroot";
    
    static final String TLS = "tls";
    
    private final Version version;
    
    public AltLocValue2(Version version) {
        this.version = version;
    }
    
    /**
     * 
     */
    public DHTValueType getValueType() {
        return ALT_LOC;
    }
    
    /**
     * 
     */
    public Version getVersion() {
        return version;
    }
    
    /**
     * The GUID of the AltLoc.
     */
    public abstract byte[] getGUID();

    /**
     * The (Gnutella) Port of the AltLoc.
     */
    public abstract int getPort();

    /**
     * The length of the file.
     */
    public abstract long getFileSize();

    /**
     * The TigerTree root hash.
     */
    public abstract byte[] getRootHash();

    /**
     * Returns true if the AltLoc is firewalled.
     */
    public abstract boolean isFirewalled();

    /**
     * @return true if the alternative location supports TLS
     */
    public abstract boolean supportsTLS();
    
    @Override
    public DHTValue serialize() {
        GGEP ggep = new GGEP();
        
        ggep.put(CLIENT_ID, getGUID());
        
        byte[] port = new byte[2];
        ByteUtils.short2beb((short)getPort(), port, 0);
        ggep.put(PORT, port);
        
        byte[] firewalled = { (byte)(isFirewalled() ? 1 : 0) };
        ggep.put(FIREWALLED, firewalled);
        
        if (version.compareTo(VERSION_ONE) >= 0) {
            ggep.put(LENGTH, /* long */ getFileSize());
            
            byte[] ttroot = getRootHash();
            if (ttroot != null) {
                ggep.put(TTROOT, ttroot);
            }
            
            if (supportsTLS()) {
                ggep.put(TLS);
            }
        }
        
        return new DHTValueImpl(ALT_LOC, version, ggep.toByteArray());
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(getGUID());
    }
    
    @Override
    public final boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof AltLocValue2)) {
            return false;
        }
        
        AltLocValue2 other = (AltLocValue2)o;
        return Arrays.equals(getGUID(), other.getGUID())
            && getPort() == other.getPort()
            && getFileSize() == other.getFileSize()
            && Arrays.equals(getRootHash(), other.getRootHash())
            && isFirewalled() == other.isFirewalled()
            && supportsTLS() == other.supportsTLS()
            && getVersion().equals(other.getVersion());
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("AltLoc: guid=").append(new GUID(getGUID()))
            .append(", port=").append(getPort())
            .append(", firewalled=").append(isFirewalled())
            .append(", tls=").append(supportsTLS())
            .append(", fileSize=").append(getFileSize())
            .append(", ttroot=").append(getRootHash() != null 
                    ? ArrayUtils.toHexString(getRootHash()) : "null");
        
        if (this instanceof Self) {
            buffer.append(", local=true");
        }
        
        return buffer.toString();
    }
    
    /**
     * 
     */
    public static class Self extends AltLocValue2 {
        
        private final long fileSize;
        
        private final byte[] ttroot;
        
        private final NetworkManager networkManager; 
        
        private final ApplicationServices applicationServices;
        
        public Self(long fileSize, byte[] ttroot,
                NetworkManager networkManager,
                ApplicationServices applicationServices) {
            super (VERSION);
            
            if (fileSize < 0L) {
                throw new IllegalArgumentException(
                        "Illegal fileSize: " + fileSize);
            }
            
            if (ttroot != null && ttroot.length 
                    != MerkleTree.HASHSIZE) {
                throw new IllegalArgumentException(
                        "Illegal ttroot length: " + ttroot.length);
            }
            
            this.fileSize = fileSize;
            this.ttroot = ttroot;
            this.networkManager = networkManager;
            this.applicationServices = applicationServices;
        }
        
        @Override
        public Version getVersion() {
            return VERSION;
        }

        @Override
        public int getPort() {
            return networkManager.getPort();
        }
        
        @Override
        public byte[] getGUID() {
            return applicationServices.getMyGUID();
        }
        
        @Override
        public boolean isFirewalled() {
            return !networkManager.acceptedIncomingConnection();
        }

        @Override
        public long getFileSize() {
            return fileSize;
        }

        @Override
        public byte[] getRootHash() {
            return ttroot;
        }
        
        @Override
        public boolean supportsTLS() {
            return networkManager.isIncomingTLSEnabled();
        }
    }
    
    /**
     * 
     */
    public static class Impl extends AltLocValue2 {
        
        private final byte[] guid;
        
        private final int port;
        
        private final long fileSize;
        
        private final byte[] ttroot;
        
        private final boolean firewalled;
        
        private final boolean supportsTLS;
        
        /**
         * 
         */
        public Impl(DHTValue value) throws IOException {
            this(value.getValueType(), value.getVersion(), value.getValue());
        }
        
        /**
         * 
         */
        public Impl(Version version, byte[] data) throws IOException {
            this (ALT_LOC, version, data);
        }
        
        /**
         * 
         */
        private Impl(DHTValueType valueType, 
                Version version, byte[] data) throws IOException {
            super (version);
            
            if (!valueType.equals(ALT_LOC)) {
                throw new IOException();
            }
            
            try {
                GGEP ggep = new GGEP(data, 0);
                
                this.guid = ggep.getBytes(CLIENT_ID);
                if (guid.length != 16) {
                    throw new IOException("Illegal GUID length: " + guid.length);
                }
                
                byte[] portBytes = ggep.getBytes(PORT);
                this.port = ByteUtils.beb2short(portBytes, 0) & 0xFFFF;
                if (!NetworkUtils.isValidPort(port)) {
                    throw new IOException("Illegal port: " + port);
                }
                
                byte[] firewalled = ggep.getBytes(FIREWALLED);
                if (firewalled.length != 1) {
                    throw new IOException("Illegal Firewalled length: " + firewalled.length);
                }
                
                this.firewalled = (firewalled[0] != 0);
                
                this.supportsTLS = ggep.hasKey(TLS);
                
                if (version.compareTo(VERSION_ONE) >= 0) {
                    
                    this.fileSize = ggep.getLong(LENGTH);
                    
                    if (ggep.hasKey(TTROOT)) {
                        byte[] ttroot = ggep.getBytes(TTROOT);
                        if (ttroot.length != MerkleTree.HASHSIZE) {
                            throw new IOException("Illegal ttroot length: " + ttroot.length);
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
                throw new IOException("BadGGEPPropertyException", err);
                
            } catch (BadGGEPBlockException err) {
                throw new IOException("BadGGEPBlockException", err);
            }
        }
        
        /**
         * Constructor for testing purposes
         */
        Impl(Version version, byte[] guid, int port, long fileSize, 
                byte[] ttroot, boolean firewalled, boolean supportsTLS) {
            super(version);
            
            if (guid == null || guid.length != 16) {
                throw new IllegalArgumentException("Illegal GUID");
            }
            
            if (!NetworkUtils.isValidPort(port)) {
                throw new IllegalArgumentException("Illegal port: " + port);
            }
            
            if (version.compareTo(AltLocValue2.VERSION_ONE) >= 0) {
                if (fileSize < 0L) {
                    throw new IllegalArgumentException("Illegal fileSize: " + fileSize);
                }
                
                if (ttroot != null && ttroot.length != MerkleTree.HASHSIZE) {
                    throw new IllegalArgumentException("Illegal ttroot length: " + ttroot.length);
                }
            }
            
            this.guid = guid;
            this.port = port;
            this.fileSize = fileSize;
            this.ttroot = ttroot;
            this.firewalled = firewalled;
            this.supportsTLS = supportsTLS;
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
        public boolean supportsTLS() {
            return supportsTLS;
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
