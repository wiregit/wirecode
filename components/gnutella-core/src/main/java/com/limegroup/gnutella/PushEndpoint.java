package com.limegroup.gnutella;


import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.messages.*;

import java.util.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.UnknownHostException;


/**
 * a class that represents an endpoint behind one or more PushProxies.
 * almost everything is immutable including the contents of the set.
 * 
 * the network format this is serialized to is:
 * byte 0 : 
 *    - bits 0-2 how many push proxies we have (so max is 7)
 *    - bits 3-4 the version of the f2f transfer protocol this altloc supports
 *    - bits 5-7 other possible features.
 * bytes 1-16 : the guid
 * followed by 6 bytes per PushProxy
 * 
 * the http format this is serialized to is an ascii string consisting of
 * ';'-delimited tokens.  The first token is the client GUID represented in hex
 * and is the only required token.  The other tokens can be addresses of push proxies
 * or various feature headers.  Currently the only feature header we parse is the
 * fwawt header that contains the version number of the firewall to firewall transfer
 * protocol supported by the altloc.
 * 
 * Examples:
 * 
 *  //altloc with 2 proxies that supports firewall transfer 1 :
 * 
 * <ThisIsTheGUIDASDF>;fwt/1.0;20.30.40.50:60;1.2.3.4:5567
 * 
 *   //altloc with 1 proxy that doesn't support firewall transfer :
 * 
 * <ThisIsTHeGUIDasfdaa527>;1.2.3.4:5564
 * 
 * //altloc with 1 proxy that supports two features we don't know/care about :
 * 
 * <ThisIsTHeGUIDasfdaa527>;someFeature/3.2;10.20.30.40:5564;otherFeature/0.4
 * 
 *  //altloc without any proxies that doesn't support any features
 *  // not very useful, but still valid  
 * 
 * <ThisIsTheGUIDasdf23457>
 */
public class PushEndpoint implements HTTPHeaderValue{

	public static final int HEADER_SIZE=17; //guid+# of proxies, maybe other things too
	public static final int PROXY_SIZE=6; //ip:port
	
	public static final int PLAIN=0x0; //no features for this PE
	
	private static final int SIZE_MASK=0x7; //0000 0111
	
	private static final int FWT_VERSION_MASK=0x18; //0001 1000
	
	//the features mask does not clear the bits we do not understand
	//because we may pass on the altloc to someone who does.
	private static final int FEATURES_MASK=0xE0;   //1110 0000
	

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
	private static final Map GUID_PROXY_MAP = 
	    Collections.synchronizedMap(new WeakHashMap());
	
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
	private Set _proxies;

	/**
	 * 
	 * @param guid the client guid	
	 * @param proxies the push proxies for that host
	 */
	public PushEndpoint(byte [] guid, Set proxies,int features,int version) {
		_features = ((features & FEATURES_MASK) | (version << 3));
		_fwtVersion=version;
		_clientGUID=guid;
		_guid = new GUID(_clientGUID);
		if (proxies!=null)
		    _proxies = proxies;
		else 
		    _proxies = new HashSet();
	}
	
	
	
	public PushEndpoint(byte [] guid, Set proxies) {
		this(guid,proxies,PLAIN,0);
	}
	
	/**
	 * creates a PushEndpoint without any proxies.  
	 * not very useful but can happen.
	 */
	public PushEndpoint(byte [] guid) {
		this(guid, Collections.EMPTY_SET);
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
		
		_clientGUID = GUID.fromHexString(guidS);
		_guid = new GUID(_clientGUID);
		
		StringTokenizer tok = new StringTokenizer(httpString,";");
		
		Set proxies = new HashSet();
		
		int fwtVersion =0;
		
		while(tok.hasMoreTokens() && proxies.size() < 4) {
			String current = tok.nextToken().trim();
			
			// see if this token is the fwt header
			// if this token fails to parse we abort since we must know
			// if the PE supports fwt or not. 
			if (current.startsWith(HTTPConstants.FW_TRANSFER)) {
			    fwtVersion = (int) HTTPUtils.parseFeatureToken(current);
				continue;
			}
			
			// if its not the header, try to parse it as a push proxy
			// ignoring invalid ones
			try {
			    proxies.add(parseIpPort(current));
			}catch(IOException ohWell) {
			    continue;
			}
		}
		
		_proxies = proxies;
		_fwtVersion=fwtVersion;
		// its ok to use the _proxies and _size fields directly since altlocs created
		// from http string do not need to change
		_features = proxies.size() | (_fwtVersion << 3);
	}
	
	/**
	 * @return a byte-packed representation of this
	 */
	public byte [] toBytes() {
	    Set proxies = getProxies();
		byte [] ret = new byte[getSizeBytes(proxies)];
		toBytes(ret,0,proxies);
		return ret;
	}
	
	/**
	 * creates a byte packet representation of this
	 * @param where the byte [] to serialize to 
	 * @param offset the offset within that byte [] to serialize
	 */
	public void toBytes(byte [] where, int offset) {
		toBytes(where, offset, getProxies());
	}
	
	private void toBytes(byte []where, int offset, Set proxies) {
	    
	    if (where.length-offset < getSizeBytes(proxies))
			throw new IllegalArgumentException ("target array too small");
		
		//store the number of proxies
		where[offset] = (byte)(Math.min(4,proxies.size()) | getFeatures());
		
		//store the guid
		System.arraycopy(_clientGUID,0,where,offset+1,16);
		
		//store the push proxies
		int i=0;
		for (Iterator iter = proxies.iterator();iter.hasNext() && i < 4;) {
			PushProxyInterface ppi = (PushProxyInterface) iter.next();
			
			byte [] addr = ppi.getPushProxyAddress().getAddress();
			short port = (short)ppi.getPushProxyPort();
			
			System.arraycopy(addr,0,where,offset+HEADER_SIZE+i*PROXY_SIZE,4);
			ByteOrder.short2leb(port,where,offset+HEADER_SIZE+i*PROXY_SIZE+4);
			i++;
		}
	}
	/**
	 * 
	 * @param data data read from network 
	 */
	public static PushEndpoint fromBytes(byte [] data) throws BadPacketException{
		return fromBytes(data, 0);
	}
	
	/**
	 * 
	 * @param data data read from network
	 * @param offset offset within that data
	 */
	public static PushEndpoint fromBytes(byte [] data, int offset)
		throws BadPacketException {
		byte [] tmp = new byte[6];
		byte [] guid =new byte[16];
		Set proxies = new HashSet(); //PushProxyContainers are good with HashSets
		
		//get the number of push proxies
		int number = data[offset] & SIZE_MASK;
		int features = data[offset] & FEATURES_MASK;
		int version = (data[offset] & FWT_VERSION_MASK) >> 3;
		
		if (data.length -offset < HEADER_SIZE+number*PROXY_SIZE)
			throw new BadPacketException("not a valid PushEndpoint");
		
		//get the guid
		System.arraycopy(data,offset+1,guid,0,16);
		
		
		for (int i=0;i<number;i++) {
			System.arraycopy(data, offset+HEADER_SIZE+i*PROXY_SIZE,tmp,0,6);
			proxies.add(new QueryReply.PushProxyContainer(tmp));
		}
		
		/** this adds the read set to the existing proxies */
		PushEndpoint pe = new PushEndpoint(guid,proxies,features,version);
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
	public Set getProxies() {
	    GuidSetWrapper current = (GuidSetWrapper)GUID_PROXY_MAP.get(_guid);
	    Set proxies = _proxies;
	    if(current != null)
	        proxies = current.getProxies();
	    
	    Assert.that(proxies != null);	    
		return proxies;
	}
	
	/**
	 * @param the set of proxies for this PE
	 * @return how many bytes a PE will use when serialized.
	 */
	public static int getSizeBytes(Set proxies) {
		return HEADER_SIZE + Math.min(proxies.size(),4) * PROXY_SIZE;
	}
	
	/**
	 * @return which version of F2F transfers this PE supports.
	 * This always returns the most current version we know the PE supports
	 * unless it has never been put in the map.
	 */
	public int supportsFWTVersion() {
		GuidSetWrapper current = (GuidSetWrapper)
			GUID_PROXY_MAP.get(_guid);
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
		GuidSetWrapper current = (GuidSetWrapper)
			GUID_PROXY_MAP.get(g);
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
			", GUID:"+_guid+", proxies:{ "; 
		for (Iterator iter = getProxies().iterator();iter.hasNext();) {
			PushProxyInterface ppi = (PushProxyInterface)iter.next();
			ret = ret+ppi.getPushProxyAddress()+":"+ppi.getPushProxyPort()+" ";
		}
		ret = ret+ "}]";
		return ret;
	}
	
	public String httpStringValue() {
	    StringBuffer httpString =new StringBuffer(_guid.toHexString()).append(";");
		
		//if version is not 0, append it to the http string
	    int fwtVersion=supportsFWTVersion();
		if (fwtVersion!=0)
			httpString.append(HTTPConstants.FW_TRANSFER)
				.append("/")
				.append(fwtVersion)
				.append(";");
		
		int proxiesWritten=0;
		for (Iterator iter = getProxies().iterator();
			iter.hasNext() && proxiesWritten <4;) {
			PushProxyInterface cur = (PushProxyInterface)iter.next();
			
			httpString.append(NetworkUtils.ip2string(
				        cur.getPushProxyAddress().getAddress()));
			httpString.append(":").append(cur.getPushProxyPort()).append(";");
			proxiesWritten++;
		}
		
		//trim the ; at the end
		httpString.deleteCharAt(httpString.length()-1);
		
		return httpString.toString();
		
	}
	
	/**
	 * @return the various features this PE reports.  This always
	 * returns the most current features, or the ones it was created with
	 * if they have never been updated.
	 */
	public int getFeatures() {
		GuidSetWrapper current = (GuidSetWrapper)
			GUID_PROXY_MAP.get(_guid);
		int currentFeatures = current==null ? _features : current.getFeatures();
		return currentFeatures & FEATURES_MASK;
	}

	/**
	 * updates the features of all PushEndpoints for the given guid 
	 */
	public static void setFeatures(byte [] guid,int features) {
		GUID g = new GUID(guid);
		GuidSetWrapper current = (GuidSetWrapper)
			GUID_PROXY_MAP.get(g);
		if (current!=null)
			current.setFeatures(features);
	}
	
	/**
	 * Updates either the PushEndpoint or the GUID_PROXY_MAP to ensure
	 * that GUID_PROXY_MAP has a reference to all live PE GUIDs and
	 * all live PE's reference the same GUID object as in GUID_PROXY_MAP.
	 */
	public void updateProxies(boolean good) {
	    GuidSetWrapper existing;
	    GUID guidRef = null;

	    synchronized(GUID_PROXY_MAP) {
	        existing = (GuidSetWrapper)GUID_PROXY_MAP.get(_guid);

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
	                existing.updateProxies(Collections.EMPTY_SET,true);
	            
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
	
	/**
	 * Overwrites the current known push proxies for the host specified
	 * in the httpString with the set contained in the httpString.
	 * 
	 * @param guid the guid whose proxies to overwrite
	 * @param httpString comma-separated list of proxies
	 * @throws IOException if parsing of the http fails.
	 */
	public static void overwriteProxies(byte [] guid, String httpString) 
		throws IOException {
	    Set newSet = new HashSet();
	    StringTokenizer tok = new StringTokenizer(httpString,",");
	    while(tok.hasMoreTokens()) {
	        String proxy = tok.nextToken().trim();
	        try {
	            newSet.add(parseIpPort(proxy));
	        }catch(IOException ohWell){}
	    }

        overwriteProxies(guid, newSet);
    }
    
    public static void overwriteProxies(byte[] guid, Set newSet) {
	    GUID g = new GUID(guid);
	    GuidSetWrapper wrapper;
	    synchronized(GUID_PROXY_MAP) {
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
	 * @return an object implementing PushProxyInterface 
	 * @throws IOException parsing failed.
	 */
	private static PushProxyInterface parseIpPort(String http)
		throws IOException{
	    int separator = http.indexOf(":");
		
		//see if this is a valid ip:port address; 
		if (separator == -1 || separator!= http.lastIndexOf(":") ||
				separator == http.length())
			throw new IOException();
			
		String host = http.substring(0,separator);
		
		if (!NetworkUtils.isValidAddress(host))
		    throw new IOException();
		
		String portS = http.substring(separator+1);
		
		
		try {
			int port = Integer.parseInt(portS);
			if(!NetworkUtils.isValidPort(port))
			    throw new IOException();
			
			QueryReply.PushProxyContainer ppc = 
				new QueryReply.PushProxyContainer(host, port);
			
			return ppc;
		}catch(NumberFormatException notBad) {
		    throw new IOException(notBad.getMessage());
		}
	}
	
	private static class GuidSetWrapper {
	    private final WeakReference _guidRef;
	    private Set _proxies;
	    private int _features,_fwtVersion;
	    
	    GuidSetWrapper(GUID guid) {
	    	this(guid,0,0);
	    }
	    
	    GuidSetWrapper(GUID guid,int features, int version) {
	        _guidRef = new WeakReference(guid);
	        _features=features;
	        _fwtVersion=version;
	    }
	    
	    synchronized void updateProxies(Set s, boolean add){
	        Set existing = new HashSet();
	        
	        if (_proxies!=null)
	            existing.addAll(_proxies);
	        
	        if (add)
	            existing.addAll(s);
	        else
	            existing.removeAll(s);
	        
	        overwriteProxies(existing);
	    }
	    
	    synchronized void overwriteProxies(Set s) {
	        _proxies = Collections.unmodifiableSet(s);
	    }
	    
	    synchronized Set getProxies() {
	        return _proxies != null ? _proxies : Collections.EMPTY_SET;
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
	    
	    GUID getGuid() {
	        return (GUID) _guidRef.get();
	    }
	}
	
}
