package com.limegroup.gnutella.dht;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.RouterService;

/**
 * AltLocDHTValue Payload
 * 
 * GUID:Port
 */
public class AltLocDHTValue implements DHTValue {
    
    private static final long serialVersionUID = 8302182739922310121L;
    
    private static final int SIZE = 16 + 2;
    
    public static final DHTValue LOCAL = new LocalDHTValue();
    
    public static final DHTValueType ALT_LOC = DHTValueType.valueOf("ALOC");
    
    public static final Version VERSION = Version.valueOf(0, 0);
    
    private final DHTValueType valueType;
    
    private final Version version;
    
    private final byte[] guid;
    
    private final int port;
    
    private AltLocDHTValue() {
        this.valueType = ALT_LOC;
        this.version = VERSION;
        this.guid = null;
        this.port = -1;
    }
    
    AltLocDHTValue(DHTValueType valueType, Version version, byte[] data) throws DHTValueException {
        if (data == null || data.length != SIZE) {
            throw new DHTValueException("Unexpected length of data: " + data.length);
        }
        
        this.valueType = valueType;
        this.version = version;
        
        this.guid = new byte[16];
        System.arraycopy(data, 0, guid, 0, guid.length);
        
        this.port = (ByteOrder.beb2short(data, 0) & 0xFFFF);
        
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
        buffer.append("AltLoc: guid=").append(getGUID())
            .append(", port=").append(getPort());
        
        if (isLocal()) {
            buffer.append(", local=true");
        }
        
        return buffer.toString();
    }
    
    private byte[] value() {
        byte[] guid = getGUID();
        int port = getPort();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        try {
            out.write(guid);
            out.writeShort(port);
        } catch (IOException err) {
            // Impossible
            throw new RuntimeException(err);
        } finally {
            IOUtils.close(out);
        }
        return baos.toByteArray();
    }
    
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
        protected boolean isLocal() {
            return true;
        }
    }
}
