package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GGEP;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.storage.Value;
import org.limewire.mojito.storage.ValueType;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;

/**
 * A default implementation of {@link PushProxiesValue}.
 */
public class DefaultPushProxiesValue extends AbstractPushProxiesValue {
    
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
     * Creates a {@link DefaultPushProxiesValue}.
     */
    public DefaultPushProxiesValue(Value value) throws IOException {
        this(value.getValueType(), value.getVersion(), value.getValue());
    }
    
    /**
     * Creates a {@link DefaultPushProxiesValue}.
     */
    public DefaultPushProxiesValue(Version version, 
            byte[] data) throws IOException {
        this (PUSH_PROXIES, version, data);
    }
    
    /**
     * Creates a {@link DefaultPushProxiesValue}.
     */
    private DefaultPushProxiesValue(ValueType valueType, 
            Version version, byte[] data) throws IOException {
        
        super(version);
        
        if (!valueType.equals(PUSH_PROXIES)) {
            throw new IOException();
        }
        
        try {
            GGEP ggep = new GGEP(data, 0);
            
            this.guid = ggep.getBytes(CLIENT_ID);
            if (guid.length != 16) {
                throw new BadValueException("Illegal GUID length: " + guid.length);
            }
            
            // Ideally this would be changed to getByte and getByte would be added,
            // but since clients in the field are inserting features as an int,
            // we need to preserve the functionality.
            this.features = (byte)ggep.getInt(FEATURES);
            this.fwtVersion = ggep.getInt(FWT_VERSION);
            
            byte[] portBytes = ggep.getBytes(PORT);
            this.port = ByteUtils.beb2short(portBytes, 0) & 0xFFFF;
            if (!NetworkUtils.isValidPort(port)) {
                throw new BadValueException("Illegal port: " + port);
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
                    throw new BadValueException("Illegal port: " + port);
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
     * Creates a {@link DefaultPushProxiesValue}.
     */
    public DefaultPushProxiesValue(NetworkManager networkManager, 
            ApplicationServices applicationServices, 
            PushEndpointFactory pushEndpointFactory) {
        super(VERSION);
        
        PushEndpoint endpoint = pushEndpointFactory.createForSelf();
        
        this.guid = applicationServices.getMyGUID();
        this.features = endpoint.getFeatures();
        this.fwtVersion = endpoint.getFWTVersion();
        this.port = endpoint.getPort();
        this.proxies = getPushProxies(networkManager, endpoint);
        this.tlsInfo = getNumbersFromProxies(proxies);
    }
    
    /**
     * Constructor for testing purposes.
     */
    public DefaultPushProxiesValue(Version version, byte[] guid, 
            byte features, int fwtVersion, 
            int port, Set<? extends IpPort> proxies) {
        super (version);
        
        this.guid = guid;
        this.features = features;
        this.fwtVersion = fwtVersion;
        this.port = port;
        this.proxies = new IpPortSet(proxies);
        this.tlsInfo = getNumbersFromProxies(proxies);
    }
    
    @Override
    public Version getVersion() {
        return version;
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
