package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.messages.*;

import java.util.StringTokenizer;
import java.io.IOException;
import java.net.UnknownHostException;
;

/**
 * a class that represents an endpoint behind one or more PushProxies.
 * almost everything is immutable including the contents of the set.
 * 
 * the network format this is serialized to is:
 * byte 0 : 
 *    - bits 0-2 how many push proxies we have (so max is 7)
 *    - bits 3-5 the version of the f2f transfer protocol this altloc supports
 *    - bits 6-7 other possible features.
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
	
	private static final int FWT_VERSION_MASK=0x38; //0011 1000
	
	//the features mask does not clear the bits we do not understand
	//because we may pass on the altloc to someone who does.
	private static final int FEATURES_MASK=0xC0;   //1100 0000
	
	/**
	 * the client guid of the endpoint
	 */
	private final byte [] _clientGUID;
	
	/**
	 * the GUID as GUID object - cached to avoid recreating
	 */
	private final GUID _guid;
	
	/**
	 * set of <tt>PushProxyInterface</tt> objects.
	 */
	private final Set _proxies;
	
	/**
	 * how big this PE is/will be in bytes.
	 */
	private int _size=-1;
	
	/**
	 * the hashcode of this object
	 */
	private int _hashcode=-1;
	
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
	 * 
	 * @param guid the client guid	
	 * @param proxies the push proxies for that host
	 */
	public PushEndpoint(byte [] guid, Set proxies,int features,int version) {
		
		_features = ((features & FEATURES_MASK) | (version << 3));
		_fwtVersion=version;
		
		_clientGUID=guid;
		_guid = new GUID(guid);
		
		if (proxies!=null)
			_proxies = Collections.unmodifiableSet(proxies);
		else 
			_proxies = Collections.EMPTY_SET;
		
		
	}
	
	
	
	public PushEndpoint(byte [] guid, Set proxies) {
		this(guid,proxies,PLAIN,0);
	}
	
	/**
	 * creates a PushEndpoint without any proxies.  
	 * not very useful but can happen.
	 */
	public PushEndpoint(byte [] guid) {
		this(guid, DataUtils.EMPTY_SET);
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
		
		int hashcode = _guid.hashCode();
		
		HashSet proxies = new HashSet();
		
		int fwtVersion =0;
		
		while(tok.hasMoreTokens() && proxies.size() < 4) {
			String current = tok.nextToken();
			
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
				
				hashcode = 37* hashcode + ppc.hashCode();
				
				proxies.add(ppc);
				
			}catch(UnknownHostException notBad) {
				continue;
			}catch(NumberFormatException notBad) {
				continue;
			}
		}
		
		if (proxies.size() == 0)
			_proxies = Collections.EMPTY_SET;
		else
			_proxies = proxies;
		
		_hashcode=hashcode;
		
		_fwtVersion=fwtVersion;
		
		// its ok to use the _proxies and _size fields directly since altlocs created
		// from http string do not need to change
		_features = _proxies.size() | (_fwtVersion << 3);
		_size = HEADER_SIZE+
			Math.min(_proxies.size(),4) * PROXY_SIZE;
	}
	
	protected final int getHashcode() {
	    int hashcode = _guid.hashCode();
		
		for (Iterator iter = getProxies().iterator();iter.hasNext();) {
			PushProxyInterface cur = (PushProxyInterface)iter.next();
			hashcode = 37 *hashcode+cur.hashCode();	
		}
		
		return hashcode;
	}
	
	/**
	 * @return a byte-packed representation of this
	 */
	public byte [] toBytes() {
	    Set proxies = getProxies();
		byte [] ret = new byte[getSizeBytes(getProxies())];
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
		where[offset] = (byte)(Math.min(4,getProxies().size()) | _features);
		
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
		
		return new PushEndpoint(guid,proxies,features,version);
	}
	
	public byte [] getClientGUID() {
		return _clientGUID;
	}
	
	public Set getProxies() {
		return _proxies;
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
	    if (_hashcode==-1) {
	        _hashcode = getHashcode();
	    }
	        
		return _hashcode;
	}
	
	public boolean equals(Object other) {
		
		//this method ignores the version of firewall-to-firewall 
		//transfers supported.
		
		if (other == null)
			return false;
		if (!(other instanceof PushEndpoint))
			return false;
		
		PushEndpoint o = (PushEndpoint)other;
		
		// create a local ref to the two sets in case we compare
		// against a PushEndpointForSelf
		Set myProxies = getProxies();
		Set otherProxies = o.getProxies();
		
		//same guid
		boolean ret = Arrays.equals(_clientGUID,o.getClientGUID());
		
		//same # of push proxies
		ret = ret & myProxies.size() == otherProxies.size();
		
		//and the same proxies
		HashSet temp = new HashSet(myProxies);
		temp.retainAll(otherProxies);
		
		ret = ret & temp.size() ==myProxies.size();
		
		return ret;
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
		
		if (_httpString ==null) 	
			_httpString=generateHTTPString();
		
		return _httpString;
	}
	
	protected final String generateHTTPString() {
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
		return _features;
	}
	
}
