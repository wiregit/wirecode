
package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;

/**
 * a class that represents an endpoint behind one or more PushProxies
 * everything is immutable including the contents of the set.
 * 
 * the network format this is serialized to is:
 * byte 0 : 
 *    - bits 0-2 how many push proxies we have (so max is 7)
 *    - bits 3-7 possible future features/flags
 * bytes 1-16 : the guid
 * followed by 6 bytes per PushProxy
 */
public class PushEndpoint {

	public static final int HEADER_SIZE=17; //guid+# of proxies, maybe other things too
	public static final int PROXY_SIZE=6; //ip:port
	
	private static final int SIZE_MASK=0x7;
	private static final int FEATURES_MASK=0xF8;
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
	 * 
	 * @param guid the client guid	
	 * @param proxies the push proxies for that host
	 */
	public PushEndpoint(byte [] guid, Set proxies) {
		
		_clientGUID=guid;
		_guid = new GUID(guid);
		
		if (proxies!=null)
			_proxies = Collections.unmodifiableSet(proxies);
		else 
			_proxies = DataUtils.EMPTY_SET;
		
		_size = HEADER_SIZE+
			Math.min(_proxies.size(),4) * PROXY_SIZE;
		
		//also calculate the hashcode in the constructor
		
		int hashcode = _guid.hashCode();
		
		for (Iterator iter = _proxies.iterator();iter.hasNext();) {
			PushProxyInterface cur = (PushProxyInterface)iter.next();
			hashcode = 37 *hashcode+cur.hashCode();
		}
		_hashcode = hashcode;
	}
	
	/**
	 * creates a PushEndpoint without any proxies.  
	 * not very useful but can happen.
	 */
	public PushEndpoint(byte [] guid) {
		this(guid, DataUtils.EMPTY_SET);
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
		where[offset] = (byte)(Math.min(4,_proxies.size()));
		
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
		
		if (data.length -offset < HEADER_SIZE+number*PROXY_SIZE)
			throw new BadPacketException("not a valid PushEndpoint");
		
		//get the guid
		System.arraycopy(data,offset+1,guid,0,16);
		
		
		for (int i=0;i<number;i++) {
			System.arraycopy(data, offset+HEADER_SIZE+i*PROXY_SIZE,tmp,0,6);
			proxies.add(new QueryReply.PushProxyContainer(tmp));
		}
		
		return new PushEndpoint(guid,proxies);
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
	
	public int hashCode() {
		return _hashcode;
	}
	
	public boolean equals(Object other) {
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
		String ret = "PE [GUID:"+_guid+", proxies:{ "; 
		for (Iterator iter = _proxies.iterator();iter.hasNext();) {
			PushProxyInterface ppi = (PushProxyInterface)iter.next();
			ret = ret+ppi.getPushProxyAddress()+":"+ppi.getPushProxyPort()+" ";
		}
		ret = ret+ "}]";
		return ret;
	}
}
