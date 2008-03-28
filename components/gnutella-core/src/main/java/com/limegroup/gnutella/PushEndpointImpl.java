package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkInstanceUtils;


/**
 * a class that represents an endpoint behind one or more PushProxies.
 * almost everything is immutable including the contents of the set.
 * 
 * the network format this is serialized to is:
 * byte 0 (from right-to-left): 
 *    - bits 0-2 how many push proxies we have (so max is 7)
 *    - bits 3-4 the version of the f2f transfer protocol this altloc supports
 *    - bits 5-6 other possible features.
 *    - bit  7   set if the TLS-capable push proxy indexes byte is included
 * bytes 1-16 : the guid
 * bytes 17-22: ip:port of the address (if FWT version > 0)
 * followed by a byte of TLS-capable PushProxy indexes (if bit 7 of features is set)
 * followed by 6 bytes per PushProxy 
 * 
 * the http format this is serialized to is an ascii string consisting of
 * ';'-delimited tokens.  The first token is the client GUID represented in hex
 * and is the only required token.  The other tokens can be addresses of push proxies
 * or various feature headers.  At most one of the tokens should be the external ip and port 
 * of the firewalled node in a port:ip format. Currently the only feature header we 
 * parse is the fwawt header that contains the version number of the firewall to 
 * firewall transfer protocol supported by the altloc.  In addition, the 'pptls=' field
 * can indicate which, if any, push proxies support TLS.  If the field is present, it 
 * must be immediately before the listing of the push proxies.  The hexadecimal string
 * after the '=' is a bit-representation of which push proxies are valid for TLS.
 * 
 * A PE does not need to know the actual external address of the firewalled host,
 * however without that knowledge we cannot do firewall-to-firewall transfer with 
 * the given host.  Also, the RemoteFileDesc objects requires a valid IP for construction,
 * so in the case we do not know the external address we return a BOGUS_IP.
 * 
 * Examples:
 * 
 *  //altloc with 2 proxies and supports firewall transfer 1 :
 * 
 * <ThisIsTheGUIDASDF>;fwt/1.0;20.30.40.50:60;1.2.3.4:5567
 * 
 *   //altloc with 1 proxy and doesn't support firewall transfer, with external address:
 * 
 * <ThisIsTHeGUIDasfdaa527>;1.2.3.4:5564;6346:2.3.4.5
 * 
 * //altloc with 1 proxy and supports two features we don't know/care about :
 * 
 * <ThisIsTHeGUIDasfdaa527>;someFeature/3.2;10.20.30.40:5564;otherFeature/0.4
 * 
 * //altloc with 1 proxy (the first) that's TLS capable, 1 that isn't:
 * 
 * <ThisIsTheGUIDASDF>;fwt/1.0;pptls=8;20.30.40.50:60;1.2.3.4:5567
 * 
 *  //altloc without any proxies and doesn't support any features
 *  // not very useful, but still valid  
 * 
 * <ThisIsTheGUIDasdf23457>
 */
public class PushEndpointImpl extends AbstractPushEndpoint {
    /**
	 * the client guid of the endpoint
	 */
	private final byte [] _clientGUID;
	
	/**
	 * the guid as an object to avoid recreating
	 * If there are other PushEnpoint objects, they all will ultimately
	 * point to the same GUID object.  This ensures that as long as
	 * there is at least one PE object for a remote host, the set of
	 * proxies will not be gc-ed.
	 */
	private GUID _guid;
	
	/**
	 * the various features this PE supports.
	 */
	private final int _features;
	
	/**
	 * the version of firewall to firewall transfer protocol
	 * this endpoint supports.  
	 */
	private final int _fwtVersion;
	
	/**
	 * the set of proxies this has immediately after creating the endpoint
	 * cleared after registering in the map.  This is used only to 
	 * hold the parsed proxies until they are put in the map.
	 */
	private Set<? extends IpPort> _proxies;
	
	/**
	 * the external address of this PE.  Needed for firewall-to-firewall
	 * transfers, but can be null.
	 */
	private final IpPort _externalAddr;
    
    private final PushEndpointCache pushEndpointCache;

    private final NetworkInstanceUtils networkInstanceUtils;

    public PushEndpointImpl(byte[] guid, Set<? extends IpPort> proxies, byte features,
            int fwtVersion, IpPort addr, PushEndpointCache pushEndpointCache, NetworkInstanceUtils networkInstanceUtils) {
        this.pushEndpointCache = pushEndpointCache;
        this.networkInstanceUtils = networkInstanceUtils;
        
		_features = ((features & FEATURES_MASK) | (fwtVersion << 3));
		_fwtVersion=fwtVersion;
		_clientGUID=guid;
		_guid = new GUID(_clientGUID);
		if (proxies != null) {
            if (proxies instanceof IpPortSet)
                _proxies = Collections.unmodifiableSet(proxies);
            else
                _proxies = Collections.unmodifiableSet(new IpPortSet(proxies));
        } else
            _proxies = Collections.emptySet();
		_externalAddr = addr;
	}

	/**
	 * 
	 * @return an IpPort representing our valid external
	 * address, or null if we don't have such.
	 */
	public IpPort getValidExternalAddress() {
        IpPort ret = getIpPort();
	    if (ret == null || !networkInstanceUtils.isValidExternalIpPort(ret))
	        return null;
        
        assert !ret.getAddress().equals(RemoteFileDesc.BOGUS_IP) : 
            "bogus ip address leaked, field is "+_externalAddr+
            " cache contains "+pushEndpointCache.getCached(_guid);
        
	    return ret;
	}
	
	public byte [] getClientGUID() {
		return _clientGUID;
	}
	
	public Set<? extends IpPort> getProxies() {

	    synchronized(this) {
	    	if (_proxies!=null)
	        	return _proxies;
	    }

	    PushEndpoint current = pushEndpointCache.getCached(_guid);
	    if (current == null)
            return Collections.emptySet();        
	    return current.getProxies();
	}
	
	
	public int getFWTVersion() {
		PushEndpoint current = pushEndpointCache.getCached(_guid);
		int currentVersion = current == null ? 
				_fwtVersion : current.getFWTVersion();
		return currentVersion;
	}
	
	public byte getFeatures() {
		PushEndpoint current = pushEndpointCache.getCached(_guid);
		int currentFeatures = current==null ? _features : current.getFeatures();
		return (byte)(currentFeatures & FEATURES_MASK);
	}

	private IpPort getIpPort() {
        PushEndpoint current = pushEndpointCache.getCached(_guid);
        return current == null || current.getValidExternalAddress() == null ?
                _externalAddr :         current.getValidExternalAddress();
    }
    
	/**
     * Implements the IpPort interface, returning a bogus ip if we don't know
     * it.
     * 
     * @return the external address if known otherwise {@link RemoteFileDesc#BOGUS_IP}
     */
    public String getAddress() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getAddress() : RemoteFileDesc.BOGUS_IP;
    }
    
    public InetAddress getInetAddress() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getInetAddress() : null;
    }
    
    /**
     * Implements the IpPort interface, returning a bogus port if we don't know it
     * 
     * @return the port of the external address if known otherwise 6346
     */
    public int getPort() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getPort() : 6346;
    }
    
    public InetSocketAddress getInetSocketAddress() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getInetSocketAddress() : null;
    }
    
    public boolean isLocal() {
        return false;
    }
	
	public synchronized void updateProxies(boolean good) {
        _guid = pushEndpointCache.updateProxiesFor(_guid, this, good);
        _proxies = null;
    }
    
    public PushEndpoint createClone() {
        return new PushEndpointImpl(_guid.bytes(), getProxies(), getFeatures(), getFWTVersion(), getIpPort(), pushEndpointCache, networkInstanceUtils);
    }
	
}
