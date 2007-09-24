package com.limegroup.gnutella;



import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.PushEndpointCache.CachedPushEndpoint;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderValue;


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
public class PushEndpoint implements HTTPHeaderValue, IpPort {
    
	public static final int HEADER_SIZE=17; //guid+# of proxies, maybe other things too
	public static final int PROXY_SIZE=6; //ip:port
	
	public static final byte PLAIN=0x0; //no features for this PE
    public static final byte PPTLS_BINARY = (byte)0x80;
	
	static final byte SIZE_MASK=0x7; //0000 0111
	
	static final byte FWT_VERSION_MASK=0x18; //0001 1000
	
	//the features mask does not clear the bits we do not understand
	//because we may pass on the altloc to someone who does.
	static final byte FEATURES_MASK= (byte)0xE0;   //1110 0000
    
    /** The pptls portion constant. */
    public static final String PPTLS_HTTP = "pptls";
	
    /** The maximum number of proxies to use. */
    public static final int MAX_PROXIES = 4;
	
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

    PushEndpoint(byte[] guid, Set<? extends IpPort> proxies, byte features,
            int version, IpPort addr, PushEndpointCache pushEndpointCache) {
        this.pushEndpointCache = pushEndpointCache;
        
		_features = ((features & FEATURES_MASK) | (version << 3));
		_fwtVersion=version;
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
	 * @return a byte-packed representation of this
	 */
	public byte [] toBytes(boolean includeTLS) {
	    Set<? extends IpPort> proxies = getProxies();
	    int payloadSize = getSizeBytes(proxies, includeTLS);
	    IpPort addr = getValidExternalAddress();
        int FWTVersion = supportsFWTVersion();
	    if (addr != null && FWTVersion > 0)
	        payloadSize+=6;
		byte [] ret = new byte[payloadSize];
		toBytes(ret,0,proxies,addr,FWTVersion, includeTLS);
		return ret;
	}
	
	/**
	 * creates a byte packet representation of this
	 * @param where the byte [] to serialize to 
	 * @param offset the offset within that byte [] to serialize
	 */
	public void toBytes(byte [] where, int offset, boolean includeTLS) {
		toBytes(where, offset, getProxies(), getValidExternalAddress(),supportsFWTVersion(), includeTLS);
	}
	
	private void toBytes(byte []where, int offset, Set<? extends IpPort> proxies,
                         IpPort address, int FWTVersion, boolean includeTLS) {
	    
	    int neededSpace = getSizeBytes(proxies, includeTLS);
	    if (address != null) { 
            if (FWTVersion > 0)
                neededSpace+=6;
        } else {
            FWTVersion = 0;
        }
	    
	    if (where.length-offset < neededSpace)
			throw new IllegalArgumentException ("target array too small");
	    
        int featureIdx = offset;
		//store the number of proxies
		where[offset] = (byte)(Math.min(MAX_PROXIES,proxies.size()) 
		        | getFeatures() 
		        | FWTVersion << 3);
		
		//store the guid
		System.arraycopy(_clientGUID,0,where,++offset,16);
		offset+=16;
		
		//if we know the external address, store that too
		//if its valid and not private and port is valid
		if (address != null && FWTVersion > 0) {
		    byte [] addr = address.getInetAddress().getAddress();
		    int port = address.getPort();
		    
		    System.arraycopy(addr,0,where,offset,4);
		    offset+=4;
		    ByteOrder.short2leb((short)port,where,offset);
		    offset+=2;
		}
        
        // If we're including TLS, then add a byte for which proxies support it.
        BitNumbers bn = new BitNumbers(Math.min(proxies.size(), MAX_PROXIES));
        int pptlsIdx = offset;
        int i=0;
        if(includeTLS) {
            // If any of the proxies are TLS-capable, increment the offset
            for(IpPort ppi : proxies) {
                if(i >= MAX_PROXIES)
                    break;
                
                if(ppi instanceof Connectable && ((Connectable)ppi).isTLSCapable()) {
                    offset++;
                    break;
                }
                
                i++;
            }
        }
		
		//store the push proxies
		i=0;
        for(IpPort ppi : proxies) {
            if(i >= MAX_PROXIES)
                break;
            
            if(includeTLS && ppi instanceof Connectable && ((Connectable)ppi).isTLSCapable())
                bn.set(i);
            
			byte [] addr = ppi.getInetAddress().getAddress();
			short port = (short)ppi.getPort();
			
			System.arraycopy(addr,0,where,offset,4);
			offset+=4;
			ByteOrder.short2leb(port,where,offset);
			offset+=2;
			i++;
		}
        
        // insert the tls indexes & turn the feature on!
        if(!bn.isEmpty()) {
            byte[] tlsIndexes = bn.toByteArray();
            assert tlsIndexes.length == 1;
            where[pptlsIdx] = tlsIndexes[0];
            where[featureIdx] |= PPTLS_BINARY;
        } else {
            where[featureIdx] &= ~PPTLS_BINARY; // make sure its not in the features!
        }
	}
	
	/**
	 * 
	 * @return an IpPort representing our valid external
	 * address, or null if we don't have such.
	 */
	protected IpPort getValidExternalAddress() {
        IpPort ret = getIpPort();
	    if (ret == null || !NetworkUtils.isValidExternalIpPort(ret))
	        return null;
        
        assert !ret.getAddress().equals(RemoteFileDesc.BOGUS_IP) : "bogus ip address leaked";
	    return ret;
	}
	
    public byte [] getClientGUID() {
		return _clientGUID;
	}
	
	/**
	 * 
	 * @return a view of the current set of proxies.
	 */
	public Set<? extends IpPort> getProxies() {

	    synchronized(this) {
	    	if (_proxies!=null)
	        	return _proxies;
	    }

	    CachedPushEndpoint current = pushEndpointCache.getCached(_guid);
	    if (current == null)
            return Collections.emptySet();        
	    return current.getProxies();
	}
	
	/**
	 * @param the set of proxies for this PE
	 * @return how many bytes a PE will use when serialized.
	 */
	public static int getSizeBytes(Set<? extends IpPort> proxies, boolean includeTLS) {
        boolean hasTLS = false;
        if(includeTLS) {
            int i = 0;
            for(IpPort ipp : proxies) {
                if(i >= MAX_PROXIES)
                    break;
                
                if(ipp instanceof ConnectableImpl && ((Connectable)ipp).isTLSCapable()) {
                    hasTLS = true;
                    break;
                }
                i++;
            }
        }
		return HEADER_SIZE + (hasTLS ? 1 : 0 ) + Math.min(proxies.size(),MAX_PROXIES) * PROXY_SIZE;
	}
	
	/**
	 * @return which version of F2F transfers this PE supports.
	 * This always returns the most current version we know the PE supports
	 * unless it has never been put in the map.
	 */
	public int supportsFWTVersion() {
		CachedPushEndpoint current = pushEndpointCache.getCached(_guid);
		int currentVersion = current == null ? 
				_fwtVersion : current.getFWTVersion();
		return currentVersion;
	}
	
	public int hashCode() {
	    return _guid.hashCode();
	}
	
	public boolean equals(Object other) {
		
		//this method ignores the version of firewall-to-firewall 
		//transfers supported, the features and the sets of proxies
		if (other == null)
			return false;
		if (!(other instanceof PushEndpoint))
			return false;
		
		PushEndpoint o = (PushEndpoint)other;
		
		//same guid
		return  _guid.equals(o._guid); 
	}
	
	public String toString() {
		String ret = "PE [FEATURES:"+getFeatures()+", FWT Version:"+supportsFWTVersion()+
			", GUID:"+_guid+", address: "+
            getAddress()+":"+getPort()+", proxies:{ "; 
        for(IpPort ppi : getProxies()) {
			ret = ret+ppi.getInetAddress()+":"+ppi.getPort()+" ";
		}
		ret = ret+ "}]";
		return ret;
	}
	
	public String httpStringValue() {
        StringBuilder httpString =new StringBuilder(_guid.toHexString()).append(";");
		
		//if version is not 0, append it to the http string
	    int fwtVersion=supportsFWTVersion();
		if (fwtVersion!=0) {
			httpString.append(HTTPConstants.FW_TRANSFER)
				.append("/")
				.append(fwtVersion)
				.append(";");
		
			// append the external address of this endpoint if such exists
			// and is valid, non-private and if the port is valid as well.
			IpPort address = getValidExternalAddress();
			if (address!=null) {
			    String addr = getAddress();
			    int port = getPort();
			    if (!addr.equals(RemoteFileDesc.BOGUS_IP) && 
			            NetworkUtils.isValidPort(port)){
			        httpString.append(port)
			        .append(":")
			        .append(addr)
			        .append(";");
			    }
			}
		
		}
		
		int proxiesWritten=0;
        int proxyIndex = httpString.length();
        Set<? extends IpPort> proxies = getProxies();
        BitNumbers bn = new BitNumbers(proxies.size());
        for(IpPort cur : proxies) {
            if(proxiesWritten >= MAX_PROXIES)
                break;
			
            if(cur instanceof Connectable && ((Connectable)cur).isTLSCapable())
                bn.set(proxiesWritten);
                
            // use getInetAddress.getAddress to guarantee it's in bitform
			httpString.append(NetworkUtils.ip2string(
				        cur.getInetAddress().getAddress()));
			httpString.append(":").append(cur.getPort()).append(";");
			proxiesWritten++;
		}
        
        if(!bn.isEmpty())
            httpString.insert(proxyIndex, PPTLS_HTTP + "=" + bn.toHexString() + ";");
		
		//trim the ; at the end
		httpString.deleteCharAt(httpString.length()-1);
		
		return httpString.toString();
		
	}
	
	/**
	 * @return the various features this PE reports.  This always
	 * returns the most current features, or the ones it was created with
	 * if they have never been updated.
	 */
	public byte getFeatures() {
		CachedPushEndpoint current = pushEndpointCache.getCached(_guid);
		int currentFeatures = current==null ? _features : current.getFeatures();
		return (byte)(currentFeatures & FEATURES_MASK);
	}

	private IpPort getIpPort() {
        CachedPushEndpoint current = pushEndpointCache.getCached(_guid);
        return current == null || current.getIpPort() == null ? 
                _externalAddr : current.getIpPort();
    }
    
    /**
     * Implements the IpPort interface, returning a bogus ip if we don't know
     * it.
     */
    public String getAddress() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getAddress() : RemoteFileDesc.BOGUS_IP;
    }
    
    public InetAddress getInetAddress() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getInetAddress() : null;
    }
    
    /** Returns the GUID for this PushEndpoint. */
    public byte[] getGuid() {
        return _guid.bytes();
    }
    
    /**
     * Implements the IpPort interface, returning a bogus port if we don't know it
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
	
	/**
	 * Updates either the PushEndpoint or the GUID_PROXY_MAP to ensure
	 * that GUID_PROXY_MAP has a reference to all live PE GUIDs and
	 * all live PE's reference the same GUID object as in GUID_PROXY_MAP.
	 * 
	 * If this method is not called, the PE will know only about the set
	 * of proxies the remote host had when it was created.  Otherwise it
	 * will point to the most recent known set.
	 */
	public synchronized void updateProxies(boolean good) {
        _guid = pushEndpointCache.updateProxiesFor(_guid, this, good);
        _proxies = null;
    }
    
    public PushEndpoint createClone() {
        return new PushEndpoint(_guid.bytes(), getProxies(), getFeatures(), supportsFWTVersion(), getIpPort(), pushEndpointCache);
    }
	
}
