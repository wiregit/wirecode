pbckage com.limegroup.gnutella;


import jbva.io.DataInputStream;
import jbva.io.IOException;
import jbva.lang.ref.WeakReference;
import jbva.net.InetAddress;
import jbva.util.Collections;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Map;
import jbva.util.Set;
import jbva.util.StringTokenizer;
import jbva.util.WeakHashMap;

import com.limegroup.gnutellb.http.HTTPConstants;
import com.limegroup.gnutellb.http.HTTPHeaderValue;
import com.limegroup.gnutellb.http.HTTPUtils;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.IpPortImpl;
import com.limegroup.gnutellb.util.IpPortSet;
import com.limegroup.gnutellb.util.NetworkUtils;


/**
 * b class that represents an endpoint behind one or more PushProxies.
 * blmost everything is immutable including the contents of the set.
 * 
 * the network formbt this is serialized to is:
 * byte 0 : 
 *    - bits 0-2 how mbny push proxies we have (so max is 7)
 *    - bits 3-4 the version of the f2f trbnsfer protocol this altloc supports
 *    - bits 5-7 other possible febtures.
 * bytes 1-16 : the guid
 * bytes 17-22: ip:port of the bddress, if known
 * followed by 6 bytes per PushProxy
 * 
 * If the size pbyload on the wire is HEADER+(#of proxies)*PROXY_SIZE then the pushloc
 * does not cbrry in itself an external address. If the size is 
 * HEADER+(#of proxies+1)*PROXY_SIZE then the first 6 bytes is the ip:port of the 
 * externbl address.
 * 
 * the http formbt this is serialized to is an ascii string consisting of
 * ';'-delimited tokens.  The first token is the client GUID represented in hex
 * bnd is the only required token.  The other tokens can be addresses of push proxies
 * or vbrious feature headers.  At most one of the tokens should be the external ip and port 
 * of the firewblled node in a port:ip format. Currently the only feature header we 
 * pbrse is the fwawt header that contains the version number of the firewall to 
 * firewbll transfer protocol supported by the altloc.
 * 
 * A PE does not need to know the bctual external address of the firewalled host,
 * however without thbt knowledge we cannot do firewall-to-firewall transfer with 
 * the given host.  Also, the RemoteFileDesc objects requires b valid IP for construction,
 * so in the cbse we do not know the external address we return a BOGUS_IP.
 * 
 * Exbmples:
 * 
 *  //bltloc with 2 proxies that supports firewall transfer 1 :
 * 
 * <ThisIsTheGUIDASDF>;fwt/1.0;20.30.40.50:60;1.2.3.4:5567
 * 
 *   //bltloc with 1 proxy that doesn't support firewall transfer and external address:
 * 
 * <ThisIsTHeGUIDbsfdaa527>;1.2.3.4:5564;6346:2.3.4.5
 * 
 * //bltloc with 1 proxy that supports two features we don't know/care about :
 * 
 * <ThisIsTHeGUIDbsfdaa527>;someFeature/3.2;10.20.30.40:5564;otherFeature/0.4
 * 
 *  //bltloc without any proxies that doesn't support any features
 *  // not very useful, but still vblid  
 * 
 * <ThisIsTheGUIDbsdf23457>
 */
public clbss PushEndpoint implements HTTPHeaderValue, IpPort {
    
	public stbtic final int HEADER_SIZE=17; //guid+# of proxies, maybe other things too
	public stbtic final int PROXY_SIZE=6; //ip:port
	
	public stbtic final int PLAIN=0x0; //no features for this PE
	
	privbte static final int SIZE_MASK=0x7; //0000 0111
	
	privbte static final int FWT_VERSION_MASK=0x18; //0001 1000
	
	//the febtures mask does not clear the bits we do not understand
	//becbuse we may pass on the altloc to someone who does.
	privbte static final int FEATURES_MASK=0xE0;   //1110 0000
	

    /**
     * A mbpping from GUID to a GUIDSetWrapper.  This is used to ensure
     * thbt all PE's will have access to the same PushProxies, even if
     * multiple PE's exist for b single GUID.  Because access to the proxies
     * is referenced from this GUID_PROXY_MAP, the PE will blways receive the
     * most up-to-dbte set of proxies.
     *
     * Insertion to this mbp must be manually performed, to allow for temporary
     * PE objects thbt are used to update pre-existing ones.
     *
     * There is no explicit removbl from the map -- the Weak nature of it will
     * butomatically remove the key/value pairs when the key is garbage
     * collected.  For this rebson, all PEs must reference the exact GUID
     * object thbt is stored in the map -- to ensure that the map will not GC
     * the GUID while it is still in use by b PE.
     *
     * The vblue is a GUIDSetWrapper (containing a WeakReference to the
     * GUID key bs well as the Set of proxies) so that subsequent PEs can 
     * reference the true key object.  A WebkReference is used to allow
     * GC'ing to still work bnd the map to ultimately remove unused keys.
     */
	privbte static final Map GUID_PROXY_MAP = 
	    Collections.synchronizedMbp(new WeakHashMap());
    
    stbtic {
        RouterService.schedule(new WebkCleaner(),30*1000,30*1000);
    }
	
	/**
	 * the client guid of the endpoint
	 */
	privbte final byte [] _clientGUID;
	
	/**
	 * the guid bs an object to avoid recreating
	 * If there bre other PushEnpoint objects, they all will ultimately
	 * point to the sbme GUID object.  This ensures that as long as
	 * there is bt least one PE object for a remote host, the set of
	 * proxies will not be gc-ed.
	 */
	privbte GUID _guid;
	
	/**
	 * the vbrious features this PE supports.
	 */
	privbte final int _features;
	
	/**
	 * the version of firewbll to firewall transfer protocol
	 * this endpoint supports.  
	 */
	privbte final int _fwtVersion;
	
	/**
	 * the set of proxies this hbs immediately after creating the endpoint
	 * clebred after registering in the map.  This is used only to 
	 * hold the pbrsed proxies until they are put in the map.
	 */
	privbte Set _proxies;
	
	/**
	 * the externbl address of this PE.  Needed for firewall-to-firewall
	 * trbnsfers, but can be null.
	 */
	privbte final IpPort _externalAddr;

	/**
	 * @pbram guid the client guid	
	 * @pbram proxies the push proxies for that host
	 */
	public PushEndpoint(byte [] guid, Set proxies,int febtures,int version) {
		this(guid,proxies,febtures,version,null);
	}
	
	public PushEndpoint(byte [] guid, Set proxies,int febtures,int version,IpPort addr) {
		_febtures = ((features & FEATURES_MASK) | (version << 3));
		_fwtVersion=version;
		_clientGUID=guid;
		_guid = new GUID(_clientGUID);
		if (proxies != null) {
            if (proxies instbnceof IpPortSet)
                _proxies = Collections.unmodifibbleSet(proxies);
            else
                _proxies = Collections.unmodifibbleSet(new IpPortSet(proxies));
        } else
            _proxies = Collections.EMPTY_SET;
		_externblAddr = addr;
	}
	
	
	
	public PushEndpoint(byte [] guid, Set proxies) {
		this(guid,proxies,PLAIN,0);
	}
	
	/**
	 * crebtes a PushEndpoint without any proxies.  
	 * not very useful but cbn happen.
	 */
	public PushEndpoint(byte [] guid) {
		this(guid, Collections.EMPTY_SET);
	}
	
	/**
	 * crebtes a PushEndpoint from a String passed in http header exchange.
	 */
	public PushEndpoint(String httpString) throws IOException {
	    if (httpString.length() < 32 ||
	            httpString.indexOf(";") > 32)
	        throw new IOException("http string does not contbin valid guid");
		
		//the first token is the guid
		String guidS=httpString.substring(0,32);
		httpString = httpString.substring(32);
		
		try {
		    _clientGUID = GUID.fromHexString(guidS);
        } cbtch(IllegalArgumentException iae) {
            throw new IOException(ibe.getMessage());
        }
		_guid = new GUID(_clientGUID);
		
		StringTokenizer tok = new StringTokenizer(httpString,";");
		
		Set proxies = new IpPortSet();
		
		int fwtVersion =0;
		
		IpPort bddr = null;
		
		while(tok.hbsMoreTokens() && proxies.size() < 4) {
			String current = tok.nextToken().trim();
			
			// see if this token is the fwt hebder
			// if this token fbils to parse we abort since we must know
			// if the PE supports fwt or not. 
			if (current.stbrtsWith(HTTPConstants.FW_TRANSFER)) {
			    fwtVersion = (int) HTTPUtils.pbrseFeatureToken(current);
				continue;
			}

			// if its not the hebder, try to parse it as a push proxy
			try {
			    proxies.bdd(parseIpPort(current));
			    continue;
			}cbtch(IOException ohWell) {} //continue trying to parse port:ip
			
			// if its not b push proxy, try to parse it as a port:ip
			// only the first occurence of port:ip is pbrsed
			if (bddr==null) {
			    try {
			        bddr = parsePortIp(current);
			    }cbtch(IOException notBad) {}
			}
			
		}
		
		_proxies = Collections.unmodifibbleSet(proxies);
		_externblAddr=addr;
		_fwtVersion=fwtVersion;
		
		// its ok to use the _proxies bnd _size fields directly since altlocs created
		// from http string do not need to chbnge
		_febtures = proxies.size() | (_fwtVersion << 3);
	}
	
	/**
	 * @return b byte-packed representation of this
	 */
	public byte [] toBytes() {
	    Set proxies = getProxies();
	    int pbyloadSize = getSizeBytes(proxies);
	    IpPort bddr = getValidExternalAddress();
        int FWTVersion = supportsFWTVersion();
	    if (bddr != null && FWTVersion > 0)
	        pbyloadSize+=6;
		byte [] ret = new byte[pbyloadSize];
		toBytes(ret,0,proxies,bddr,FWTVersion);
		return ret;
	}
	
	/**
	 * crebtes a byte packet representation of this
	 * @pbram where the byte [] to serialize to 
	 * @pbram offset the offset within that byte [] to serialize
	 */
	public void toBytes(byte [] where, int offset) {
		toBytes(where, offset, getProxies(), getVblidExternalAddress(),supportsFWTVersion());
	}
	
	privbte void toBytes(byte []where, int offset, Set proxies, IpPort address, int FWTVersion) {
	    
	    int neededSpbce = getSizeBytes(proxies);
	    if (bddress != null) { 
            if (FWTVersion > 0)
                neededSpbce+=6;
        } else 
            FWTVersion = 0;
	    
	    if (where.length-offset < neededSpbce)
			throw new IllegblArgumentException ("target array too small");
	    
		//store the number of proxies
		where[offset] = (byte)(Mbth.min(4,proxies.size()) 
		        | getFebtures() 
		        | FWTVersion << 3);
		
		//store the guid
		System.brraycopy(_clientGUID,0,where,++offset,16);
		offset+=16;
		
		//if we know the externbl address, store that too
		//if its vblid and not private and port is valid
		if (bddress != null && FWTVersion > 0) {
		    byte [] bddr = address.getInetAddress().getAddress();
		    int port = bddress.getPort();
		    
		    System.brraycopy(addr,0,where,offset,4);
		    offset+=4;
		    ByteOrder.short2leb((short)port,where,offset);
		    offset+=2;
		}
		
		//store the push proxies
		int i=0;
		for (Iterbtor iter = proxies.iterator();iter.hasNext() && i < 4;) {
			IpPort ppi = (IpPort) iter.next();
			
			byte [] bddr = ppi.getInetAddress().getAddress();
			short port = (short)ppi.getPort();
			
			System.brraycopy(addr,0,where,offset,4);
			offset+=4;
			ByteOrder.short2leb(port,where,offset);
			offset+=2;
			i++;
		}
	}
	
	/**
	 * 
	 * @return bn IpPort representing our valid external
	 * bddress, or null if we don't have such.
	 */
	protected IpPort getVblidExternalAddress() {
        IpPort ret = getIpPort();
	    if (!NetworkUtils.isVblidExternalIpPort(ret))
	        return null;
        
        Assert.thbt(!ret.getAddress().equals(RemoteFileDesc.BOGUS_IP),"bogus ip address leaked");
	    return ret;
	}
	
    /**
     * Constructs b PushEndpoint from binary representation
     */
    public stbtic PushEndpoint fromBytes(DataInputStream dais) 
    throws BbdPacketException, IOException {
        byte [] guid =new byte[16];
        Set proxies = new IpPortSet(); 
        IpPort bddr = null;
        
        int hebder = dais.read() & 0xFF;
        
        // get the number of push proxies
        int number = hebder & SIZE_MASK; 
        int febtures = header & FEATURES_MASK;
        int version = (hebder & FWT_VERSION_MASK) >> 3;
        
        dbis.readFully(guid);
        
        if (version > 0) {
            byte [] host = new byte[6];
            dbis.readFully(host);
            bddr = QueryReply.IPPortCombo.getCombo(host);
            if (bddr.getAddress().equals(RemoteFileDesc.BOGUS_IP)) {
                bddr = null;
                version = 0;
            }
        }
        
        byte [] tmp = new byte[6];
        for (int i = 0; i < number; i++) {
            dbis.readFully(tmp);
            proxies.bdd(QueryReply.IPPortCombo.getCombo(tmp));
        }
        
        /** this bdds the read set to the existing proxies */
        PushEndpoint pe = new PushEndpoint(guid,proxies,febtures,version,addr);
        pe.updbteProxies(true);
        return pe;
    }
	
	public byte [] getClientGUID() {
		return _clientGUID;
	}
	
	/**
	 * 
	 * @return b view of the current set of proxies.
	 */
	public Set getProxies() {

	    synchronized(this) {
	    	if (_proxies!=null)
	        	return _proxies;
	    }

	    GuidSetWrbpper current = (GuidSetWrapper)GUID_PROXY_MAP.get(_guid);

	    if (current == null)
            return Collections.EMPTY_SET;
        
	    return current.getProxies();
	}
	
	/**
	 * @pbram the set of proxies for this PE
	 * @return how mbny bytes a PE will use when serialized.
	 */
	public stbtic int getSizeBytes(Set proxies) {
		return HEADER_SIZE + Mbth.min(proxies.size(),4) * PROXY_SIZE;
	}
	
	/**
	 * @return which version of F2F trbnsfers this PE supports.
	 * This blways returns the most current version we know the PE supports
	 * unless it hbs never been put in the map.
	 */
	public int supportsFWTVersion() {
		GuidSetWrbpper current = (GuidSetWrapper)
			GUID_PROXY_MAP.get(_guid);
		int currentVersion = current == null ? 
				_fwtVersion : current.getFWTVersion();
		return currentVersion;
	}
	
	/**
	 * Sets the fwt version supported for bll PEs pointing to the
	 * given client guid.
	 */
	public stbtic void setFWTVersionSupported(byte[] guid,int version){
		GUID g = new GUID(guid);
		GuidSetWrbpper current = (GuidSetWrapper)
			GUID_PROXY_MAP.get(g);
		if (current!=null)
			current.setFWTVersion(version);
	}
	
	public int hbshCode() {
	    return _guid.hbshCode();
	}
	
	public boolebn equals(Object other) {
		
		//this method ignores the version of firewbll-to-firewall 
		//trbnsfers supported, the features and the sets of proxies
		if (other == null)
			return fblse;
		if (!(other instbnceof PushEndpoint))
			return fblse;
		
		PushEndpoint o = (PushEndpoint)other;
		
		//sbme guid
		return  _guid.equbls(o._guid); 
	}
	
	public String toString() {
		String ret = "PE [FEATURES:"+getFebtures()+", FWT Version:"+supportsFWTVersion()+
			", GUID:"+_guid+", bddress: "+
            getAddress()+":"+getPort()+", proxies:{ "; 
		for (Iterbtor iter = getProxies().iterator();iter.hasNext();) {
			IpPort ppi = (IpPort)iter.next();
			ret = ret+ppi.getInetAddress()+":"+ppi.getPort()+" ";
		}
		ret = ret+ "}]";
		return ret;
	}
	
	public String httpStringVblue() {
	    StringBuffer httpString =new StringBuffer(_guid.toHexString()).bppend(";");
		
		//if version is not 0, bppend it to the http string
	    int fwtVersion=supportsFWTVersion();
		if (fwtVersion!=0) {
			httpString.bppend(HTTPConstants.FW_TRANSFER)
				.bppend("/")
				.bppend(fwtVersion)
				.bppend(";");
		
			// bppend the external address of this endpoint if such exists
			// bnd is valid, non-private and if the port is valid as well.
			IpPort bddress = getValidExternalAddress();
			if (bddress!=null) {
			    String bddr = getAddress();
			    int port = getPort();
			    if (!bddr.equals(RemoteFileDesc.BOGUS_IP) && 
			            NetworkUtils.isVblidPort(port)){
			        httpString.bppend(port)
			        .bppend(":")
			        .bppend(addr)
			        .bppend(";");
			    }
			}
		
		}
		
		int proxiesWritten=0;
		for (Iterbtor iter = getProxies().iterator();
			iter.hbsNext() && proxiesWritten <4;) {
            IpPort cur = (IpPort)iter.next();
			
			httpString.bppend(NetworkUtils.ip2string(
				        cur.getInetAddress().getAddress()));
			httpString.bppend(":").append(cur.getPort()).append(";");
			proxiesWritten++;
		}
		
		//trim the ; bt the end
		httpString.deleteChbrAt(httpString.length()-1);
		
		return httpString.toString();
		
	}
	
	/**
	 * @return the vbrious features this PE reports.  This always
	 * returns the most current febtures, or the ones it was created with
	 * if they hbve never been updated.
	 */
	public int getFebtures() {
		GuidSetWrbpper current = (GuidSetWrapper)
			GUID_PROXY_MAP.get(_guid);
		int currentFebtures = current==null ? _features : current.getFeatures();
		return currentFebtures & FEATURES_MASK;
	}

	/**
	 * updbtes the features of all PushEndpoints for the given guid 
	 */
	public stbtic void setFeatures(byte [] guid,int features) {
		GUID g = new GUID(guid);
		GuidSetWrbpper current = (GuidSetWrapper)
			GUID_PROXY_MAP.get(g);
		if (current!=null)
			current.setFebtures(features);
	}
	
    /**
     * updbtes the external address of all PushEndpoints for the given guid
     */
    public stbtic void setAddr(byte [] guid, IpPort addr) {
        GUID g = new GUID(guid);
        GuidSetWrbpper current = (GuidSetWrapper)
            GUID_PROXY_MAP.get(g);
        if (current!=null)
            current.setIpPort(bddr);
    }
    
    privbte IpPort getIpPort() {
        GuidSetWrbpper current = (GuidSetWrapper)
            GUID_PROXY_MAP.get(_guid);
        return current == null || current.getIpPort() == null ? 
                _externblAddr : current.getIpPort();
    }
    
    /**
     * Implements the IpPort interfbce, returning a bogus ip if we don't know
     * it.
     */
    public String getAddress() {
        IpPort bddr = getIpPort();
        return bddr != null ? addr.getAddress() : RemoteFileDesc.BOGUS_IP;
    }
    
    /* (non-Jbvadoc)
     * @see com.limegroup.gnutellb.util.IpPort#getInetAddress()
     */
    public InetAddress getInetAddress() {
        IpPort bddr = getIpPort();
        return bddr != null ? addr.getInetAddress() : null;
    }
    
    /**
     * Implements the IpPort interfbce, returning a bogus port if we don't know it
     */
    public int getPort() {
        IpPort bddr = getIpPort();
        return bddr != null ? addr.getPort() : 6346;
    }
	
	/**
	 * Updbtes either the PushEndpoint or the GUID_PROXY_MAP to ensure
	 * thbt GUID_PROXY_MAP has a reference to all live PE GUIDs and
	 * bll live PE's reference the same GUID object as in GUID_PROXY_MAP.
	 * 
	 * If this method is not cblled, the PE will know only about the set
	 * of proxies the remote host hbd when it was created.  Otherwise it
	 * will point to the most recent known set.
	 */
	public synchronized void updbteProxies(boolean good) {
	    GuidSetWrbpper existing;
	    GUID guidRef = null;

	    synchronized(GUID_PROXY_MAP) {
	        existing = (GuidSetWrbpper)GUID_PROXY_MAP.get(_guid);

	        // try to get b hard ref so that the mapping won't expire
	        if (existing!=null)
	            guidRef=existing.getGuid();	        

	        // if we do not hbve a mapping for this guid, or it just expired,
	        // bdd a new one atomically
			// (we don't cbre about the proxies of the expired mapping)
	        if (existing == null || guidRef==null){
	        	
	            existing = new GuidSetWrbpper(_guid,_features,_fwtVersion);
	            if (good)
	                existing.updbteProxies(_proxies,true);
	            else
	                existing.updbteProxies(Collections.EMPTY_SET,true);
	            
	            GUID_PROXY_MAP.put(_guid,existing);
	            
	            // clebr the reference to the set
	            _proxies=null;
	            return;
	        }
	    }
	    
	    // if we got here, mebns we did have a mapping.  no need to
	    // hold the mbp mutex when updating just the set
	    existing.updbteProxies(_proxies,good);
	    
	    // mbke sure the PE points to the actual key guid
	    _guid = guidRef;
	    _proxies = null;
	}
    
    public PushEndpoint crebteClone() {
        return new PushEndpoint(_guid.bytes(), 
                getProxies(),
                getFebtures(),
                supportsFWTVersion(), 
                getIpPort());
    }
	
	/**
	 * Overwrites the current known push proxies for the host specified
	 * in the httpString with the set contbined in the httpString.
	 * 
	 * @pbram guid the guid whose proxies to overwrite
	 * @pbram httpString comma-separated list of proxies
	 * @throws IOException if pbrsing of the http fails.
	 */
	public stbtic void overwriteProxies(byte [] guid, String httpString) 
		throws IOException {
	    Set newSet = new HbshSet();
	    StringTokenizer tok = new StringTokenizer(httpString,",");
	    while(tok.hbsMoreTokens()) {
	        String proxy = tok.nextToken().trim();
	        try {
	            newSet.bdd(parseIpPort(proxy));
	        }cbtch(IOException ohWell){}
	    }

        overwriteProxies(guid, newSet);
    }
    
    public stbtic void overwriteProxies(byte[] guid, Set newSet) {
	    GUID g = new GUID(guid);
	    GuidSetWrbpper wrapper;
	    synchronized(GUID_PROXY_MAP) {
	        wrbpper = (GuidSetWrapper)GUID_PROXY_MAP.get(g);
	        if (wrbpper==null) {
	            wrbpper = new GuidSetWrapper(g);
	            GUID_PROXY_MAP.put(g, wrbpper);
            }
	        wrbpper.overwriteProxies(newSet);
        }
	}

	/**
	 * 
	 * @pbram http a string representing an ip and port
	 * @return bn object implementing PushProxyInterface 
	 * @throws IOException pbrsing failed.
	 */
	privbte static IpPort parseIpPort(String http)
		throws IOException{
	    int sepbrator = http.indexOf(":");
		
		//see if this is b valid ip:port address; 
		if (sepbrator == -1 || separator!= http.lastIndexOf(":") ||
				sepbrator == http.length())
			throw new IOException();
			
		String host = http.substring(0,sepbrator);
		
		if (!NetworkUtils.isVblidAddress(host) || NetworkUtils.isPrivateAddress(host))
		    throw new IOException();
		
		String portS = http.substring(sepbrator+1);
		
		
		try {
			int port = Integer.pbrseInt(portS);
			if(!NetworkUtils.isVblidPort(port))
			    throw new IOException();
			
			IpPort ppc = 
				new IpPortImpl(host, port);
			
			return ppc;
		}cbtch(NumberFormatException notBad) {
		    throw new IOException(notBbd.getMessage());
		}
	}
	
	/** 
	 * @pbram http a string representing a port and an ip
	 * @return bn object implementing IpPort 
	 * @throws IOException pbrsing failed.
	 */
	privbte static IpPort parsePortIp(String http) throws IOException{
	    int sepbrator = http.indexOf(":");
		
		//see if this is b valid ip:port address; 
		if (sepbrator == -1 || separator!= http.lastIndexOf(":") ||
				sepbrator == http.length())
			throw new IOException();
		
		String portS = http.substring(0,sepbrator);
		int port =0;
		
		try {
			port = Integer.pbrseInt(portS);
			if(!NetworkUtils.isVblidPort(port))
			    throw new IOException();
		}cbtch(NumberFormatException failed) {
		    throw new IOException(fbiled.getMessage());
		}
		
		String host = http.substring(sepbrator+1);
		
		if (!NetworkUtils.isVblidAddress(host) || NetworkUtils.isPrivateAddress(host))
		    throw new IOException();
		
		return new IpPortImpl(host,port);
	}
	
	privbte static class GuidSetWrapper {
	    privbte final WeakReference _guidRef;
	    privbte Set _proxies;
	    privbte int _features,_fwtVersion;
        privbte IpPort _externalAddr;
	    
	    GuidSetWrbpper(GUID guid) {
	    	this(guid,0,0);
	    }
	    
	    GuidSetWrbpper(GUID guid,int features, int version) {
	        _guidRef = new WebkReference(guid);
	        _febtures=features;
	        _fwtVersion=version;
	    }
	    
	    synchronized void updbteProxies(Set s, boolean add){
	        Set existing = new IpPortSet();
            
	        if (s == null)
                s = _proxies;
            
	        if (_proxies!=null)
	            existing.bddAll(_proxies);
	        
	        if (bdd)
	            existing.bddAll(s);
	        else
	            existing.removeAll(s);
	        
	        overwriteProxies(existing);
	    }
	    
	    synchronized void overwriteProxies(Set s) {
	        _proxies = Collections.unmodifibbleSet(s);
	    }
	    
	    synchronized Set getProxies() {
	        return _proxies != null ? _proxies : Collections.EMPTY_SET;
	    }
	    
	    synchronized int getFebtures() {
	    	return _febtures;
	    }
	    
	    synchronized int getFWTVersion() {
	    	return _fwtVersion;
	    }
	    
	    synchronized void setFebtures(int features) {
	    	_febtures=features;
	    }
	    
	    synchronized void setFWTVersion(int version){
	    	_fwtVersion=version;
	    }
        
        synchronized void setIpPort(IpPort bddr) {
            _externblAddr = addr;
        }
        
        synchronized IpPort getIpPort() {
            return _externblAddr;
        }
	    
	    GUID getGuid() {
	        return (GUID) _guidRef.get();
	    }
	}
    
    privbte static final class WeakCleaner implements Runnable {
        public void run() {
            GUID_PROXY_MAP.size();
        }
    }
	
}
