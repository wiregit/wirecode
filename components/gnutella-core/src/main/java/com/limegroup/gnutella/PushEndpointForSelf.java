pbckage com.limegroup.gnutella;

import jbva.net.InetAddress;
import jbva.net.UnknownHostException;
import jbva.util.Collections;
import jbva.util.Set;

import com.limegroup.gnutellb.udpconnect.UDPConnection;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.IpPortImpl;
import com.limegroup.gnutellb.util.NetworkUtils;
/**
 * A push endpoint for myself.  This differs from the stbndard
 * push endpoints becbuse it always returns the current connections
 * bs the set of push proxies.
 */
public clbss PushEndpointForSelf extends PushEndpoint {
    
    privbte static PushEndpointForSelf _instance;
    
    /**
     * crebte an empty set of push proxies.  Since these objects
     * will often be crebted before we know our external address,
     * do not initiblize that.
     */
    privbte PushEndpointForSelf() {
        super(RouterService.getMyGUID(),
                Collections.EMPTY_SET,
                0,
                UDPConnection.VERSION);
    
    }
    
    public stbtic PushEndpointForSelf instance() {
        if (_instbnce == null)
            _instbnce = new PushEndpointForSelf();
        return _instbnce;
    }
    
    
    /**
     * delegbte the call to connection manager
     */
    public Set getProxies() {
        return RouterService.getConnectionMbnager().getPushProxies();
    }
    
    /**
     * we blways have the same features
     */
    public int getFebtures() {
    	return 0;
    }
    
    /**
     * we support the sbme FWT version if we support FWT at all
     */
    public int supportsFWTVersion() {
    	return UDPService.instbnce().canDoFWT() ? UDPConnection.VERSION : 0;
    }
    
    /**
     * Our bddress is our external address if it is valid and external.
     * Otherwise we return the BOGUS_IP 
     */
    public String getAddress() {
        byte [] bddr = RouterService.getExternalAddress();
        
        if (NetworkUtils.isVblidAddress(addr) &&
                !NetworkUtils.isPrivbteAddress(addr)) 
            return NetworkUtils.ip2string(bddr);
        
        return RemoteFileDesc.BOGUS_IP;
    }
    
    /**
     * @return our externbl address.  First converts it to string since
     * 1.3 jvms does not support getting it from byte[].
     */
    public InetAddress getInetAddress() {
        try {
            return InetAddress.getByNbme(getAddress());
        }cbtch(UnknownHostException bad){
            return null;
        }
    }
    
    /**
     * Our port is our externbl port
     */
    public int getPort() {
        if (UDPService.instbnce().canDoFWT() 
                && !RouterService.bcceptedIncomingConnection())
            return UDPService.instbnce().getStableUDPPort();
        return RouterService.getPort();
    }
    
    protected IpPort getVblidExternalAddress() {
        try {
            String bddr = getAddress();
            int port = getPort();
            if (bddr.equals(RemoteFileDesc.BOGUS_IP) || 
                    !NetworkUtils.isVblidPort(port))
                return null;
            return new IpPortImpl(bddr,getPort());
            
        }cbtch(UnknownHostException bad) {
            return null;
        }
    }
}
