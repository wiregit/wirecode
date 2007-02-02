package com.limegroup.gnutella.dht;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Set;

import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.PushEndpointForSelf;

/**
 * { IpPort }
 */
public class PushProxiesDHTValue implements DHTValue {
    
    private static final long serialVersionUID = -8565050579104508260L;
    
    @Deprecated
    public static final DHTValue MY_PUSH_PROXIES = new LocalDHTValue();
    
    public static final DHTValueType PUSH_PROXIES = DHTValueType.valueOf("PROX");
    
    public static final Version VERSION = Version.valueOf(0, 0);
    
    private final DHTValueType valueType;
    
    private final Version version;
    
    private final Set<? extends IpPort> proxies;
    
    private PushProxiesDHTValue() {
        this.valueType = PUSH_PROXIES;
        this.version = VERSION;
        this.proxies = null;
    }
    
    public PushProxiesDHTValue(Set<? extends IpPort> proxies) {
        this.valueType = PUSH_PROXIES;
        this.version = VERSION;
        this.proxies = proxies;
    }
    
    PushProxiesDHTValue(DHTValueType valueType, Version version, byte[] data) throws DHTValueException {
        this.valueType = valueType;
        this.version = version;
        
        Set<IpPort> proxies = new IpPortSet();
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        
        try {
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                int length = in.readUnsignedByte();
                byte[] addr = new byte[length-2];
                in.readFully(addr);
                int port = in.readUnsignedShort();
                
                proxies.add(new IpPortImpl(InetAddress.getByAddress(addr), port));
            }
        } catch (IOException err) {
            throw new DHTValueException(err);
        } finally {
            IOUtils.close(in);
        }
        
        this.proxies = proxies;
    }
    
    public Set<? extends IpPort> getProxies() {
        return proxies;
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
        buffer.append("PushProxies: ").append(getProxies());
        
        if (isLocal()) {
            buffer.append(", local=true");
        }
        
        return buffer.toString();
    }
    
    private byte[] value() {
        Set<? extends IpPort> proxies = getProxies();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        try {
            out.writeInt(proxies.size());
            for (IpPort proxy : proxies) {
                byte[] addr = NetworkUtils.getBytes(
                        proxy.getInetAddress(), proxy.getPort());
                
                out.writeByte(addr.length);
                out.write(addr, 0, addr.length);
            }
        } catch (IOException err) {
            // Impossible
            throw new RuntimeException(err);
        } finally {
            IOUtils.close(out);
        }
        
        return baos.toByteArray();
    }
    
    // TODO leaving as the code might be handy
    @Deprecated
    private static class LocalDHTValue extends PushProxiesDHTValue {
        
        private static final long serialVersionUID = -3105395767814243136L;

        private LocalDHTValue() {}
        
        @Override
        public Set<? extends IpPort> getProxies() {
            return PushEndpointForSelf.instance().getProxies();
        }
        
        @Override
        protected boolean isLocal() {
            return true;
        }
    }
}
