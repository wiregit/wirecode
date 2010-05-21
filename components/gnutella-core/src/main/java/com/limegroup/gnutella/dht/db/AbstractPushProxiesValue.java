package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueImpl;
import org.limewire.net.address.StrictIpPortSet;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;

public abstract class AbstractPushProxiesValue implements PushProxiesValue {

    static final String CLIENT_ID = "client-id";
    
    static final String FWT_VERSION = "fwt-version";
    
    static final String FEATURES = "features";
    
    static final String PORT = "port";
    
    static final String PROXIES = "proxies";
    
    static final String TLS = "tls";
    
    protected final Version version;
    
    public AbstractPushProxiesValue(Version version) {
        this.version = version;
    }

    @Override
    public Version getVersion() {
        return version;
    }
    
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
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(getGUID());
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof PushProxiesValue)) {
            return false;
        }
        
        PushProxiesValue other = (PushProxiesValue)o;
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
    static BitNumbers getNumbersFromProxies(Set<? extends IpPort> proxies) {
        return BitNumbers.synchronizedBitNumbers(
                HTTPHeaderUtils.getTLSIndices(proxies));
    }
    
    /**
     * Extracts and returns localhost's Push-Proxies.
     */
    static Set<? extends IpPort> getPushProxies(
            NetworkManager networkManager, PushEndpoint endpoint) {
        if (networkManager.acceptedIncomingConnection()
                && networkManager.isIpPortValid()) {
            return new StrictIpPortSet<Connectable>(new ConnectableImpl(
                    new IpPortImpl(networkManager.getAddress(), 
                            networkManager.getPort()), 
                            networkManager.isIncomingTLSEnabled()));
        }
        
        return endpoint.getProxies();
    }
}
