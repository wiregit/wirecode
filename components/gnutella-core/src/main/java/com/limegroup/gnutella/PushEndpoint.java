package com.limegroup.gnutella;


import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.messages.*;

import java.util.*;
import java.io.IOException;
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
	
	
	private static final Map GUID_PROXY_MAP = 
	    Collections.synchronizedMap(new WeakHashMap());
	
	/**
	 * the client guid of the endpoint
	 */
	private final byte [] _clientGUID;
	
	/**
	 * the guid as an object to avoid recreating
	 */
	private final GUID _guid;
	
	/**
	 * the string representation as sent in headers.
	 */
	private String _httpString;
	
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
	 * the set of proxies I have immediately after creating the endpoint
	 * cleared after registering in the map.
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
		    _proxies = Collections.synchronizedSet(proxies);
		else 
		    _proxies = Collections.synchronizedSet(new HashSet());
			
		
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
	 * 
	 */
	public PushEndpoint(String httpString) throws IOException{
		

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
			
			int separator = current.indexOf(":");
			
			//see if this is a valid ip:port address; skip gracefully invalid altlocs
			if (separator == -1 || separator!= current.lastIndexOf(":") ||
					separator == current.length())
				continue;
				
			String host = current.substring(0,separator);
			
			if (!NetworkUtils.isValidAddress(host))
			    continue;
			
			String portS = current.substring(separator+1);
			
			
			try {
				int port = Integer.parseInt(portS);
				
				QueryReply.PushProxyContainer ppc = 
					new QueryReply.PushProxyContainer(host, port);
				
				proxies.add(ppc);
				
			}catch(UnknownHostException notBad) {
				continue;
			}catch(NumberFormatException notBad) {
				continue;
			}
		}
		
		_proxies = Collections.synchronizedSet(proxies);
		
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
		where[offset] = (byte)(Math.min(4,proxies.size()) | _features);
		
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
		updateProxies(pe,true);
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
	    Set current = (Set)GUID_PROXY_MAP.get(_clientGUID);
		return current==null ? null : Collections.unmodifiableSet(current);
	}
	
	/**
	 * @param the set of proxies for this PE
	 * @return how many bytes a PE will use when serialized.
	 */
	public static int getSizeBytes(Set proxies) {
			return HEADER_SIZE+
				Math.min(proxies.size(),4) * PROXY_SIZE;
	}
	
	/**
	 * @return which version of F2F transfers this PE supports.
	 */
	public int supportsFWTVersion() {
		return _fwtVersion;
	}
	
	public int hashCode() {
	    return _clientGUID.hashCode();
	}
	
	public boolean equals(Object other) {
		
		//this method ignores the version of firewall-to-firewall 
		//transfers supported.
		
		if (other == null)
			return false;
		if (!(other instanceof PushEndpoint))
			return false;
		
		PushEndpoint o = (PushEndpoint)other;
		
		//same guid
		return Arrays.equals(_clientGUID,o.getClientGUID());
	}
	
	public String toString() {
		String ret = "PE [FEATURES:"+_features+", FWT Version:"+_fwtVersion+
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
		if (_fwtVersion!=0)
			httpString.append(HTTPConstants.FW_TRANSFER)
				.append("/")
				.append(_fwtVersion)
				.append(";");
		
		for (Iterator iter = getProxies().iterator();iter.hasNext();) {
			PushProxyInterface cur = (PushProxyInterface)iter.next();
			
			httpString.append(NetworkUtils.ip2string(
				        cur.getPushProxyAddress().getAddress()));
			httpString.append(":").append(cur.getPushProxyPort()).append(";");
		}
		
		//trim the ; at the end
		httpString.deleteCharAt(httpString.length()-1);
		
		return httpString.toString();
		
	}
	
	public int getFeatures() {
		return _features & FEATURES_MASK;
	}
	
	/**
	 * Merges the known push proxies for the host specified in the http string  
	 * with the set contained in the http string.
	 * @param good whether these proxies were successfull or not (X-FAlt vs X-NFAlt)
	 * @return a PushEndpoint object with updated proxies
	 */
	public static PushEndpoint updateProxies(String httpString,boolean good) 
		throws IOException{
	    PushEndpoint pe = new PushEndpoint(httpString);
	    updateProxies(pe,good);
	    return pe;
	}
	
	private static void updateProxies(PushEndpoint pe,boolean good){
	    
	    Set existing;
	    	    
	    synchronized(GUID_PROXY_MAP) {
	        existing = (Set)GUID_PROXY_MAP.get(pe._guid);
	        
	        // if we do not have a mapping for this guid, add a
	        // new one atomically
	        if (existing == null && good) {
	            existing = Collections.synchronizedSet(pe._proxies);
	            GUID_PROXY_MAP.put(pe._guid,existing);
	            pe._proxies=null;
	            return;
	        }
	    }
	    
	    // if we got here, means we did have a mapping.  no need to
	    // hold the map mutex when updating just the set
	    if (good)
	        existing.addAll(pe._proxies);
	    else
	        existing.removeAll(pe._proxies);
	        
	    pe._proxies=null;
	}
	
	/**
	 * Overwrites the current known push proxies for the host specified
	 * in the httpString with the set contained in the httpString
	 * @param httpString
	 * @throws IOException
	 */
	public static PushEndpoint overwriteProxies(String httpString) 
		throws IOException{
	    PushEndpoint pe = new PushEndpoint(httpString);
	    GUID_PROXY_MAP.put(pe._guid,pe._proxies);
	    pe._proxies=null;
	    return pe;
	}
	
}
