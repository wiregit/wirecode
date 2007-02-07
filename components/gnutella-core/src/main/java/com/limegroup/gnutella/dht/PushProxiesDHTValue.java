package com.limegroup.gnutella.dht;

import java.util.Set;

import org.limewire.io.IpPort;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;

/**
 * 
 */
public interface PushProxiesDHTValue extends DHTValue {
    
    /**
     * DHTValueType for Push Proxies
     */
    public static final DHTValueType PUSH_PROXIES = DHTValueType.valueOf("PROX");
    
    /**
     * Version of PushProxiesDHTValue
     */
    public static final Version VERSION = Version.valueOf(0, 0);
    
    /**
     * Returns a Set of Push Proxies
     */
    public Set<? extends IpPort> getPushProxies();
}
