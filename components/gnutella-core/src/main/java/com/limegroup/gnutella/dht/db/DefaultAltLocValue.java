package com.limegroup.gnutella.dht.db;

import java.io.IOException;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.security.MerkleTree;

/**
 * 
 */
public class DefaultAltLocValue extends AbstractAltLocValue {
    
    private final byte[] guid;
    
    private final int port;
    
    private final long fileSize;
    
    private final byte[] ttroot;
    
    private final boolean firewalled;
    
    private final boolean supportsTLS;
    
    /**
     * 
     */
    public DefaultAltLocValue(DHTValue value) throws IOException {
        this(value.getValueType(), value.getVersion(), value.getValue());
    }
    
    /**
     * 
     */
    public DefaultAltLocValue(Version version, byte[] data) throws IOException {
        this (ALT_LOC, version, data);
    }
    
    /**
     * 
     */
    private DefaultAltLocValue(DHTValueType valueType, 
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
    
    public DefaultAltLocValue(long fileSize, byte[] ttroot,
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
        
        this.guid = applicationServices.getMyGUID();
        this.port = networkManager.getPort();
        this.firewalled = !networkManager.acceptedIncomingConnection();
        this.supportsTLS = networkManager.isIncomingTLSEnabled();
    }
    
    /**
     * Constructor for testing purposes
     */
    DefaultAltLocValue(Version version, byte[] guid, int port, long fileSize, 
            byte[] ttroot, boolean firewalled, boolean supportsTLS) {
       super (version);
       
        if (guid == null || guid.length != 16) {
            throw new IllegalArgumentException("Illegal GUID");
        }
        
        if (!NetworkUtils.isValidPort(port)) {
            throw new IllegalArgumentException("Illegal port: " + port);
        }
        
        if (version.compareTo(AltLocValue.VERSION_ONE) >= 0) {
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
    public byte[] getGUID() {
        return guid;
    }

    @Override
    public int getPort() {
        return port;
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
    public boolean isFirewalled() {
        return firewalled;
    }

    @Override
    public boolean supportsTLS() {
        return supportsTLS;
    }
}
