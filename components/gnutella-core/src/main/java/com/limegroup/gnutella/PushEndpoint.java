
package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;

/**
 * a class that represents an endpoint behind one or more PushProxies
 * everything is immutable including the contents of the set.
 * 
 * the network format this is serialized to is:
 * byte 0 : how many push proxies we have
 * bytes 1-16 : the guid
 * followed by 6 bytes per PushProxy
 */
public class PushEndpoint {

	/**
	 * the client guid of the endpoint
	 */
	private final byte [] _clientGUID;
	
	/**
	 * set of <tt>PushProxyInterface</tt> objects.
	 */
	private final Set _proxies;
	

	/**
	 * 
	 * @param guid the client guid	
	 * @param proxies the push proxies for that host
	 */
	public PushEndpoint(byte [] guid, Set proxies) {
		_clientGUID=guid;
		_proxies = Collections.unmodifiableSet(proxies);
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
		byte [] ret = new byte[17+_proxies.size()*6];
		toBytes(ret,0);
		return ret;
	}
	
	/**
	 * creates a byte packet representation of this
	 * @param where the byte [] to serialize to 
	 * @param offset the offset within that byte [] to serialize
	 */
	public void toBytes(byte [] where, int offset) {
		
		if (where.length-offset < 17*_proxies.size()*6)
			throw new IllegalArgumentException ("target array too small");
		
		//store the number of proxies
		where[offset] = (byte)_proxies.size();
		
		//store the guid
		System.arraycopy(_clientGUID,0,where,offset+1,16);
		
		//store the push proxies
		int i=0;
		for (Iterator iter = _proxies.iterator();iter.hasNext();) {
			PushProxyInterface ppi = (PushProxyInterface) iter.next();
			
			byte [] addr = ppi.getPushProxyAddress().getAddress();
			short port = (short)ppi.getPushProxyPort();
			
			System.arraycopy(addr,0,where,offset+17+i*6,4);
			ByteOrder.short2leb(port,where,offset+17+i*6+4);
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
		int number = data[offset];
		
		if (data.length -offset < 17+number*6)
			throw new BadPacketException("not a valid PushEndpoint");
		
		//get the guid
		System.arraycopy(data,offset+1,guid,0,16);
		
		
		for (int i=0;i<number;i++) {
			System.arraycopy(data, offset+17+i*6,tmp,0,6);
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
}
