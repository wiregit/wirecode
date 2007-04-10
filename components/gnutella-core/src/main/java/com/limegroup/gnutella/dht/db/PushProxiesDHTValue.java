package com.limegroup.gnutella.dht.db;

import java.util.Set;

import org.limewire.io.IpPort;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;

/**
 * An interface for Gnutella Push Proxies
 */
public interface PushProxiesDHTValue extends DHTValue {
    
    /**
     * DHTValueType for Push Proxies
     */
    public static final DHTValueType PUSH_PROXIES = DHTValueType.valueOf("Gnutella Push Proxy", "PROX");
    
    /**
     * Version of PushProxiesDHTValue
     */
    public static final Version VERSION = Version.valueOf(0);
    
    /**
     * The Port number of the Gnutella Node
     */
    public int getPort();
    
    /**
     * The supported features of the Gnutella Node
     */
    public int getFeatures();
    
    /**
     * The version of the firewalls-to-firewall 
     * transfer protocol
     */
    public int getFwtVersion();
    
    /**
     * A Set of Push Proxies of the Gnutella Node
     */
    public Set<? extends IpPort> getPushProxies();
    
    /**
     * Returns true if this DHTValue is for self (localhost)
     */
    public boolean isPushProxiesForSelf();
}
