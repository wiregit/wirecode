package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

import com.limegroup.gnutella.udpconnect.UDPConnection;
/**
 * A push endpoint for myself.  This differs from the standard
 * push endpoints because it always returns the current connections
 * as the set of push proxies.
 */
public class PushEndpointForSelf extends PushEndpoint {
    
    private static final PushEndpointForSelf INSTANCE = 
        new PushEndpointForSelf();
    /**
     * create an empty set of push proxies
     */
    private PushEndpointForSelf() {
        super(RouterService.getMessageRouter().getOurGUID(),
                Collections.EMPTY_SET,
                0,
                UDPConnection.VERSION);
    
    }
    
    public static PushEndpointForSelf instance() {
        return INSTANCE;
    }
    
    /**
     * delegate the call to connection manager
     */
    public Set getProxies() {
        return RouterService.getConnectionManager().getPushProxies();
    }
    
    /**
     * override to not cache the size
     */
    public int getSizeBytes() {
        return HEADER_SIZE+
			Math.min(getProxies().size(),4) * PROXY_SIZE;
    }
    
    /**
     * override to not cache the hashcode
     */
    public int hashcode() {
        return getHashcode();
    }
}
