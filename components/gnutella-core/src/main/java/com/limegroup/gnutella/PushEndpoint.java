
package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.messages.*;

import java.util.StringTokenizer;
import java.io.IOException;
import java.net.UnknownHostException;
;

/**
 * a class that represents an endpoint behind one or more PushProxies
 * everything is immutable including the contents of the set.
 * 
 * the network format this is serialized to is:
 * byte 0 : 
 *    - bits 0-2 how many push proxies we have (so max is 7)
 *    - bits 3-5 the version of the f2f transfer protocol this altloc supports
 *    - bits 6-7 other possible features.
 * bytes 1-16 : the guid
 * followed by 6 bytes per PushProxy
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
	private final int _size;
	
	/**
	 * the hashcode of this object
	 */
	private final int _hashcode;
	
	/**
	 * the string representation as sent in headers.
	 */
	private final String _httpString;
	
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
			_proxies = DataUtils.EMPTY_SET;
		
		_size = HEADER_SIZE+
			Math.min(_proxies.size(),4) * PROXY_SIZE;
		
		//create the http string representation
		//TODO: once the http format for f2f PEs gets finalized, 
		//mark the http string.
		String httpString = _guid.toHexString()+";";
		
		//also calculate the hashcode in the constructor
		
		int hashcode = _guid.hashCode();
		
		for (Iterator iter = _proxies.iterator();iter.hasNext();) {
			PushProxyInterface cur = (PushProxyInterface)iter.next();
			hashcode = 37 *hashcode+cur.hashCode();
			httpString = httpString + 
				NetworkUtils.ip2string(cur.getPushProxyAddress().getAddress());
			httpString = httpString +":"+cur.getPushProxyPort()+";";
		}
		
		//trim the ; at the end
		if (_proxies.size() > 0)
			httpString = httpString.substring(0,httpString.length()-1);
		
		_httpString = httpString;
		_hashcode = hashcode;
		
		
	}
	
	public PushEndpoint(byte [] guid, Set proxies) {
		//TODO: make this constructor check which version of fwt we support
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
		
		//TODO: once the format for marking F2F-enabled falts gets finalized,
		//mark the _features header.
		_features = PLAIN;
		
		//TODO: find the version of firewall to firewall transfer supported
		_fwtVersion=0;
		
		_httpString=httpString;
		StringTokenizer tok = new StringTokenizer(httpString,";");
		
		String guidS=null;
		try{
			guidS = tok.nextToken();
		}catch(NoSuchElementException bad) {
			throw new IOException(bad.getMessage());
		}
		
		_clientGUID = GUID.fromHexString(guidS);
		_guid = new GUID(_clientGUID);
		
		
		int hashcode = _guid.hashCode();
		
		HashSet proxies = new HashSet();
		
		int parsedProxies = 0;
		
		while(tok.hasMoreTokens() && parsedProxies < 4) {
			String current = tok.nextToken();
			
			int separator = current.indexOf(":");
			
			//see if this is a valid; skip gracefully invalid altlocs
			if (separator == -1 || separator!= current.lastIndexOf(":") ||
					separator == current.length())
				continue;
				
			String host = current.substring(0,separator);
			String portS = current.substring(separator+1);
			
			
			try {
				int port = Integer.parseInt(portS);
				
				QueryReply.PushProxyContainer ppc = 
					new QueryReply.PushProxyContainer(host, port);
				
				hashcode = 37* hashcode + ppc.hashCode();
				
				proxies.add(ppc);
				parsedProxies++;
				
			}catch(UnknownHostException notBad) {
				continue;
			}catch(IllegalArgumentException notBad) {
				continue;
			}
		}
		
		if (proxies.size() == 0)
			_proxies = DataUtils.EMPTY_SET;
		else
			_proxies = proxies;
		
		_hashcode=hashcode;
		
		_size = HEADER_SIZE+
			Math.min(_proxies.size(),4) * PROXY_SIZE;
	}
	
	/**
	 * @return a byte-packed representation of this
	 */
	public byte [] toBytes() {
		byte [] ret = new byte[_size];
		toBytes(ret,0);
		return ret;
	}
	
	/**
	 * creates a byte packet representation of this
	 * @param where the byte [] to serialize to 
	 * @param offset the offset within that byte [] to serialize
	 */
	public void toBytes(byte [] where, int offset) {
		
		if (where.length-offset < _size)
			throw new IllegalArgumentException ("target array too small");
		
		//store the number of proxies
		where[offset] = (byte)(Math.min(4,_proxies.size()) | _features);
		
		//store the guid
		System.arraycopy(_clientGUID,0,where,offset+1,16);
		
		//store the push proxies
		int i=0;
		for (Iterator iter = _proxies.iterator();iter.hasNext() && i < 4;) {
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
	 * 
	 * @return how many bytes this PE will use when serialized.
	 */
	public int getSizeBytes() {
		return _size;
	}
	
	/**
	 * @return which version of F2F transfers this PE supports.
	 */
	public int supportsFWTVersion() {
		return _fwtVersion;
	}
	
	public int hashCode() {
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
		
		//same guid
		boolean ret = Arrays.equals(_clientGUID,o.getClientGUID());
		
		//same # of push proxies
		ret = ret & _proxies.size() == o.getProxies().size();
		
		//and the same proxies
		HashSet temp = new HashSet(_proxies);
		temp.retainAll(o.getProxies());
		
		ret = ret & temp.size() ==_proxies.size();
		
		return ret;
	}
	
	public String toString() {
		String ret = "PE [FEATURES:"+_features+", FWT Version:"+_fwtVersion+
			", GUID:"+_guid+", proxies:{ "; 
		for (Iterator iter = _proxies.iterator();iter.hasNext();) {
			PushProxyInterface ppi = (PushProxyInterface)iter.next();
			ret = ret+ppi.getPushProxyAddress()+":"+ppi.getPushProxyPort()+" ";
		}
		ret = ret+ "}]";
		return ret;
	}
	
	public String httpStringValue() {
		return _httpString;
	}
	
	public int getFeatures() {
		return _features;
	}
	
}
