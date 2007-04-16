package com.limegroup.gnutella.dht.db;

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
 *   <proxies>
 *     <proxy-count>5</proxy-count>
 *     <proxy-0>
 *       <ip-address>1.2.3.4</ip-address>
 *       <port>6000</port>
 *     </proxy-0>
 *     <proxy-1>
 *       <ip-address>4.3.2.1</ip-address>
 *       <port>7000</port>
 *     </proxy-1>
 *   </proxies>
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
    
    private static final String IP_ADDRESS = "ip-address";
    
    private static final String PORT = "port";
    
    private static final String PROXIES = "proxies";
    
    private static final String PROXY_COUNT = "proxy-count";
    
    private static final String PROXY = "proxy-"; // proxy-0, proxy-99, ...
    
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
            this.port = getPortFromGGEP(ggep);
            
            GGEP proxiesGGEP = new GGEP(ggep.get(PROXIES), 0);
            if (proxiesGGEP == null) {
                throw new DHTValueException("No such element: " + PROXIES);
            }
            
            int count = proxiesGGEP.getInt(PROXY_COUNT);
            
            Set<IpPort> proxies = new IpPortSet();
            for (int i = 0; i < count; i++) {
                String proxyKey = PROXY + i;
                
                byte[] element = proxiesGGEP.get(proxyKey);
                if (element == null) {
                    throw new DHTValueException("No such element: " + proxyKey);
                }
                
                GGEP proxyGGEP = new GGEP(element, 0);
                
                byte[] proxyAddr = proxyGGEP.get(IP_ADDRESS);
                if (proxyAddr == null) {
                    throw new DHTValueException("No such element: " + IP_ADDRESS);
                }
                int proxyPort = getPortFromGGEP(proxyGGEP);
                
                proxies.add(new IpPortImpl(InetAddress.getByAddress(proxyAddr), proxyPort));
            }
            
            this.proxies = proxies;
            
        } catch (BadGGEPPropertyException err) {
            throw new DHTValueException(err);
            
        } catch (BadGGEPBlockException err) {
            throw new DHTValueException(err);
            
        } catch (UnknownHostException err) {
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
        
        GGEP proxiesGGEP = new GGEP();
        Set<? extends IpPort> proxies = value.getPushProxies();
        proxiesGGEP.put(PROXY_COUNT, proxies.size());
        
        int index = 0;
        for (IpPort proxy : proxies) {
            GGEP proxyGGEP = new GGEP();
            
            byte[] proxyAddr = proxy.getInetAddress().getAddress();
            proxyGGEP.put(IP_ADDRESS, proxyAddr);
            
            byte[] proxyPort = new byte[2];
            ByteOrder.short2beb((short)proxy.getPort(), proxyPort, 0);
            proxyGGEP.put(PORT, proxyPort);
            
            String proxyKey = PROXY + Integer.toString(index++);
            proxiesGGEP.put(proxyKey, proxyGGEP.toByteArray());
        }
        
        ggep.put(PROXIES, proxiesGGEP.toByteArray());
        return ggep.toByteArray();
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
    }
}
