package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.GGEP;

/**
 * An implementation of DHTValue for Gnutella Push Proxies
 */
public abstract class AbstractPushProxiesValue implements PushProxiesValue {

    /**
     * DHTValueType for Push Proxies
     */
    public static final DHTValueType PUSH_PROXIES = DHTValueType.valueOf("Gnutella Push Proxy", "PROX");
    
    /**
     * Version of PushProxiesDHTValue
     */
    public static final Version VERSION = Version.valueOf(0);
        
    static final String CLIENT_ID = "client-id";
    
    static final String FWT_VERSION = "fwt-version";
    
    static final String FEATURES = "features";
    
    static final String PORT = "port";
    
    static final String PROXIES = "proxies";
    
    static final String TLS = "tls";
    
    private final Version version;
    
    public AbstractPushProxiesValue(Version version) {
        this.version = version;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getValueType()
     */
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getValueType()
     */
    public DHTValueType getValueType() {
        return PUSH_PROXIES;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getVersion()
     */
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getVersion()
     */
    public Version getVersion() {
        return version;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#size()
     */
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#size()
     */
    public int size() {
        return getValue().length;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getGUID()
     */
    public abstract byte[] getGUID();
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getPort()
     */
    public abstract int getPort();
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getFeatures()
     */
    public abstract byte getFeatures();
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getFwtVersion()
     */
    public abstract int getFwtVersion();
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getPushProxies()
     */
    public abstract Set<? extends IpPort> getPushProxies();
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getTLSInfo()
     */
    public abstract BitNumbers getTLSInfo();
    
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
        GGEP ggep = new GGEP();
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
            
            if (!value.getTLSInfo().isEmpty())
                ggep.put(TLS,value.getTLSInfo().toByteArray());
        } catch (IOException err) {
            // Impossible
            throw new RuntimeException(err);
        }
        
        return ggep.toByteArray();
    }
    
    static BitNumbers getNumbersFromProxies(Set<? extends IpPort> proxies) {
        BitNumbers tlsInfo = new BitNumbers(proxies.size());
        int i = 0;
        for (IpPort proxy : proxies) {
            if (proxy instanceof Connectable) {
                if (((Connectable)proxy).isTLSCapable())
                    tlsInfo.set(i);
            }
            i++;
        }
        return tlsInfo;
    }
}
