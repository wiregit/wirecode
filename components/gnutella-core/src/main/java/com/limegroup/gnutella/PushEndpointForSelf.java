package com.limegroup.gnutella;

import java.util.Collections;
import java.util.Set;

import com.limegroup.gnutella.udpconnect.UDPConnection;
/**
 * A push endpoint for myself.  This differs from the standard
 * push endpoints because it always returns the current connections
 * as the set of push proxies.
 */
public class PushEndpointForSelf extends PushEndpoint {
    
    private static PushEndpointForSelf _instance;
    
    /**
     * create an empty set of push proxies
     */
    private PushEndpointForSelf() {
        super(RouterService.getMyGUID(),
                Collections.EMPTY_SET,
                0,
                UDPConnection.VERSION);
    
    }
    
    public static PushEndpointForSelf instance() {
        if (_instance == null)
            _instance = new PushEndpointForSelf();
        return _instance;
    }
    
    
    /**
     * delegate the call to connection manager
     */
    public Set getProxies() {
        return RouterService.getConnectionManager().getPushProxies();
    }
    
    /**
     * we always have the same features
     */
    public int getFeatures() {
    	return 0;
    }
    
    /**
     * we always support the same fwt version
     */
    public int supportsFWTVersion() {
    	return UDPConnection.VERSION;
    }
    
}
