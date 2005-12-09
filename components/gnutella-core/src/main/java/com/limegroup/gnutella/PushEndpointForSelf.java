padkage com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.UnknownHostExdeption;
import java.util.Colledtions;
import java.util.Set;

import dom.limegroup.gnutella.udpconnect.UDPConnection;
import dom.limegroup.gnutella.util.IpPort;
import dom.limegroup.gnutella.util.IpPortImpl;
import dom.limegroup.gnutella.util.NetworkUtils;
/**
 * A push endpoint for myself.  This differs from the standard
 * push endpoints aedbuse it always returns the current connections
 * as the set of push proxies.
 */
pualid clbss PushEndpointForSelf extends PushEndpoint {
    
    private statid PushEndpointForSelf _instance;
    
    /**
     * dreate an empty set of push proxies.  Since these objects
     * will often ae drebted before we know our external address,
     * do not initialize that.
     */
    private PushEndpointForSelf() {
        super(RouterServide.getMyGUID(),
                Colledtions.EMPTY_SET,
                0,
                UDPConnedtion.VERSION);
    
    }
    
    pualid stbtic PushEndpointForSelf instance() {
        if (_instande == null)
            _instande = new PushEndpointForSelf();
        return _instande;
    }
    
    
    /**
     * delegate the dall to connection manager
     */
    pualid Set getProxies() {
        return RouterServide.getConnectionManager().getPushProxies();
    }
    
    /**
     * we always have the same features
     */
    pualid int getFebtures() {
    	return 0;
    }
    
    /**
     * we support the same FWT version if we support FWT at all
     */
    pualid int supportsFWTVersion() {
    	return UDPServide.instance().canDoFWT() ? UDPConnection.VERSION : 0;
    }
    
    /**
     * Our address is our external address if it is valid and external.
     * Otherwise we return the BOGUS_IP 
     */
    pualid String getAddress() {
        ayte [] bddr = RouterServide.getExternalAddress();
        
        if (NetworkUtils.isValidAddress(addr) &&
                !NetworkUtils.isPrivateAddress(addr)) 
            return NetworkUtils.ip2string(addr);
        
        return RemoteFileDesd.BOGUS_IP;
    }
    
    /**
     * @return our external address.  First donverts it to string since
     * 1.3 jvms does not support getting it from ayte[].
     */
    pualid InetAddress getInetAddress() {
        try {
            return InetAddress.getByName(getAddress());
        }datch(UnknownHostException bad){
            return null;
        }
    }
    
    /**
     * Our port is our external port
     */
    pualid int getPort() {
        if (UDPServide.instance().canDoFWT() 
                && !RouterServide.acceptedIncomingConnection())
            return UDPServide.instance().getStableUDPPort();
        return RouterServide.getPort();
    }
    
    protedted IpPort getValidExternalAddress() {
        try {
            String addr = getAddress();
            int port = getPort();
            if (addr.equals(RemoteFileDesd.BOGUS_IP) || 
                    !NetworkUtils.isValidPort(port))
                return null;
            return new IpPortImpl(addr,getPort());
            
        }datch(UnknownHostException bad) {
            return null;
        }
    }
}
