package com.limegroup.gnutella;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.rudp.UDPConnection;

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
                IpPort.EMPTY_SET,
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
    public Set<? extends IpPort> getProxies() {
        return RouterService.getConnectionManager().getPushProxies();
    }
    
    /**
     * we always have the same features
     */
    public int getFeatures() {
    	return 0;
    }
    
    /**
     * we support the same FWT version if we support FWT at all
     */
    public int supportsFWTVersion() {
    	return UDPService.instance().canDoFWT() ? UDPConnection.VERSION : 0;
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
        if (UDPService.instance().canDoFWT() 
                && !RouterService.acceptedIncomingConnection())
            return UDPService.instance().getStableUDPPort();
        return RouterService.getPort();
    }
    
    protected IpPort getValidExternalAddress() {
        try {
            String addr = getAddress();
            int port = getPort();
            if (addr.equals(RemoteFileDesc.BOGUS_IP) || 
                    !NetworkUtils.isValidPort(port))
                return null;
            return new IpPortImpl(addr,getPort());
            
        }catch(UnknownHostException bad) {
            return null;
        }
    }
}
