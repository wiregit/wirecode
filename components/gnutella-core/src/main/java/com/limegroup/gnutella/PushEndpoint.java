package com.limegroup.gnutella;



import java.io.DataInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IPPortCombo;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.messages.BadPacketException;


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
	
	private static final byte SIZE_MASK=0x7; //0000 0111
	
	private static final byte FWT_VERSION_MASK=0x18; //0001 1000
	
	//the features mask does not clear the bits we do not understand
	//because we may pass on the altloc to someone who does.
	private static final byte FEATURES_MASK= (byte)0xE0;   //1110 0000
    
    /** The pptls portion constant. */
    public static final String PPTLS_HTTP = "pptls";
	

    /**
     * A mapping from GUID to a GUIDSetWrapper.  This is used to ensure
     * that all PE's will have access to the same PushProxies, even if
     * multiple PE's exist for a single GUID.  Because access to the proxies
     * is referenced from this GUID_PROXY_MAP, the PE will always receive the
     * most up-to-date set of proxies.
     *
     * Insertion to this map must be manually performed, to allow for temporary
     * PE objects that are used to update pre-existing ones.
     *
     * There is no explicit removal from the map -- the Weak nature of it will
     * automatically remove the key/value pairs when the key is garbage
     * collected.  For this reason, all PEs must reference the exact GUID
     * object that is stored in the map -- to ensure that the map will not GC
     * the GUID while it is still in use by a PE.
     *
     * The value is a GUIDSetWrapper (containing a WeakReference to the
     * GUID key as well as the Set of proxies) so that subsequent PEs can 
     * reference the true key object.  A WeakReference is used to allow
     * GC'ing to still work and the map to ultimately remove unused keys.
     */
	private static final Map<GUID, GuidSetWrapper> GUID_PROXY_MAP = 
	    Collections.synchronizedMap(new WeakHashMap<GUID, GuidSetWrapper>());
    
    static {
        RouterService.schedule(new WeakCleaner(),30*1000,30*1000);
    }
    
    /** The maximum number of proxies to use. */
    private static final int MAX_PROXIES = 4;
	
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

	/**
	 * @param guid the client guid	
	 * @param proxies the push proxies for that host
	 */
	public PushEndpoint(byte [] guid, Set<? extends IpPort> proxies, byte features, int version) {
		this(guid,proxies,features,version,null);
	}
	
    @SuppressWarnings("unchecked")
    public PushEndpoint(byte [] guid, Set<? extends IpPort> proxies, byte features, int version,IpPort addr) {
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
	
	
	
	public PushEndpoint(byte [] guid, Set<? extends IpPort> proxies) {
		this(guid,proxies,PLAIN,0);
	}
	
	/**
	 * creates a PushEndpoint without any proxies.  
	 * not very useful but can happen.
	 */
	public PushEndpoint(byte [] guid) {
		this(guid, IpPort.EMPTY_SET);
	}
	
	/**
	 * creates a PushEndpoint from a String passed in http header exchange.
	 */
	public PushEndpoint(String httpString) throws IOException {
	    if (httpString.length() < 32 ||
	            httpString.indexOf(";") > 32)
	        throw new IOException("http string does not contain valid guid");
		
		//the first token is the guid
		String guidS=httpString.substring(0,32);
		httpString = httpString.substring(32);
		
		try {
		    _clientGUID = GUID.fromHexString(guidS);
        } catch(IllegalArgumentException iae) {
            throw new IOException(iae.getMessage());
        }
		_guid = new GUID(_clientGUID);
		
		StringTokenizer tok = new StringTokenizer(httpString,";");
		
		Set<IpPort> proxies = new IpPortSet();
		
		int fwtVersion =0;
		
		IpPort addr = null;
        BitNumbers tlsProxies = null;
		
		while(tok.hasMoreTokens()) {
			String current = tok.nextToken().trim();
			
			// see if this token is the fwt header
			// if this token fails to parse we abort since we must know
			// if the PE supports fwt or not. 
			if (current.startsWith(HTTPConstants.FW_TRANSFER)) {
			    fwtVersion = (int) HTTPUtils.parseFeatureToken(current);
				continue;
			}
            
            // don't parse it in the middle of parsing proxies.
            if (proxies.size() == 0 && current.startsWith(PPTLS_HTTP)) {
                String value = HTTPUtils.parseValue(current);
                if(value != null) {
                    try {
                        tlsProxies = new BitNumbers(value);
                    } catch(IllegalArgumentException invalid) {
                        throw (IOException)new IOException().initCause(invalid);
                    }
                }
                continue;
            }

            // Only look for more proxies if we didn't reach our limit
            if(proxies.size() < MAX_PROXIES) {
                boolean tlsCapable = tlsProxies != null && tlsProxies.isSet(proxies.size());
    			// if its not the header, try to parse it as a push proxy
    			try {
    			    proxies.add(parseIpPort(current, tlsCapable));
    			    continue;
    			} catch(IOException ohWell) {
    			    tlsProxies = null; // stop adding TLS, since our index may be off
                }
            }
			
			// if its not a push proxy, try to parse it as a port:ip
			// only the first occurence of port:ip is parsed
			if (addr==null) {
			    try {
			        addr = parsePortIp(current);
			    }catch(IOException notBad) {}
			}
			
		}
		
		_proxies = Collections.unmodifiableSet(proxies);
		_externalAddr=addr;
		_fwtVersion=fwtVersion;
		
		// its ok to use the _proxies and _size fields directly since altlocs created
		// from http string do not need to change
		_features = proxies.size() | (_fwtVersion << 3);
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
	
    /**
     * Constructs a PushEndpoint from binary representation
     */
    public static PushEndpoint fromBytes(DataInputStream dais) 
      throws BadPacketException, IOException {
        byte [] guid =new byte[16];
        Set<IpPort> proxies = new IpPortSet(); 
        IpPort addr = null;
        
        byte header = (byte)(dais.read() & 0xFF);
        
        // get the number of push proxies
        byte number = (byte)(header & SIZE_MASK); 
        byte features = (byte)(header & FEATURES_MASK);
        byte version = (byte)((header & FWT_VERSION_MASK) >> 3);
        
        dais.readFully(guid);
        
        if (version > 0) {
            byte [] host = new byte[6];
            dais.readFully(host);
            try {
                addr = IPPortCombo.getCombo(host);
            } catch(InvalidDataException ide) {
                throw new BadPacketException(ide);
            }
            if (addr.getAddress().equals(RemoteFileDesc.BOGUS_IP)) {
                addr = null;
                version = 0;
            }
        }
        
        // If the features mentioned this has pptls bytes, read that.
        BitNumbers bn = null;
        if((features & PPTLS_BINARY) != 0) {
            byte[] tlsIndexes = new byte[1];
            dais.readFully(tlsIndexes);
            bn = new BitNumbers(tlsIndexes);
        }   
        
        byte [] tmp = new byte[6];
        for (int i = 0; i < number; i++) {
            dais.readFully(tmp);
            try {
                IpPort ipp = IPPortCombo.getCombo(tmp);
                if(bn != null && bn.isSet(i))
                    ipp = new ConnectableImpl(ipp, true);
                proxies.add(ipp);
            } catch(InvalidDataException ide) {
                throw new BadPacketException(ide);
            }
        }
        
        /** this adds the read set to the existing proxies */
        PushEndpoint pe = new PushEndpoint(guid,proxies,features,version,addr);
        pe.updateProxies(true);
        return pe;
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

	    GuidSetWrapper current = GUID_PROXY_MAP.get(_guid);

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
		GuidSetWrapper current = GUID_PROXY_MAP.get(_guid);
		int currentVersion = current == null ? 
				_fwtVersion : current.getFWTVersion();
		return currentVersion;
	}
	
	/**
	 * Sets the fwt version supported for all PEs pointing to the
	 * given client guid.
	 */
	public static void setFWTVersionSupported(byte[] guid,int version){
		GUID g = new GUID(guid);
		GuidSetWrapper current = GUID_PROXY_MAP.get(g);
		if (current!=null)
			current.setFWTVersion(version);
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
		GuidSetWrapper current = GUID_PROXY_MAP.get(_guid);
		int currentFeatures = current==null ? _features : current.getFeatures();
		return (byte)(currentFeatures & FEATURES_MASK);
	}

	/**
	 * updates the features of all PushEndpoints for the given guid 
	 */
	public static void setFeatures(byte [] guid,int features) {
		GUID g = new GUID(guid);
		GuidSetWrapper current = GUID_PROXY_MAP.get(g);
		if (current!=null)
			current.setFeatures(features);
	}
	
    /**
     * updates the external address of all PushEndpoints for the given guid
     */
    public static void setAddr(byte [] guid, IpPort addr) {
        GUID g = new GUID(guid);
        GuidSetWrapper current = GUID_PROXY_MAP.get(g);
        if (current!=null)
            current.setIpPort(addr);
    }
    
    private IpPort getIpPort() {
        GuidSetWrapper current = GUID_PROXY_MAP.get(_guid);
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.IpPort#getInetAddress()
     */
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
	    GuidSetWrapper existing;
	    GUID guidRef = null;

	    synchronized(GUID_PROXY_MAP) {
	        existing = GUID_PROXY_MAP.get(_guid);

	        // try to get a hard ref so that the mapping won't expire
	        if (existing!=null)
	            guidRef=existing.getGuid();	        

	        // if we do not have a mapping for this guid, or it just expired,
	        // add a new one atomically
			// (we don't care about the proxies of the expired mapping)
	        if (existing == null || guidRef==null){
	        	
	            existing = new GuidSetWrapper(_guid,_features,_fwtVersion);
	            if (good)
	                existing.updateProxies(_proxies,true);
	            else
	                existing.updateProxies(IpPort.EMPTY_SET,true);
	            
	            GUID_PROXY_MAP.put(_guid,existing);
	            
	            // clear the reference to the set
	            _proxies=null;
	            return;
	        }
	    }
	    
	    // if we got here, means we did have a mapping.  no need to
	    // hold the map mutex when updating just the set
	    existing.updateProxies(_proxies,good);
	    
	    // make sure the PE points to the actual key guid
	    _guid = guidRef;
	    _proxies = null;
	}
    
    public PushEndpoint createClone() {
        return new PushEndpoint(_guid.bytes(), 
                getProxies(),
                getFeatures(),
                supportsFWTVersion(), 
                getIpPort());
    }
	
	/**
	 * Overwrites the current known push proxies for the host specified
	 * by the GUID, using the the proxies as written in the httpString.
	 * 
	 * @param guid the guid whose proxies to overwrite
	 * @param httpString comma-separated list of proxies and possible proxy features
	 */
	public static void overwriteProxies(byte [] guid, String httpString) {
	    Set<IpPort> newSet = new HashSet<IpPort>();
	    StringTokenizer tok = new StringTokenizer(httpString,",");
        BitNumbers tlsProxies = null;
	    while(tok.hasMoreTokens()) {
	        String proxy = tok.nextToken().trim();
            // only read features when we haven't read proxies yet.
            if(newSet.size() == 0 && proxy.startsWith(PPTLS_HTTP)) {
                try {
                    String value = HTTPUtils.parseValue(proxy);
                    if(value != null) {
                        try {
                            tlsProxies = new BitNumbers(value);
                        } catch(IllegalArgumentException ignored) {}
                    }
                } catch(IOException invalid) {}
                continue;
            }
            
            boolean tlsCapable = tlsProxies != null && tlsProxies.isSet(newSet.size());
	        try {
	            newSet.add(parseIpPort(proxy, tlsCapable));
	        } catch(IOException ohWell){
	            tlsProxies = null; // unset, since index may be off.
            }
	    }

        overwriteProxies(guid, newSet);
    }

    /**
     * Overwrites any stored proxies for the host specified by the guid.
     * 
     * @param guid the guid whose proxies to overwrite
     * @param newSet the proxies to overwrite with
     */
    public static void overwriteProxies(byte[] guid, Set<? extends IpPort> newSet) {
	    GUID g = new GUID(guid);
	    GuidSetWrapper wrapper;
	    synchronized(GUID_PROXY_MAP) {
	        wrapper = GUID_PROXY_MAP.get(g);
	        if (wrapper==null) {
	            wrapper = new GuidSetWrapper(g);
	            GUID_PROXY_MAP.put(g, wrapper);
            }
	        wrapper.overwriteProxies(newSet);
        }
	}

	/**
	 * 
	 * @param http a string representing an ip and port
	 * @return an object implementing PushProxyInterface 
	 * @throws IOException parsing failed.
	 */
	private static IpPort parseIpPort(String http, boolean tlsCapable) throws IOException {
	    int separator = http.indexOf(":");
		
		//see if this is a valid ip:port address; 
		if (separator == -1 || separator!= http.lastIndexOf(":") || separator == http.length())
			throw new IOException("invalid separator in http: " + http);
			
		String host = http.substring(0,separator);
		
		if (!NetworkUtils.isValidAddress(host) || NetworkUtils.isPrivateAddress(host))
		    throw new IOException("invalid addr: " + host);
		
		String portS = http.substring(separator+1);
		
		try {
			int port = Integer.parseInt(portS);
			if(!NetworkUtils.isValidPort(port))
			    throw new IOException("invalid port: " + port);
            if(tlsCapable)
                return new ConnectableImpl(host, port, true);
            else
                return new IpPortImpl(host, port); // could technically also return a ConnectableImpl w/ tls=false
		} catch(NumberFormatException invalid) {
		    throw (IOException)new IOException().initCause(invalid);
		}
	}
	
	/** 
	 * @param http a string representing a port and an ip
	 * @return an object implementing IpPort 
	 * @throws IOException parsing failed.
	 */
	private static IpPort parsePortIp(String http) throws IOException{
	    int separator = http.indexOf(":");
		
		//see if this is a valid ip:port address; 
		if (separator == -1 || separator!= http.lastIndexOf(":") ||
				separator == http.length())
			throw new IOException();
		
		String portS = http.substring(0,separator);
		int port =0;
		
		try {
			port = Integer.parseInt(portS);
			if(!NetworkUtils.isValidPort(port))
			    throw new IOException();
		}catch(NumberFormatException failed) {
		    throw new IOException(failed.getMessage());
		}
		
		String host = http.substring(separator+1);
		
		if (!NetworkUtils.isValidAddress(host) || NetworkUtils.isPrivateAddress(host))
		    throw new IOException();
		
		return new IpPortImpl(host,port);
	}
	
	private static class GuidSetWrapper {
	    private final WeakReference<GUID> _guidRef;
	    private Set<IpPort> _proxies;
	    private int _features,_fwtVersion;
        private IpPort _externalAddr;
	    
	    GuidSetWrapper(GUID guid) {
	    	this(guid,0,0);
	    }
	    
	    GuidSetWrapper(GUID guid,int features, int version) {
	        _guidRef = new WeakReference<GUID>(guid);
	        _features=features;
	        _fwtVersion=version;
	    }
	    
	    synchronized void updateProxies(Set<? extends IpPort> s, boolean add){
	        Set<IpPort> existing = new IpPortSet();
            
	        if (s == null)
                s = _proxies;
            
	        if (_proxies!=null)
	            existing.addAll(_proxies);
	        
	        if (add)
	            existing.addAll(s);
	        else
	            existing.removeAll(s);
	        
	        overwriteProxies(existing);
	    }
	    
	    synchronized void overwriteProxies(Set<? extends IpPort> s) {
	        _proxies = Collections.unmodifiableSet(s);
	    }
	    
	    synchronized Set<IpPort> getProxies() {
	        return _proxies != null ? _proxies : IpPort.EMPTY_SET;
	    }
	    
	    synchronized int getFeatures() {
	    	return _features;
	    }
	    
	    synchronized int getFWTVersion() {
	    	return _fwtVersion;
	    }
	    
	    synchronized void setFeatures(int features) {
	    	_features=features;
	    }
	    
	    synchronized void setFWTVersion(int version){
	    	_fwtVersion=version;
	    }
        
        synchronized void setIpPort(IpPort addr) {
            _externalAddr = addr;
        }
        
        synchronized IpPort getIpPort() {
            return _externalAddr;
        }
	    
	    GUID getGuid() {
	        return _guidRef.get();
	    }
	}
    
    private static final class WeakCleaner implements Runnable {
        public void run() {
            GUID_PROXY_MAP.size();
        }
    }
	
}
