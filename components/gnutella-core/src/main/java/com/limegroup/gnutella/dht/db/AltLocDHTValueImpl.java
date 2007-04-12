package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;

/**
 * An implementation of AltLocDHTValue
 */
public class AltLocDHTValueImpl implements AltLocDHTValue {
    
    private static final long serialVersionUID = 8302182739922310121L;
    
    /**
     * An AltLocDHTValue for the localhost
     */
    public static final DHTValue SELF = new AltLocForSelf();
    
    private final Version version;
    
    private final byte[] guid;
    
    private final int port;
    
    private final boolean firewalled;
    
    /**
     * Creates an AltLocDHTValue from the given data
     */
    public static DHTValue createFromData(Version version, byte[] data) 
            throws DHTValueException {
        return new AltLocDHTValueImpl(version, data);
    }
    
    /**
     * Constructor to create an AltLocDHTValue for the localhost
     */
    private AltLocDHTValueImpl() {
        this.version = VERSION;
        this.guid = null;
        this.port = -1;
        this.firewalled = true;
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
        
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        try {
            this.guid = new byte[16];
            in.readFully(guid);
            this.port = in.readUnsignedShort();
            this.firewalled = in.readBoolean();
        } catch (IOException err) {
            throw new DHTValueException(err);
        } finally {
            IOUtils.close(in);
        }
        
        if (!NetworkUtils.isValidPort(port)) {
            throw new DHTValueException("Illegal port: " + port);
        }
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
        return value();
    }

    public void write(OutputStream out) throws IOException {
        out.write(getValue());
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
    
    private byte[] value() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        try {
            out.write(getGUID());
            out.writeShort(getPort());
            out.writeBoolean(isFirewalled());
        } catch (IOException err) {
            // Impossible
            throw new RuntimeException(err);
        } finally {
            IOUtils.close(out);
        }
        return baos.toByteArray();
    }
    
    /**
     * An AltLocDHTValue for the localhost
     */
    private static class AltLocForSelf extends AltLocDHTValueImpl {
        
        private static final long serialVersionUID = 8101291047246461600L;
        
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
        public boolean isAltLocForSelf() {
            return true;
        }
    }
}
