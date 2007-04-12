package com.limegroup.gnutella.dht.db;

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
import com.limegroup.gnutella.RouterService;

/**
 * An implementation of PushProxiesDHTValue
 */
public class PushProxiesDHTValueImpl implements PushProxiesDHTValue {
    
    private static final long serialVersionUID = -8565050579104508260L;
    
    /**
     * The Push Proxy value for the localhost
     */
    public static final DHTValue FOR_SELF = new PushProxiesForSelf();
    
    /**
     * The version of the value
     */
    private final Version version;
    
    /**
     * Gnutella features bit-field
     */
    private final int features;
    
    /**
     * Gnutella Firewall-2-Firewall Transfer Protocol version
     */
    private final int fwtVersion;
    
    /**
     * The port number which may differ from the Contact addresse's
     * port number
     */
    private final int port;
    
    /**
     * A Set of PushProxy IpPorts
     */
    private final Set<? extends IpPort> proxies;
    
    /**
     * Factory method to create PushProxiesDHTValues
     */
    public static DHTValue createFromData(Version version, byte[] data) 
            throws DHTValueException {
        return new PushProxiesDHTValueImpl(version, data);
    }
    
    /**
     * Constructor to create a PushProxiesDHTValue for the localhost
     */
    private PushProxiesDHTValueImpl() {
        this(VERSION, 0, 0, -1, null);
    }
    
    /**
     * Constructor for testing purposes
     */
    PushProxiesDHTValueImpl(Version version, int features, 
            int fwtVersion, int port, Set<? extends IpPort> proxies) {
        this.version = version;
        this.features = features;
        this.fwtVersion = fwtVersion;
        this.port = port;
        this.proxies = proxies;
    }
    
    /**
     * Constructor to create PushProxiesDHTValues that are read from the Network
     */
    private PushProxiesDHTValueImpl(Version version, byte[] data) 
            throws DHTValueException {
        
        if (version == null) {
            throw new DHTValueException("Version is null");
        }
        
        if (data == null) {
            throw new DHTValueException("Data is null");
        }
        
        this.version = version;
        
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        
        try {
            this.features = in.readInt();
            this.fwtVersion = in.readInt();
            this.port = in.readUnsignedShort();
            
            if (!NetworkUtils.isValidPort(port)) {
                throw new DHTValueException("Illegal port: " + port);
            }
            
            Set<IpPort> proxies = new IpPortSet();
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                byte[] proxy = new byte[in.readUnsignedByte()];
                in.readFully(proxy);
                int proxyPort = in.readUnsignedShort();
                
                proxies.add(new IpPortImpl(InetAddress.getByAddress(proxy), proxyPort));
            }
            
            this.proxies = proxies;
            
        } catch (IOException err) {
            throw new DHTValueException(err);
        } finally {
            IOUtils.close(in);
        }
    }
    
    public int getFeatures() {
        return features;
    }

    public int getFwtVersion() {
        return fwtVersion;
    }

    public int getPort() {
        return port;
    }
    
    public Set<? extends IpPort> getPushProxies() {
        return proxies;
    }
    
    public byte[] getValue() {
        return value();
    }

    public void write(OutputStream out) throws IOException {
        out.write(getValue());
    }
    
    public DHTValueType getValueType() {
        return PUSH_PROXIES;
    }
    
    public Version getVersion() {
        return version;
    }
    
    public boolean isEmpty() {
        return false;
    }
    
    public boolean isPushProxiesForSelf() {
        return false;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("PushProxies: ").append(getPushProxies());
        
        if (isPushProxiesForSelf()) {
            buffer.append(", local=true");
        }
        
        return buffer.toString();
    }
    
    private byte[] value() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        try {
            out.writeInt(getFeatures());
            out.writeInt(getFwtVersion());
            
            out.writeShort(getPort());
            
            Set<? extends IpPort> proxies = getPushProxies();
            out.writeInt(proxies.size());
            for (IpPort proxy : proxies) {
                byte[] proxyAddr = proxy.getInetAddress().getAddress();
                
                out.writeByte(proxyAddr.length);
                out.write(proxyAddr);
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
    
    /**
     * An implementation of PushProxiesDHTValue for the localhost
     */
    private static class PushProxiesForSelf extends PushProxiesDHTValueImpl {
        
        private static final long serialVersionUID = -3222117316287224578L;

        @Override
        public int getFeatures() {
            return PushEndpointForSelf.instance().getFeatures();
        }

        @Override
        public int getFwtVersion() {
            return PushEndpointForSelf.instance().supportsFWTVersion();
        }
        
        @Override
        public int getPort() {
            return RouterService.getPort();
        }

        @Override
        public Set<? extends IpPort> getPushProxies() {
            return PushEndpointForSelf.instance().getProxies();
        }

        @Override
        public boolean isPushProxiesForSelf() {
            return true;
        }
    }
}
