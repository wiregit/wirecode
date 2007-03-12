package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpointForSelf;
import com.limegroup.gnutella.RouterService;

/**
 *
 */
public class AltLocDHTValueImpl implements AltLocDHTValue {
    
    private static final long serialVersionUID = 8302182739922310121L;
    
    /**
     * An AltLocDHTValue for the localhost
     */
    public static final DHTValue SELF = new AltLocForSelf();
    
    private final DHTValueType valueType;
    
    private final Version version;
    
    private final byte[] guid;
    
    private final int port;
    
    private final boolean firewalled;
    
    private final InetAddress address;
    
    private final int features;
    
    private final int fwtVersion;
    
    private final int pushProxyPort;
    
    /**
     * Creates an AltLocDHTValue from the given data
     */
    public static DHTValue createFromData(DHTValueType valueType, 
            Version version, byte[] data) throws DHTValueException {
        return new AltLocDHTValueImpl(valueType, version, data);
    }
    
    /**
     * Constructor to create an AltLocDHTValue for the localhost
     */
    private AltLocDHTValueImpl() {
        this.valueType = ALT_LOC;
        this.version = VERSION;
        this.guid = null;
        this.port = -1;
        this.firewalled = true;
        this.address = null;
        this.features = 0;
        this.fwtVersion = 0;
        this.pushProxyPort = -1;
    }
    
    /**
     * Constructor to create AltLocDHTValues that are read from the Network
     */
    private AltLocDHTValueImpl(DHTValueType valueType, Version version, byte[] data) throws DHTValueException {
        if (data == null) {
            throw new DHTValueException("Data is null");
        }
        
        this.valueType = valueType;
        this.version = version;
        
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        try {
            this.guid = new byte[16];
            in.readFully(guid);
            this.port = in.readUnsignedShort();
            this.firewalled = in.readBoolean();
            if (firewalled) {
                this.features = in.readInt();
                this.fwtVersion = in.readInt();
                
                int length = in.readUnsignedByte();
                byte[] addr = new byte[length];
                in.readFully(addr);
                this.address = InetAddress.getByAddress(addr);
                
                this.pushProxyPort = in.readUnsignedShort();
            } else {
                this.features = 0;
                this.fwtVersion = 0;
                this.address = null;
                this.pushProxyPort = -1;
            }
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
    
    public int getFeatures() {
        return features;
    }
    
    public int getFwtVersion() {
        return fwtVersion;
    }
    
    public InetAddress getInetAddress() {
        return address;
    }
    
    public int getPushProxyPort() {
        return pushProxyPort;
    }
    
    public byte[] getValue() {
        return value();
    }

    public void write(OutputStream out) throws IOException {
        out.write(getValue());
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
    
    public boolean isAltLocForSelf() {
        return false;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("AltLoc: guid=").append(new GUID(getGUID()))
            .append(", port=").append(getPort())
            .append(", firewalled=").append(isFirewalled());
            
        if (isFirewalled()) {
            buffer.append(", features=").append(getFeatures())
                .append(", fwtVersion=").append(getFwtVersion())
                .append(", address=").append(getInetAddress())
                .append(", pushProxyPort=").append(getPushProxyPort());
        }
        
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
            if (isFirewalled()) {
                out.writeInt(getFeatures());
                out.writeInt(getFwtVersion());
                
                byte[] addr = getInetAddress().getAddress();
                out.writeByte(addr.length);
                out.write(addr);
                
                out.writeShort(getPushProxyPort());
            }
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
            return RouterService.acceptedIncomingConnection();
        }
        
        @Override
        public int getFeatures() {
            return PushEndpointForSelf.instance().getFeatures();
        }

        @Override
        public int getFwtVersion() {
            return PushEndpointForSelf.instance().supportsFWTVersion();
        }

        @Override
        public InetAddress getInetAddress() {
            try {
                return InetAddress.getByAddress(RouterService.getExternalAddress());
            } catch (UnknownHostException err) {
                return null;
            }
        }

        @Override
        public int getPushProxyPort() {
            return RouterService.getPort();
        }

        @Override
        public boolean isAltLocForSelf() {
            return true;
        }
    }
}
