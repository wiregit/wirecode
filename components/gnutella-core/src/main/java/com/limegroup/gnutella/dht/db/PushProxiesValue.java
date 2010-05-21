package com.limegroup.gnutella.dht.db;

import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.IpPort;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValueType;

/**
 * 
 */
public interface PushProxiesValue extends SerializableValue {

    /**
     * {@link DHTValueType} for Push-Proxies.
     */
    public static final DHTValueType PUSH_PROXIES 
        = DHTValueType.valueOf("Gnutella Push Proxy", "PROX");

    /**
     * Version of {@link PushProxiesValue}.
     */
    public static final Version VERSION = Version.valueOf(0);

    /**
     * Returns the {@link Version}
     */
    public Version getVersion();

    /**
     * The Client ID of the Gnutella Node.
     */
    public byte[] getGUID();

    /**
     * The Port number of the Gnutella Node.
     */
    public int getPort();

    /**
     * The supported features of the Gnutella Node.
     */
    public byte getFeatures();

    /**
     * The version of the firewalls-to-firewall 
     * transfer protocol.
     */
    public int getFwtVersion();

    /**
     * A Set of Push Proxies of the Gnutella Node.
     */
    public Set<? extends IpPort> getPushProxies();

    /**
     * @return BitNumbers for TLS status of push proxies,
     * or empty Bit numbers
     */
    public BitNumbers getTLSInfo();

}