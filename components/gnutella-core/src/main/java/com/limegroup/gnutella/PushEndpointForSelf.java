package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Set;

import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.NetworkUtils;
/**
 * A push endpoint for myself.  This differs from the standard
 * push endpoints because it always returns the current connections
 * as the set of push proxies.
 */
public class PushEndpointForSelf extends PushEndpoint {
    
    private static PushEndpointForSelf _instance;
    
    /**
     * create an empty set of push proxies.  Since these objects
     * will often be created before we know our external address,
     * do not initialize that.
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
    
    /**
     * Our address is our external address if it is valid and external.
     * Otherwise we return the BOGUS_IP 
     */
    public String getAddress() {
        byte [] addr = RouterService.getExternalAddress();
        
        if (NetworkUtils.isValidAddress(addr) &&
                !NetworkUtils.isPrivateAddress(addr)) 
            return NetworkUtils.ip2string(addr);
        
        return RemoteFileDesc.BOGUS_IP;
    }
    
    /**
     * @return our external address.  First converts it to string since
     * 1.3 jvms does not support getting it from byte[].
     */
    public InetAddress getInetAddress() {
        try {
            return InetAddress.getByName(getAddress());
        }catch(UnknownHostException bad){
            return null;
        }
    }
    
    /**
     * Our port is our external port
     */
    public int getPort() {
        int port = RouterService.getPort();
        if (NetworkUtils.isValidPort(port))
            return port;
        return 6346;
    }
    
    /**
     * appends our external address at the end, if valid && external.
     */
    public String httpStringValue() {
        StringBuffer sup = new StringBuffer(super.httpStringValue());
        String addr = getAddress();
        if (!addr.equals(RemoteFileDesc.BOGUS_IP)) {
            sup.append(";");
            sup.append(getPort());
            sup.append(":");
            sup.append(addr);
        }
        return sup.toString();
    }
}
