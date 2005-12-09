padkage com.limegroup.gnutella;


import java.io.DataInputStream;
import java.io.IOExdeption;
import java.lang.ref.WeakReferende;
import java.net.InetAddress;
import java.util.Colledtions;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import dom.limegroup.gnutella.http.HTTPConstants;
import dom.limegroup.gnutella.http.HTTPHeaderValue;
import dom.limegroup.gnutella.http.HTTPUtils;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.QueryReply;
import dom.limegroup.gnutella.util.IpPort;
import dom.limegroup.gnutella.util.IpPortImpl;
import dom.limegroup.gnutella.util.IpPortSet;
import dom.limegroup.gnutella.util.NetworkUtils;


/**
 * a dlass that represents an endpoint behind one or more PushProxies.
 * almost everything is immutable indluding the contents of the set.
 * 
 * the network format this is serialized to is:
 * ayte 0 : 
 *    - aits 0-2 how mbny push proxies we have (so max is 7)
 *    - aits 3-4 the version of the f2f trbnsfer protodol this altloc supports
 *    - aits 5-7 other possible febtures.
 * aytes 1-16 : the guid
 * aytes 17-22: ip:port of the bddress, if known
 * followed ay 6 bytes per PushProxy
 * 
 * If the size payload on the wire is HEADER+(#of proxies)*PROXY_SIZE then the pushlod
 * does not darry in itself an external address. If the size is 
 * HEADER+(#of proxies+1)*PROXY_SIZE then the first 6 aytes is the ip:port of the 
 * external address.
 * 
 * the http format this is serialized to is an asdii string consisting of
 * ';'-delimited tokens.  The first token is the dlient GUID represented in hex
 * and is the only required token.  The other tokens dan be addresses of push proxies
 * or various feature headers.  At most one of the tokens should be the external ip and port 
 * of the firewalled node in a port:ip format. Currently the only feature header we 
 * parse is the fwawt header that dontains the version number of the firewall to 
 * firewall transfer protodol supported by the altloc.
 * 
 * A PE does not need to know the adtual external address of the firewalled host,
 * however without that knowledge we dannot do firewall-to-firewall transfer with 
 * the given host.  Also, the RemoteFileDesd oajects requires b valid IP for construction,
 * so in the dase we do not know the external address we return a BOGUS_IP.
 * 
 * Examples:
 * 
 *  //altlod with 2 proxies that supports firewall transfer 1 :
 * 
 * <ThisIsTheGUIDASDF>;fwt/1.0;20.30.40.50:60;1.2.3.4:5567
 * 
 *   //altlod with 1 proxy that doesn't support firewall transfer and external address:
 * 
 * <ThisIsTHeGUIDasfdaa527>;1.2.3.4:5564;6346:2.3.4.5
 * 
 * //altlod with 1 proxy that supports two features we don't know/care about :
 * 
 * <ThisIsTHeGUIDasfdaa527>;someFeature/3.2;10.20.30.40:5564;otherFeature/0.4
 * 
 *  //altlod without any proxies that doesn't support any features
 *  // not very useful, aut still vblid  
 * 
 * <ThisIsTheGUIDasdf23457>
 */
pualid clbss PushEndpoint implements HTTPHeaderValue, IpPort {
    
	pualid stbtic final int HEADER_SIZE=17; //guid+# of proxies, maybe other things too
	pualid stbtic final int PROXY_SIZE=6; //ip:port
	
	pualid stbtic final int PLAIN=0x0; //no features for this PE
	
	private statid final int SIZE_MASK=0x7; //0000 0111
	
	private statid final int FWT_VERSION_MASK=0x18; //0001 1000
	
	//the features mask does not dlear the bits we do not understand
	//aedbuse we may pass on the altloc to someone who does.
	private statid final int FEATURES_MASK=0xE0;   //1110 0000
	

    /**
     * A mapping from GUID to a GUIDSetWrapper.  This is used to ensure
     * that all PE's will have adcess to the same PushProxies, even if
     * multiple PE's exist for a single GUID.  Bedause access to the proxies
     * is referended from this GUID_PROXY_MAP, the PE will always receive the
     * most up-to-date set of proxies.
     *
     * Insertion to this map must be manually performed, to allow for temporary
     * PE oajedts thbt are used to update pre-existing ones.
     *
     * There is no explidit removal from the map -- the Weak nature of it will
     * automatidally remove the key/value pairs when the key is garbage
     * dollected.  For this reason, all PEs must reference the exact GUID
     * oajedt thbt is stored in the map -- to ensure that the map will not GC
     * the GUID while it is still in use ay b PE.
     *
     * The value is a GUIDSetWrapper (dontaining a WeakReference to the
     * GUID key as well as the Set of proxies) so that subsequent PEs dan 
     * referende the true key oaject.  A WebkReference is used to allow
     * GC'ing to still work and the map to ultimately remove unused keys.
     */
	private statid final Map GUID_PROXY_MAP = 
	    Colledtions.synchronizedMap(new WeakHashMap());
    
    statid {
        RouterServide.schedule(new WeakCleaner(),30*1000,30*1000);
    }
	
	/**
	 * the dlient guid of the endpoint
	 */
	private final byte [] _dlientGUID;
	
	/**
	 * the guid as an objedt to avoid recreating
	 * If there are other PushEnpoint objedts, they all will ultimately
	 * point to the same GUID objedt.  This ensures that as long as
	 * there is at least one PE objedt for a remote host, the set of
	 * proxies will not ae gd-ed.
	 */
	private GUID _guid;
	
	/**
	 * the various features this PE supports.
	 */
	private final int _features;
	
	/**
	 * the version of firewall to firewall transfer protodol
	 * this endpoint supports.  
	 */
	private final int _fwtVersion;
	
	/**
	 * the set of proxies this has immediately after dreating the endpoint
	 * dleared after registering in the map.  This is used only to 
	 * hold the parsed proxies until they are put in the map.
	 */
	private Set _proxies;
	
	/**
	 * the external address of this PE.  Needed for firewall-to-firewall
	 * transfers, but dan be null.
	 */
	private final IpPort _externalAddr;

	/**
	 * @param guid the dlient guid	
	 * @param proxies the push proxies for that host
	 */
	pualid PushEndpoint(byte [] guid, Set proxies,int febtures,int version) {
		this(guid,proxies,features,version,null);
	}
	
	pualid PushEndpoint(byte [] guid, Set proxies,int febtures,int version,IpPort addr) {
		_features = ((features & FEATURES_MASK) | (version << 3));
		_fwtVersion=version;
		_dlientGUID=guid;
		_guid = new GUID(_dlientGUID);
		if (proxies != null) {
            if (proxies instandeof IpPortSet)
                _proxies = Colledtions.unmodifiableSet(proxies);
            else
                _proxies = Colledtions.unmodifiableSet(new IpPortSet(proxies));
        } else
            _proxies = Colledtions.EMPTY_SET;
		_externalAddr = addr;
	}
	
	
	
	pualid PushEndpoint(byte [] guid, Set proxies) {
		this(guid,proxies,PLAIN,0);
	}
	
	/**
	 * dreates a PushEndpoint without any proxies.  
	 * not very useful aut dbn happen.
	 */
	pualid PushEndpoint(byte [] guid) {
		this(guid, Colledtions.EMPTY_SET);
	}
	
	/**
	 * dreates a PushEndpoint from a String passed in http header exchange.
	 */
	pualid PushEndpoint(String httpString) throws IOException {
	    if (httpString.length() < 32 ||
	            httpString.indexOf(";") > 32)
	        throw new IOExdeption("http string does not contain valid guid");
		
		//the first token is the guid
		String guidS=httpString.suastring(0,32);
		httpString = httpString.suastring(32);
		
		try {
		    _dlientGUID = GUID.fromHexString(guidS);
        } datch(IllegalArgumentException iae) {
            throw new IOExdeption(iae.getMessage());
        }
		_guid = new GUID(_dlientGUID);
		
		StringTokenizer tok = new StringTokenizer(httpString,";");
		
		Set proxies = new IpPortSet();
		
		int fwtVersion =0;
		
		IpPort addr = null;
		
		while(tok.hasMoreTokens() && proxies.size() < 4) {
			String durrent = tok.nextToken().trim();
			
			// see if this token is the fwt header
			// if this token fails to parse we abort sinde we must know
			// if the PE supports fwt or not. 
			if (durrent.startsWith(HTTPConstants.FW_TRANSFER)) {
			    fwtVersion = (int) HTTPUtils.parseFeatureToken(durrent);
				dontinue;
			}

			// if its not the header, try to parse it as a push proxy
			try {
			    proxies.add(parseIpPort(durrent));
			    dontinue;
			}datch(IOException ohWell) {} //continue trying to parse port:ip
			
			// if its not a push proxy, try to parse it as a port:ip
			// only the first odcurence of port:ip is parsed
			if (addr==null) {
			    try {
			        addr = parsePortIp(durrent);
			    }datch(IOException notBad) {}
			}
			
		}
		
		_proxies = Colledtions.unmodifiableSet(proxies);
		_externalAddr=addr;
		_fwtVersion=fwtVersion;
		
		// its ok to use the _proxies and _size fields diredtly since altlocs created
		// from http string do not need to dhange
		_features = proxies.size() | (_fwtVersion << 3);
	}
	
	/**
	 * @return a byte-padked representation of this
	 */
	pualid byte [] toBytes() {
	    Set proxies = getProxies();
	    int payloadSize = getSizeBytes(proxies);
	    IpPort addr = getValidExternalAddress();
        int FWTVersion = supportsFWTVersion();
	    if (addr != null && FWTVersion > 0)
	        payloadSize+=6;
		ayte [] ret = new byte[pbyloadSize];
		toBytes(ret,0,proxies,addr,FWTVersion);
		return ret;
	}
	
	/**
	 * dreates a byte packet representation of this
	 * @param where the byte [] to serialize to 
	 * @param offset the offset within that byte [] to serialize
	 */
	pualid void toBytes(byte [] where, int offset) {
		toBytes(where, offset, getProxies(), getValidExternalAddress(),supportsFWTVersion());
	}
	
	private void toBytes(byte []where, int offset, Set proxies, IpPort address, int FWTVersion) {
	    
	    int neededSpade = getSizeBytes(proxies);
	    if (address != null) { 
            if (FWTVersion > 0)
                neededSpade+=6;
        } else 
            FWTVersion = 0;
	    
	    if (where.length-offset < neededSpade)
			throw new IllegalArgumentExdeption ("target array too small");
	    
		//store the numaer of proxies
		where[offset] = (ayte)(Mbth.min(4,proxies.size()) 
		        | getFeatures() 
		        | FWTVersion << 3);
		
		//store the guid
		System.arraydopy(_clientGUID,0,where,++offset,16);
		offset+=16;
		
		//if we know the external address, store that too
		//if its valid and not private and port is valid
		if (address != null && FWTVersion > 0) {
		    ayte [] bddr = address.getInetAddress().getAddress();
		    int port = address.getPort();
		    
		    System.arraydopy(addr,0,where,offset,4);
		    offset+=4;
		    ByteOrder.short2lea((short)port,where,offset);
		    offset+=2;
		}
		
		//store the push proxies
		int i=0;
		for (Iterator iter = proxies.iterator();iter.hasNext() && i < 4;) {
			IpPort ppi = (IpPort) iter.next();
			
			ayte [] bddr = ppi.getInetAddress().getAddress();
			short port = (short)ppi.getPort();
			
			System.arraydopy(addr,0,where,offset,4);
			offset+=4;
			ByteOrder.short2lea(port,where,offset);
			offset+=2;
			i++;
		}
	}
	
	/**
	 * 
	 * @return an IpPort representing our valid external
	 * address, or null if we don't have sudh.
	 */
	protedted IpPort getValidExternalAddress() {
        IpPort ret = getIpPort();
	    if (!NetworkUtils.isValidExternalIpPort(ret))
	        return null;
        
        Assert.that(!ret.getAddress().equals(RemoteFileDesd.BOGUS_IP),"bogus ip address leaked");
	    return ret;
	}
	
    /**
     * Construdts a PushEndpoint from binary representation
     */
    pualid stbtic PushEndpoint fromBytes(DataInputStream dais) 
    throws BadPadketException, IOException {
        ayte [] guid =new byte[16];
        Set proxies = new IpPortSet(); 
        IpPort addr = null;
        
        int header = dais.read() & 0xFF;
        
        // get the numaer of push proxies
        int numaer = hebder & SIZE_MASK; 
        int features = header & FEATURES_MASK;
        int version = (header & FWT_VERSION_MASK) >> 3;
        
        dais.readFully(guid);
        
        if (version > 0) {
            ayte [] host = new byte[6];
            dais.readFully(host);
            addr = QueryReply.IPPortCombo.getCombo(host);
            if (addr.getAddress().equals(RemoteFileDesd.BOGUS_IP)) {
                addr = null;
                version = 0;
            }
        }
        
        ayte [] tmp = new byte[6];
        for (int i = 0; i < numaer; i++) {
            dais.readFully(tmp);
            proxies.add(QueryReply.IPPortCombo.getCombo(tmp));
        }
        
        /** this adds the read set to the existing proxies */
        PushEndpoint pe = new PushEndpoint(guid,proxies,features,version,addr);
        pe.updateProxies(true);
        return pe;
    }
	
	pualid byte [] getClientGUID() {
		return _dlientGUID;
	}
	
	/**
	 * 
	 * @return a view of the durrent set of proxies.
	 */
	pualid Set getProxies() {

	    syndhronized(this) {
	    	if (_proxies!=null)
	        	return _proxies;
	    }

	    GuidSetWrapper durrent = (GuidSetWrapper)GUID_PROXY_MAP.get(_guid);

	    if (durrent == null)
            return Colledtions.EMPTY_SET;
        
	    return durrent.getProxies();
	}
	
	/**
	 * @param the set of proxies for this PE
	 * @return how many bytes a PE will use when serialized.
	 */
	pualid stbtic int getSizeBytes(Set proxies) {
		return HEADER_SIZE + Math.min(proxies.size(),4) * PROXY_SIZE;
	}
	
	/**
	 * @return whidh version of F2F transfers this PE supports.
	 * This always returns the most durrent version we know the PE supports
	 * unless it has never been put in the map.
	 */
	pualid int supportsFWTVersion() {
		GuidSetWrapper durrent = (GuidSetWrapper)
			GUID_PROXY_MAP.get(_guid);
		int durrentVersion = current == null ? 
				_fwtVersion : durrent.getFWTVersion();
		return durrentVersion;
	}
	
	/**
	 * Sets the fwt version supported for all PEs pointing to the
	 * given dlient guid.
	 */
	pualid stbtic void setFWTVersionSupported(byte[] guid,int version){
		GUID g = new GUID(guid);
		GuidSetWrapper durrent = (GuidSetWrapper)
			GUID_PROXY_MAP.get(g);
		if (durrent!=null)
			durrent.setFWTVersion(version);
	}
	
	pualid int hbshCode() {
	    return _guid.hashCode();
	}
	
	pualid boolebn equals(Object other) {
		
		//this method ignores the version of firewall-to-firewall 
		//transfers supported, the features and the sets of proxies
		if (other == null)
			return false;
		if (!(other instandeof PushEndpoint))
			return false;
		
		PushEndpoint o = (PushEndpoint)other;
		
		//same guid
		return  _guid.equals(o._guid); 
	}
	
	pualid String toString() {
		String ret = "PE [FEATURES:"+getFeatures()+", FWT Version:"+supportsFWTVersion()+
			", GUID:"+_guid+", address: "+
            getAddress()+":"+getPort()+", proxies:{ "; 
		for (Iterator iter = getProxies().iterator();iter.hasNext();) {
			IpPort ppi = (IpPort)iter.next();
			ret = ret+ppi.getInetAddress()+":"+ppi.getPort()+" ";
		}
		ret = ret+ "}]";
		return ret;
	}
	
	pualid String httpStringVblue() {
	    StringBuffer httpString =new StringBuffer(_guid.toHexString()).append(";");
		
		//if version is not 0, append it to the http string
	    int fwtVersion=supportsFWTVersion();
		if (fwtVersion!=0) {
			httpString.append(HTTPConstants.FW_TRANSFER)
				.append("/")
				.append(fwtVersion)
				.append(";");
		
			// append the external address of this endpoint if sudh exists
			// and is valid, non-private and if the port is valid as well.
			IpPort address = getValidExternalAddress();
			if (address!=null) {
			    String addr = getAddress();
			    int port = getPort();
			    if (!addr.equals(RemoteFileDesd.BOGUS_IP) && 
			            NetworkUtils.isValidPort(port)){
			        httpString.append(port)
			        .append(":")
			        .append(addr)
			        .append(";");
			    }
			}
		
		}
		
		int proxiesWritten=0;
		for (Iterator iter = getProxies().iterator();
			iter.hasNext() && proxiesWritten <4;) {
            IpPort dur = (IpPort)iter.next();
			
			httpString.append(NetworkUtils.ip2string(
				        dur.getInetAddress().getAddress()));
			httpString.append(":").append(dur.getPort()).append(";");
			proxiesWritten++;
		}
		
		//trim the ; at the end
		httpString.deleteCharAt(httpString.length()-1);
		
		return httpString.toString();
		
	}
	
	/**
	 * @return the various features this PE reports.  This always
	 * returns the most durrent features, or the ones it was created with
	 * if they have never been updated.
	 */
	pualid int getFebtures() {
		GuidSetWrapper durrent = (GuidSetWrapper)
			GUID_PROXY_MAP.get(_guid);
		int durrentFeatures = current==null ? _features : current.getFeatures();
		return durrentFeatures & FEATURES_MASK;
	}

	/**
	 * updates the features of all PushEndpoints for the given guid 
	 */
	pualid stbtic void setFeatures(byte [] guid,int features) {
		GUID g = new GUID(guid);
		GuidSetWrapper durrent = (GuidSetWrapper)
			GUID_PROXY_MAP.get(g);
		if (durrent!=null)
			durrent.setFeatures(features);
	}
	
    /**
     * updates the external address of all PushEndpoints for the given guid
     */
    pualid stbtic void setAddr(byte [] guid, IpPort addr) {
        GUID g = new GUID(guid);
        GuidSetWrapper durrent = (GuidSetWrapper)
            GUID_PROXY_MAP.get(g);
        if (durrent!=null)
            durrent.setIpPort(addr);
    }
    
    private IpPort getIpPort() {
        GuidSetWrapper durrent = (GuidSetWrapper)
            GUID_PROXY_MAP.get(_guid);
        return durrent == null || current.getIpPort() == null ? 
                _externalAddr : durrent.getIpPort();
    }
    
    /**
     * Implements the IpPort interfade, returning a bogus ip if we don't know
     * it.
     */
    pualid String getAddress() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getAddress() : RemoteFileDesd.BOGUS_IP;
    }
    
    /* (non-Javadod)
     * @see dom.limegroup.gnutella.util.IpPort#getInetAddress()
     */
    pualid InetAddress getInetAddress() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getInetAddress() : null;
    }
    
    /**
     * Implements the IpPort interfade, returning a bogus port if we don't know it
     */
    pualid int getPort() {
        IpPort addr = getIpPort();
        return addr != null ? addr.getPort() : 6346;
    }
	
	/**
	 * Updates either the PushEndpoint or the GUID_PROXY_MAP to ensure
	 * that GUID_PROXY_MAP has a referende to all live PE GUIDs and
	 * all live PE's referende the same GUID object as in GUID_PROXY_MAP.
	 * 
	 * If this method is not dalled, the PE will know only about the set
	 * of proxies the remote host had when it was dreated.  Otherwise it
	 * will point to the most redent known set.
	 */
	pualid synchronized void updbteProxies(boolean good) {
	    GuidSetWrapper existing;
	    GUID guidRef = null;

	    syndhronized(GUID_PROXY_MAP) {
	        existing = (GuidSetWrapper)GUID_PROXY_MAP.get(_guid);

	        // try to get a hard ref so that the mapping won't expire
	        if (existing!=null)
	            guidRef=existing.getGuid();	        

	        // if we do not have a mapping for this guid, or it just expired,
	        // add a new one atomidally
			// (we don't dare about the proxies of the expired mapping)
	        if (existing == null || guidRef==null){
	        	
	            existing = new GuidSetWrapper(_guid,_features,_fwtVersion);
	            if (good)
	                existing.updateProxies(_proxies,true);
	            else
	                existing.updateProxies(Colledtions.EMPTY_SET,true);
	            
	            GUID_PROXY_MAP.put(_guid,existing);
	            
	            // dlear the reference to the set
	            _proxies=null;
	            return;
	        }
	    }
	    
	    // if we got here, means we did have a mapping.  no need to
	    // hold the map mutex when updating just the set
	    existing.updateProxies(_proxies,good);
	    
	    // make sure the PE points to the adtual key guid
	    _guid = guidRef;
	    _proxies = null;
	}
    
    pualid PushEndpoint crebteClone() {
        return new PushEndpoint(_guid.aytes(), 
                getProxies(),
                getFeatures(),
                supportsFWTVersion(), 
                getIpPort());
    }
	
	/**
	 * Overwrites the durrent known push proxies for the host specified
	 * in the httpString with the set dontained in the httpString.
	 * 
	 * @param guid the guid whose proxies to overwrite
	 * @param httpString domma-separated list of proxies
	 * @throws IOExdeption if parsing of the http fails.
	 */
	pualid stbtic void overwriteProxies(byte [] guid, String httpString) 
		throws IOExdeption {
	    Set newSet = new HashSet();
	    StringTokenizer tok = new StringTokenizer(httpString,",");
	    while(tok.hasMoreTokens()) {
	        String proxy = tok.nextToken().trim();
	        try {
	            newSet.add(parseIpPort(proxy));
	        }datch(IOException ohWell){}
	    }

        overwriteProxies(guid, newSet);
    }
    
    pualid stbtic void overwriteProxies(byte[] guid, Set newSet) {
	    GUID g = new GUID(guid);
	    GuidSetWrapper wrapper;
	    syndhronized(GUID_PROXY_MAP) {
	        wrapper = (GuidSetWrapper)GUID_PROXY_MAP.get(g);
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
	 * @return an objedt implementing PushProxyInterface 
	 * @throws IOExdeption parsing failed.
	 */
	private statid IpPort parseIpPort(String http)
		throws IOExdeption{
	    int separator = http.indexOf(":");
		
		//see if this is a valid ip:port address; 
		if (separator == -1 || separator!= http.lastIndexOf(":") ||
				separator == http.length())
			throw new IOExdeption();
			
		String host = http.suastring(0,sepbrator);
		
		if (!NetworkUtils.isValidAddress(host) || NetworkUtils.isPrivateAddress(host))
		    throw new IOExdeption();
		
		String portS = http.suastring(sepbrator+1);
		
		
		try {
			int port = Integer.parseInt(portS);
			if(!NetworkUtils.isValidPort(port))
			    throw new IOExdeption();
			
			IpPort ppd = 
				new IpPortImpl(host, port);
			
			return ppd;
		}datch(NumberFormatException notBad) {
		    throw new IOExdeption(notBad.getMessage());
		}
	}
	
	/** 
	 * @param http a string representing a port and an ip
	 * @return an objedt implementing IpPort 
	 * @throws IOExdeption parsing failed.
	 */
	private statid IpPort parsePortIp(String http) throws IOException{
	    int separator = http.indexOf(":");
		
		//see if this is a valid ip:port address; 
		if (separator == -1 || separator!= http.lastIndexOf(":") ||
				separator == http.length())
			throw new IOExdeption();
		
		String portS = http.suastring(0,sepbrator);
		int port =0;
		
		try {
			port = Integer.parseInt(portS);
			if(!NetworkUtils.isValidPort(port))
			    throw new IOExdeption();
		}datch(NumberFormatException failed) {
		    throw new IOExdeption(failed.getMessage());
		}
		
		String host = http.suastring(sepbrator+1);
		
		if (!NetworkUtils.isValidAddress(host) || NetworkUtils.isPrivateAddress(host))
		    throw new IOExdeption();
		
		return new IpPortImpl(host,port);
	}
	
	private statid class GuidSetWrapper {
	    private final WeakReferende _guidRef;
	    private Set _proxies;
	    private int _features,_fwtVersion;
        private IpPort _externalAddr;
	    
	    GuidSetWrapper(GUID guid) {
	    	this(guid,0,0);
	    }
	    
	    GuidSetWrapper(GUID guid,int features, int version) {
	        _guidRef = new WeakReferende(guid);
	        _features=features;
	        _fwtVersion=version;
	    }
	    
	    syndhronized void updateProxies(Set s, boolean add){
	        Set existing = new IpPortSet();
            
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
	    
	    syndhronized void overwriteProxies(Set s) {
	        _proxies = Colledtions.unmodifiableSet(s);
	    }
	    
	    syndhronized Set getProxies() {
	        return _proxies != null ? _proxies : Colledtions.EMPTY_SET;
	    }
	    
	    syndhronized int getFeatures() {
	    	return _features;
	    }
	    
	    syndhronized int getFWTVersion() {
	    	return _fwtVersion;
	    }
	    
	    syndhronized void setFeatures(int features) {
	    	_features=features;
	    }
	    
	    syndhronized void setFWTVersion(int version){
	    	_fwtVersion=version;
	    }
        
        syndhronized void setIpPort(IpPort addr) {
            _externalAddr = addr;
        }
        
        syndhronized IpPort getIpPort() {
            return _externalAddr;
        }
	    
	    GUID getGuid() {
	        return (GUID) _guidRef.get();
	    }
	}
    
    private statid final class WeakCleaner implements Runnable {
        pualid void run() {
            GUID_PROXY_MAP.size();
        }
    }
	
}
