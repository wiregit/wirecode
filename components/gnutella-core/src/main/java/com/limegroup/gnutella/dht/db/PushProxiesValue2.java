package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueImpl;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.net.address.StrictIpPortSet;
import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;

public abstract class PushProxiesValue2 implements SerializableValue {

    /**
     * DHTValueType for Push Proxies.
     */
    public static final DHTValueType PUSH_PROXIES 
        = DHTValueType.valueOf("Gnutella Push Proxy", "PROX");
    
    /**
     * Version of PushProxiesDHTValue.
     */
    public static final Version VERSION = Version.valueOf(0);
        
    static final String CLIENT_ID = "client-id";
    
    static final String FWT_VERSION = "fwt-version";
    
    static final String FEATURES = "features";
    
    static final String PORT = "port";
    
    static final String PROXIES = "proxies";
    
    static final String TLS = "tls";
    
    private final Version version;
    
    public PushProxiesValue2(Version version) {
        this.version = version;
    }
    
    /**
     * 
     */
    public Version getVersion() {
        return version;
    }
    
    /**
     * The Client ID of the Gnutella Node.
     */
    public abstract byte[] getGUID();

    /**
     * The Port number of the Gnutella Node.
     */
    public abstract int getPort();

    /**
     * The supported features of the Gnutella Node.
     */
    public abstract byte getFeatures();

    /**
     * The version of the firewalls-to-firewall 
     * transfer protocol.
     */
    public abstract int getFwtVersion();

    /**
     * A Set of Push Proxies of the Gnutella Node.
     */
    public abstract Set<? extends IpPort> getPushProxies();

    /**
     * @return BitNumbers for TLS status of push proxies,
     * or empty Bit numbers
     */
    public abstract BitNumbers getTLSInfo();
    
    @Override
    public DHTValue serialize() {
        
        GGEP ggep = new GGEP();
        ggep.put(CLIENT_ID, getGUID());
        // Preserve insertion as an int, not a byte, for backwards compatability.
        ggep.put(FEATURES, (int)getFeatures());
        ggep.put(FWT_VERSION, getFwtVersion());
        
        byte[] port = new byte[2];
        ByteUtils.short2beb((short)getPort(), port, 0);
        ggep.put(PORT, port);
        
        try {
            Set<? extends IpPort> proxies = getPushProxies();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (IpPort proxy : proxies) {
                byte[] ipp = NetworkUtils.getBytes(proxy, java.nio.ByteOrder.BIG_ENDIAN);
                assert (ipp.length == 6 || ipp.length == 18);
                baos.write(ipp.length);
                baos.write(ipp);
            }
            baos.close();
            ggep.put(PROXIES, baos.toByteArray());
            
            if (!getTLSInfo().isEmpty())
                ggep.put(TLS, getTLSInfo().toByteArray());
        } catch (IOException err) {
            // Impossible
            throw new RuntimeException(err);
        }
        
        return new DHTValueImpl(PUSH_PROXIES, version, ggep.toByteArray());
    }
    
    static BitNumbers getNumbersFromProxies(Set<? extends IpPort> proxies) {
        return BitNumbers.synchronizedBitNumbers(HTTPHeaderUtils.getTLSIndices(proxies));
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(getGUID());
    }
    
    @Override
    public final boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof PushProxiesValue2)) {
            return false;
        }
        
        PushProxiesValue2 other = (PushProxiesValue2)o;
        return Arrays.equals(getGUID(), other.getGUID())
            && getPort() == other.getPort()
            && getFeatures() == other.getFeatures()
            && getFwtVersion() == other.getFwtVersion()
            && getPushProxies().equals(other.getPushProxies())
            && getTLSInfo().equals(other.getTLSInfo())
            && getVersion().equals(other.getVersion());
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("GUID=").append(new GUID(getGUID())).append("\n");
        buffer.append("Features=").append(getFeatures()).append("\n");
        buffer.append("FWTVersion=").append(getFwtVersion()).append("\n");
        buffer.append("PushProxies=").append(getPushProxies()).append("\n");
        return buffer.toString();
    }
    
    /**
     * 
     */
    @Singleton
    public static class Self extends PushProxiesValue2 {
        
        private final NetworkManager networkManager;
        
        private final ApplicationServices applicationServices;

        private final PushEndpoint pushEndpoint;

        @Inject
        public Self(NetworkManager networkManager,
                ApplicationServices applicationServices,
                PushEndpointFactory pushEndpointFactory) {
            super(VERSION);
            
            this.networkManager = networkManager;
            this.applicationServices = applicationServices;
            this.pushEndpoint = pushEndpointFactory.createForSelf();
        }
        
        @Override
        public byte[] getGUID() {
            return applicationServices.getMyGUID();
        }
        
        @Override
        public byte getFeatures() {
            return pushEndpoint.getFeatures();
        }

        @Override
        public int getFwtVersion() {
            return pushEndpoint.getFWTVersion();
        }
        
        @Override
        public int getPort() {
            return pushEndpoint.getPort();
        }

        @Override
        public Set<? extends IpPort> getPushProxies() {
            if (networkManager.acceptedIncomingConnection() 
                    && networkManager.isIpPortValid()) {
                // port should be the same as returned in #get
                return new StrictIpPortSet<Connectable>(new ConnectableImpl(
                        new IpPortImpl(networkManager.getAddress(), 
                                networkManager.getPort()), 
                                networkManager.isIncomingTLSEnabled()));
            } else {
                return pushEndpoint.getProxies();
            }
        }
        
        @Override
        public BitNumbers getTLSInfo() {
            Set<? extends IpPort> proxies = getPushProxies();
            return getNumbersFromProxies(proxies);
        }
    }
    
    /**
     * 
     */
    public static class Impl extends PushProxiesValue2 {
        
        /**
         * The GUID of the Gnutella Node.
         */
        private final byte[] guid;
        
        /**
         * Gnutella features bit-field.
         */
        private final byte features;
        
        /**
         * Gnutella Firewall-2-Firewall Transfer Protocol version.
         */
        private final int fwtVersion;
        
        /**
         * The port number which may differ from the Contact addresse's
         * port number.
         */
        private final int port;
        
        /**
         * A Set of PushProxy IpPorts.
         */
        private final Set<? extends IpPort> proxies;
        
        /**
         * TLS info for the push proxies.
         */
        private final BitNumbers tlsInfo;
        
        /**
         * 
         */
        public Impl(DHTValue value) throws IOException {
            this(value.getValueType(), value.getVersion(), value.getValue());
        }
        
        public Impl(Version version, byte[] data) throws IOException {
            this (PUSH_PROXIES, version, data);
        }
        
        /**
         * 
         */
        private Impl(DHTValueType valueType, Version version, byte[] data) throws IOException {
            super(version);
            
            if (!valueType.equals(PUSH_PROXIES)) {
                throw new IOException();
            }
            
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
                this.port = ByteUtils.beb2short(portBytes, 0) & 0xFFFF;
                if (!NetworkUtils.isValidPort(port)) {
                    throw new DHTValueException("Illegal port: " + port);
                }
                
                BitNumbers tlsInfo = BitNumbers.EMPTY_BN;
                try {
                    tlsInfo = new BitNumbers(ggep.getBytes(TLS));
                } catch (BadGGEPPropertyException notThere) {}
                
                byte[] proxiesBytes = ggep.getBytes(PROXIES);
                ByteArrayInputStream bais = new ByteArrayInputStream(proxiesBytes);
                DataInputStream in = new DataInputStream(bais);
                
                Set<IpPort> proxies = new IpPortSet();
                int id = 0;
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
                    
                    IpPort proxy = new IpPortImpl(InetAddress.getByAddress(addr), port);
                    if (tlsInfo.isSet(id++)) {
                        proxy = new ConnectableImpl(proxy, true);
                    }
                    proxies.add(proxy);
                }
                
                this.proxies = proxies;
                this.tlsInfo = tlsInfo;
                
            } catch (BadGGEPPropertyException err) {
                throw new IOException("BadGGEPPropertyException", err);
                
            } catch (BadGGEPBlockException err) {
                throw new IOException("BadGGEPBlockException", err);
            }
        }
        
        /**
         * Constructor for testing purposes.
         */
        public Impl(Version version, byte[] guid, 
                byte features, int fwtVersion, 
                int port, Set<? extends IpPort> proxies) {
            super(version);
            
            this.guid = guid;
            this.features = features;
            this.fwtVersion = fwtVersion;
            this.port = port;
            this.proxies = new IpPortSet(proxies);
            this.tlsInfo = getNumbersFromProxies(proxies);
        }
        
        @Override
        public byte[] getGUID() {
            return guid;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public byte getFeatures() {
            return features;
        }

        @Override
        public int getFwtVersion() {
            return fwtVersion;
        }

        @Override
        public Set<? extends IpPort> getPushProxies() {
            return proxies;
        }

        @Override
        public BitNumbers getTLSInfo() {
            return tlsInfo;
        }
    }
}
