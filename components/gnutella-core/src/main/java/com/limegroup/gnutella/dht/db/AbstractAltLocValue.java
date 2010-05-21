package com.limegroup.gnutella.dht.db;

import java.util.Arrays;

import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueImpl;
import org.limewire.mojito2.util.ArrayUtils;
import org.limewire.util.ByteUtils;

public abstract class AbstractAltLocValue implements AltLocValue {

    static final String CLIENT_ID = "client-id";
    
    static final String PORT = "port";
    
    static final String FIREWALLED = "firewalled";
    
    static final String LENGTH = "length";
    
    static final String TTROOT = "ttroot";
    
    static final String TLS = "tls";
    
    protected final Version version;
    
    public AbstractAltLocValue(Version version) {
        this.version = version;
    }

    @Override
    public Version getVersion() {
        return version;
    }
    
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof AltLocValue)) {
            return false;
        }
        
        AltLocValue other = (AltLocValue)o;
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
        
        return buffer.toString();
    }
}
