package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.PushEndpointForSelf;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * An implementation of PushProxiesDHTValue. The internal structure looks
 * as follows:
 * 
 * <ggep>
 *   <features>1</features>
 *   <fwt-version>1.0</fwt-version>
 *   <port>5000</port>
 *   <proxies>1.2.3.4:2000,localhost:3000</proxies>
 * </ggep>
 */
public class PushProxiesDHTValueImpl implements PushProxiesDHTValue {
    
    private static final long serialVersionUID = -8565050579104508260L;
    
    /**
     * The Push Proxy value for the localhost
     */
    public static final DHTValue FOR_SELF = new PushProxiesForSelf();
    
    private static final String FWT_VERSION = "fwt-version";
    
    private static final String FEATURES = "features";
    
    private static final String PORT = "port";
    
    private static final String PROXIES = "proxies";
    
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
     * The raw bytes of the value
     */
    private final byte[] data;
    
    /**
     * Factory method to create PushProxiesDHTValues
     */
    public static DHTValue createFromData(Version version, byte[] data) 
            throws DHTValueException {
        return new PushProxiesDHTValueImpl(version, data);
    }
    
    /**
     * Constructor for testing purposes
     */
    PushProxiesDHTValueImpl(Version version, int features, int fwtVersion, 
            int port, Set<? extends IpPort> proxies) {
        this.version = version;
        this.features = features;
        this.fwtVersion = fwtVersion;
        this.port = port;
        this.proxies = proxies;
        this.data = serialize(this);
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
        this.data = data;
        
        try {
            GGEP ggep = new GGEP(data, 0);
            
            this.features = ggep.getInt(FEATURES);
            this.fwtVersion = ggep.getInt(FWT_VERSION);
            
            byte[] portBytes = ggep.getBytes(PORT);
            this.port = ByteOrder.beb2short(portBytes, 0) & 0xFFFF;
            if (!NetworkUtils.isValidPort(port)) {
                throw new DHTValueException("Illegal port: " + port);
            }
            
            byte[] proxiesBytes = ggep.getBytes(PROXIES);
            ByteArrayInputStream bais = new ByteArrayInputStream(proxiesBytes);
            DataInputStream in = new DataInputStream(bais);
            
            Set<IpPort> proxies = new IpPortSet();
            while(in.available() > 0) {
                int length = in.readUnsignedByte();
                
                byte[] addr = new byte[length-2];
                in.readFully(addr);
                
                int port = in.readUnsignedShort();
                
                if (!NetworkUtils.isValidPort(port)) {
                    throw new DHTValueException("Illegal port: " + port);
                }
                
                proxies.add(new IpPortImpl(InetAddress.getByAddress(addr), port));
            }
            
            this.proxies = proxies;
            
        } catch (BadGGEPPropertyException err) {
            throw new DHTValueException(err);
            
        } catch (BadGGEPBlockException err) {
            throw new DHTValueException(err);
            
        } catch (UnknownHostException err) {
            throw new DHTValueException(err);
            
        } catch (IOException err) {
            throw new DHTValueException(err);
        }
    }
    
    public byte[] getValue() {
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    public DHTValueType getValueType() {
        return PUSH_PROXIES;
    }

    public Version getVersion() {
        return version;
    }

    public boolean isEmpty() {
        return (data.length == 0);
    }

    public void write(OutputStream out) throws IOException {
        out.write(data);
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

    public boolean isPushProxiesForSelf() {
        return false;
    }
    
    static byte[] serialize(PushProxiesDHTValue value) {
        GGEP ggep = new GGEP();
        ggep.put(FEATURES, value.getFeatures());
        ggep.put(FWT_VERSION, value.getFwtVersion());
        
        byte[] port = new byte[2];
        ByteOrder.short2beb((short)value.getPort(), port, 0);
        ggep.put(PORT, port);
        
        try {
            Set<? extends IpPort> proxies = value.getPushProxies();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (IpPort proxy : proxies) {
                byte[] ipp = NetworkUtils.getBytes(proxy);
                assert (ipp.length == 6 || ipp.length == 18);
                baos.write(ipp.length);
                baos.write(ipp);
            }
            baos.close();
            ggep.put(PROXIES, baos.toByteArray());
        } catch (IOException err) {
            // Impossible
            throw new RuntimeException(err);
        }
        
        return ggep.toByteArray();
    }
    
    static String toString(PushProxiesDHTValue value) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Features=").append(value.getFeatures()).append("\n");
        buffer.append("FWTVersion=").append(value.getFwtVersion()).append("\n");
        buffer.append("PushProxies=").append(value.getPushProxies()).append("\n");
        return buffer.toString();
    }
    
    public String toString() {
        return toString(this);
    }
    
    /**
     * An implementation of PushProxiesDHTValue for the localhost
     */
    private static class PushProxiesForSelf implements PushProxiesDHTValue {
        
        private static final long serialVersionUID = -3222117316287224578L;
        
        public boolean isEmpty() {
            return false;
        }

        public DHTValueType getValueType() {
            return PUSH_PROXIES;
        }

        public Version getVersion() {
            return VERSION;
        }

        public byte[] getValue() {
            return serialize(this);
        }

        public void write(OutputStream out) throws IOException {
            out.write(getValue());
        }

        public int getFeatures() {
            return PushEndpointForSelf.instance().getFeatures();
        }

        public int getFwtVersion() {
            return PushEndpointForSelf.instance().supportsFWTVersion();
        }
        
        public int getPort() {
            return RouterService.getPort();
        }

        public Set<? extends IpPort> getPushProxies() {
            return PushEndpointForSelf.instance().getProxies();
        }
        
        public boolean isPushProxiesForSelf() {
            return true;
        }
        
        public String toString() {
            return PushProxiesDHTValueImpl.toString(this);
        }
    }
}
