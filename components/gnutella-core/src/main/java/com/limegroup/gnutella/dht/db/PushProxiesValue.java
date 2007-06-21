package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
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

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpointForSelf;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * An implementation of DHTValue for Gnutella Push Proxies
 */
public abstract class PushProxiesValue implements DHTValue, Serializable {

    /**
     * DHTValueType for Push Proxies
     */
    public static final DHTValueType PUSH_PROXIES = DHTValueType.valueOf("Gnutella Push Proxy", "PROX");
    
    /**
     * Version of PushProxiesDHTValue
     */
    public static final Version VERSION = Version.valueOf(0);
    
    /**
     * The Push Proxy value for the localhost
     */
    public static final PushProxiesValue FOR_SELF = new PushProxiesForSelf();
    
    private static final String CLIENT_ID = "client-id";
    
    private static final String FWT_VERSION = "fwt-version";
    
    private static final String FEATURES = "features";
    
    private static final String PORT = "port";
    
    private static final String PROXIES = "proxies";
    
    private final Version version;
    
    /**
     * Factory method to create PushProxiesValues
     */
    public static PushProxiesValue createFromData(Version version, byte[] data) 
            throws DHTValueException {
        return new PushProxiesValueImpl(version, data);
    }

    /**
     * Factory method for testing purposes
     */
    static PushProxiesValue createPushProxiesValue(Version version, byte[] guid, 
                byte features, int fwtVersion, 
                int port, Set<? extends IpPort> proxies) {
        return new PushProxiesValueImpl(version, guid, features, fwtVersion, port, proxies);
    }
    
    public PushProxiesValue(Version version) {
        this.version = version;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getValueType()
     */
    public DHTValueType getValueType() {
        return PUSH_PROXIES;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getVersion()
     */
    public Version getVersion() {
        return version;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#size()
     */
    public int size() {
        return getValue().length;
    }
    
    /**
     * The Client ID of the Gnutella Node
     */
    public abstract byte[] getGUID();
    
    /**
     * The Port number of the Gnutella Node
     */
    public abstract int getPort();
    
    /**
     * The supported features of the Gnutella Node
     */
    public abstract byte getFeatures();
    
    /**
     * The version of the firewalls-to-firewall 
     * transfer protocol
     */
    public abstract int getFwtVersion();
    
    /**
     * A Set of Push Proxies of the Gnutella Node
     */
    public abstract Set<? extends IpPort> getPushProxies();
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("GUID=").append(new GUID(getGUID())).append("\n");
        buffer.append("Features=").append(getFeatures()).append("\n");
        buffer.append("FWTVersion=").append(getFwtVersion()).append("\n");
        buffer.append("PushProxies=").append(getPushProxies()).append("\n");
        return buffer.toString();
    }
    
    /**
     * A helper method to serialize PushProxiesValues
     */
    protected static byte[] serialize(PushProxiesValue value) {
        GGEP ggep = new GGEP(true);
        ggep.put(CLIENT_ID, value.getGUID());
        // Preserve insertion as an int, not a byte, for backwards compatability.
        ggep.put(FEATURES, (int)value.getFeatures());
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
    
    private static class PushProxiesValueImpl extends PushProxiesValue {
        
        private static final long serialVersionUID = -2912251955825278890L;

        /**
         * The GUID of the Gnutella Node
         */
        private final byte[] guid;
        
        /**
         * Gnutella features bit-field
         */
        private final byte features;
        
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
         * Constructor for testing purposes
         */
        public PushProxiesValueImpl(Version version, byte[] guid, 
                byte features, int fwtVersion, 
                int port, Set<? extends IpPort> proxies) {
            super(version);
            
            this.guid = guid;
            this.features = features;
            this.fwtVersion = fwtVersion;
            this.port = port;
            this.proxies = proxies;
            this.data = serialize(this);
        }
        
        public PushProxiesValueImpl(Version version, byte[] data) throws DHTValueException {
            super(version);
            
            if (version == null) {
                throw new DHTValueException("Version is null");
            }
            
            if (data == null) {
                throw new DHTValueException("Data is null");
            }
            
            this.data = data;
            
            try {
                GGEP ggep = new GGEP(data, 0);
                
                this.guid = ggep.getBytes(CLIENT_ID);
                if (guid.length != 16) {
                    throw new DHTValueException("Illegal GUID length: " + guid.length);
                }
                
                // Ideally this would be changed to getByte and getByte would be added,
                // but since clients in the field are inserting features as an int,
                // we need to preserve the functionality.
                this.features = (byte)ggep.getInt(FEATURES);
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
                    
                    if (length != 6 && length != 18) {
                        throw new IOException("Illegal IP:Port length: " + length);
                    }
                    
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
        
        /*
         * (non-Javadoc)
         * @see org.limewire.mojito.db.DHTValue#getValue()
         */
        public byte[] getValue() {
            byte[] copy = new byte[data.length];
            System.arraycopy(data, 0, copy, 0, data.length);
            return copy;
        }

        /*
         * (non-Javadoc)
         * @see org.limewire.mojito.db.DHTValue#write(java.io.OutputStream)
         */
        public void write(OutputStream out) throws IOException {
            out.write(data);
        }

        /*
         * (non-Javadoc)
         * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getGUID()
         */
        public byte[] getGUID() {
            return guid;
        }
        
        /*
         * (non-Javadoc)
         * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getFeatures()
         */
        public byte getFeatures() {
            return features;
        }

        /*
         * (non-Javadoc)
         * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getFwtVersion()
         */
        public int getFwtVersion() {
            return fwtVersion;
        }

        /*
         * (non-Javadoc)
         * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getPort()
         */
        public int getPort() {
            return port;
        }

        /*
         * (non-Javadoc)
         * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getPushProxies()
         */
        public Set<? extends IpPort> getPushProxies() {
            return proxies;
        }
    }
    
    /**
     * An implementation of PushProxiesDHTValue for the localhost
     */
    private static class PushProxiesForSelf extends PushProxiesValue {
        
        private static final long serialVersionUID = -3222117316287224578L;
        
        public PushProxiesForSelf() {
            super(VERSION);
        }

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

        public byte[] getGUID() {
            return RouterService.getMyGUID();
        }
        
        public byte getFeatures() {
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
    }
}
