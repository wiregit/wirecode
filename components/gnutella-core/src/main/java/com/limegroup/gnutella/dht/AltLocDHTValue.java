package com.limegroup.gnutella.dht;

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
 * AltLocDHTValue Payload
 * 
 * GUID:Port
 */
public class AltLocDHTValue implements DHTValue {
    
    private static final long serialVersionUID = 8302182739922310121L;
    
    private static final int SIZE = 16 + 2;
    
    public static final DHTValue LOCAL_HOST = new LocalDHTValue();
    
    public static final DHTValueType ALT_LOC = DHTValueType.valueOf("ALOC");
    
    public static final Version VERSION = Version.valueOf(0, 0);
    
    private final DHTValueType valueType;
    
    private final Version version;
    
    private final byte[] guid;
    
    private final int port;
    
    private final boolean firewalled;
    
    private final int pushProxyPort;
    
    private AltLocDHTValue() {
        this.valueType = ALT_LOC;
        this.version = VERSION;
        this.guid = null;
        this.port = -1;
        this.firewalled = true;
        this.pushProxyPort = -1;
    }
    
    public AltLocDHTValue(GUID guid, int port) {
        this.valueType = ALT_LOC;
        this.version = VERSION;
        this.guid = guid.bytes();
        this.port = port;
        this.firewalled = true;
        this.pushProxyPort = RouterService.getPort();
    }
    
    AltLocDHTValue(DHTValueType valueType, Version version, byte[] data) throws DHTValueException {
        if (data == null || data.length != SIZE) {
            throw new DHTValueException("Unexpected length of data: " + data.length);
        }
        
        this.valueType = valueType;
        this.version = version;
        
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        try {
            this.guid = new byte[16];
            in.readFully(guid);
            this.port = in.readUnsignedShort();
            this.firewalled = in.readBoolean();
            this.pushProxyPort = in.readUnsignedShort();
        } catch (IOException err) {
            throw new DHTValueException(err);
        } finally {
            IOUtils.close(in);
        }
        
        if (!NetworkUtils.isValidPort(port)) {
            throw new DHTValueException("Illegal port: " + port);
        }
        
        if (firewalled && !NetworkUtils.isValidPort(pushProxyPort)) {
            throw new DHTValueException("Illegal push proxy port: " + pushProxyPort);
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
    
    public int getPushProxyPort() {
        return pushProxyPort;
    }
    
    public byte[] getValue() {
        return value();
    }

    public void write(OutputStream out) throws IOException {
        out.write(value());
    }

    public DHTValueType getValueType() {
        return valueType;
    }

    public Version getVersion() {
        return version;
    }

    public boolean isEmpty() {
        return false;
    }
    
    protected boolean isLocal() {
        return false;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("AltLoc: guid=").append(new GUID(getGUID()))
            .append(", port=").append(getPort())
            .append(", firewalled=").append(isFirewalled())
            .append(", pushProxyPort=").append(getPushProxyPort());
        
        if (isLocal()) {
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
            out.writeShort(getPushProxyPort());
        } catch (IOException err) {
            // Impossible
            throw new RuntimeException(err);
        } finally {
            IOUtils.close(out);
        }
        return baos.toByteArray();
    }
    
    /**
     * 
     */
    private static class LocalDHTValue extends AltLocDHTValue {
        
        private static final long serialVersionUID = 8101291047246461600L;

        private LocalDHTValue() {}
        
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
            return RouterService.acceptedIncomingConnection();
        }
        
        @Override
        public int getPushProxyPort() {
            return RouterService.getPort();
        }

        @Override
        protected boolean isLocal() {
            return true;
        }
    }
}
