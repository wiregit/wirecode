package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.io.NetworkUtils;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * An implementation of AltLocDHTValue
 */
public class AltLocDHTValueImpl implements AltLocDHTValue {
    
    private static final long serialVersionUID = 8302182739922310121L;
    
    /**
     * An AltLocDHTValue for the localhost
     */
    public static final DHTValue SELF = new AltLocForSelf();
    
    private static final String CLIENT_ID = "client-id";
    
    private static final String PORT = "port";
    
    private static final String FIREWALLED = "firewalled";
    
    private final Version version;
    
    private final byte[] guid;
    
    private final int port;
    
    private final boolean firewalled;
    
    private final byte[] data;
    
    /**
     * Creates an AltLocDHTValue from the given data
     */
    public static DHTValue createFromData(Version version, byte[] data) 
            throws DHTValueException {
        return new AltLocDHTValueImpl(version, data);
    }
    
    /**
     * Constructor for testing purposes
     */
    AltLocDHTValueImpl(Version version, byte[] guid, int port, boolean firewalled) {
        this.version = version;
        this.guid = guid;
        this.port = port;
        this.firewalled = firewalled;
        this.data = serialize(this);
    }
    
    /**
     * Constructor to create AltLocDHTValues that are read from the Network
     */
    private AltLocDHTValueImpl(Version version, byte[] data) throws DHTValueException {
        if (version == null) {
            throw new DHTValueException("Version is null");
        }
        
        if (data == null) {
            throw new DHTValueException("Data is null");
        }
        
        this.version = version;
        this.data = data;
        
        try {
            GGEP ggep = new GGEP(data, 0);
            this.guid = ggep.get(CLIENT_ID);
            
            if (guid == null) {
                throw new DHTValueException("No such element: " + PORT);
            }
            
            if (guid.length != 16) {
                throw new DHTValueException("Illegal GUID length: " + guid.length);
            }
            
            this.port = getPortFromGGEP(ggep);
            
            byte[] firewalled = ggep.get(FIREWALLED);
            if (firewalled == null) {
                throw new DHTValueException("No such element: " + FIREWALLED);
            }
            
            if (firewalled.length != 1) {
                throw new DHTValueException("Illegal Firewalled length: " + firewalled.length);
            }
            
            this.firewalled = (firewalled[0] != 0);
            
        } catch (BadGGEPBlockException err) {
            throw new DHTValueException(err);
        }
    }
    
    private static int getPortFromGGEP(GGEP ggep) throws DHTValueException {
        byte[] portBytes = ggep.get(PORT);
        if (portBytes == null) {
            throw new DHTValueException("No such element: " + PORT);
        }
        
        if (portBytes.length != 2) {
            throw new DHTValueException("Illegal number of bytes for Port: " + portBytes.length);
        }
        
        int port = ByteOrder.beb2short(portBytes, 0) & 0xFFFF;
        if (!NetworkUtils.isValidPort(port)) {
            throw new DHTValueException("Illegal port: " + port);
        }
        
        return port;
    }
    
    public int getPort() {
        return port;
    }
    
    public byte[] getGUID() {
        return guid;
    }
    
    public boolean isFirewalled() {
        return firewalled;
    }
    
    public byte[] getValue() {
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    public void write(OutputStream out) throws IOException {
        out.write(data);
    }

    public DHTValueType getValueType() {
        return ALT_LOC;
    }

    public Version getVersion() {
        return version;
    }

    public boolean isEmpty() {
        return false;
    }
    
    public boolean isAltLocForSelf() {
        return false;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("AltLoc: guid=").append(new GUID(getGUID()))
            .append(", port=").append(getPort())
            .append(", firewalled=").append(isFirewalled());
        
        if (isAltLocForSelf()) {
            buffer.append(", local=true");
        }
        
        return buffer.toString();
    }
    
    static byte[] serialize(AltLocDHTValue value) {
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
     * An AltLocDHTValue for the localhost
     */
    private static class AltLocForSelf implements AltLocDHTValue {
        
        private static final long serialVersionUID = 8101291047246461600L;
        
        public byte[] getValue() {
            return serialize(this);
        }

        public DHTValueType getValueType() {
            return ALT_LOC;
        }

        public Version getVersion() {
            return VERSION;
        }

        public boolean isEmpty() {
            return false;
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
        
        public boolean isAltLocForSelf() {
            return true;
        }
    }
}
