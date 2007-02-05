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
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

public class PushProxiesDHTValueImpl implements PushProxiesDHTValue {
    
    private static final long serialVersionUID = -8565050579104508260L;
    
    private final DHTValueType valueType;
    
    private final Version version;
    
    private final Set<? extends IpPort> proxies;
    
    /**
     * 
     */
    public static DHTValue createFromData(DHTValueType valueType, 
            Version version, byte[] data) throws DHTValueException {
        return new PushProxiesDHTValueImpl(valueType, version, data);
    }
    
    /**
     * 
     */
    public static DHTValue createProxyValue(Set<? extends IpPort> proxies) {
        return new PushProxiesDHTValueImpl(proxies);
    }
    
    private PushProxiesDHTValueImpl() {
        this.valueType = PUSH_PROXIES;
        this.version = VERSION;
        this.proxies = null;
    }
    
    private PushProxiesDHTValueImpl(Set<? extends IpPort> proxies) {
        this.valueType = PUSH_PROXIES;
        this.version = VERSION;
        this.proxies = new IpPortSet(proxies);
    }
    
    private PushProxiesDHTValueImpl(DHTValueType valueType, Version version, byte[] data) 
            throws DHTValueException {
        
        this.valueType = valueType;
        this.version = version;
        
        Set<IpPort> proxies = new IpPortSet();
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        
        try {
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                int length = in.readUnsignedByte();
                byte[] addr = new byte[length];
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
    
    public Set<? extends IpPort> getPushProxies() {
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
        buffer.append("PushProxies: ").append(getPushProxies());
        
        if (isLocal()) {
            buffer.append(", local=true");
        }
        
        return buffer.toString();
    }
    
    private byte[] value() {
        Set<? extends IpPort> proxies = getPushProxies();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        try {
            out.writeInt(proxies.size());
            for (IpPort proxy : proxies) {
                byte[] addr = proxy.getInetAddress().getAddress();
                
                out.writeByte(addr.length);
                out.write(addr);
                out.writeShort(proxy.getPort());
            }
        } catch (IOException err) {
            // Impossible
            throw new RuntimeException(err);
        } finally {
            IOUtils.close(out);
        }
        
        return baos.toByteArray();
    }
}
