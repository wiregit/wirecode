/*
 * This message represents a list of ultrapeer connections that has been 
 * returned by an ultrapeer.  Its payload is a byte indicating how many
 * IpPorts are about to follow and their serialized list.
 */
package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.connection.*;
import com.limegroup.gnutella.messages.*;

import com.sun.java.util.collections.*;

import java.net.*;

public class UPListVendorMessage extends VendorMessage {
	
	public static final int VERSION = 1;
	
	List _ultrapeers, _leaves;
	
	final boolean _connectionTime, _localeInfo;
	
	/**
	 * the format of the response.
	 */
	private final byte _format;
	

	
	//this message is sent only as a reply to a request message, so when 
	//constructing it we need the object representing the request message
	
	public UPListVendorMessage(GiveUPVendorMessage request){
		super(F_LIME_VENDOR_ID,F_ULTRAPEER_LIST, VERSION, derivePayload(request));
		setGUID(new GUID(request.getGUID()));
		_format = (byte)(request.getFormat() & GiveUPVendorMessage.FEATURE_MASK);
		_localeInfo = request.asks4LocaleInfo();
		_connectionTime = request.asks4ConnectionTime();
	}
	
	private static byte [] derivePayload(GiveUPVendorMessage request) {
		
		//local copy of the requested format
		byte format = (byte)(request.getFormat() & GiveUPVendorMessage.FEATURE_MASK);
		
		//get a list of all ultrapeers and leafs we have connections to
		List endpointsUP = new LinkedList();
		List endpointsLeaf = new LinkedList();
		
		Iterator iter = RouterService.getConnectionManager()
			.getInitializedConnections().iterator();
		
		//add only good ultrapeers
		while(iter.hasNext()) {
			Connection c = (Connection)iter.next();
			if (c.isGoodUltrapeer()) 
				endpointsUP.add(c);
		}
		
		iter = RouterService.getConnectionManager()
			.getInitializedClientConnections().iterator();
		
		//add all leaves.. or not?
		while(iter.hasNext()) {
			Connection c = (Connection)iter.next();
			//if (c.isGoodLeaf()) //uncomment if you decide you want only good leafs 
				endpointsLeaf.add(c);
		}
		
		//TODO:when the locale pref stuff gets merged, sort the results by locale pref
		//and do not randomize
		//the ping does not carry info about which locale to preference to, so we'll just
		//preference any locale.  In reality we will probably have only connections only to 
		//this host's pref'd locale so they will end up in the pong.
		
		//then see how many of each kind the client requested, if necessary trim
		if (request.getNumberUP() != request.ALL && 
				request.getNumberUP() < endpointsUP.size()) {
			//randomized trim
			int index = (int) Math.floor(Math.random()*
					(endpointsUP.size()-request.getNumberUP()));
			endpointsUP = endpointsUP.subList(index,index+request.getNumberUP());
		}
		if (request.getNumberLeaves() != request.ALL && 
				request.getNumberLeaves() < endpointsLeaf.size()) {
			//randomized trim
			int index = (int) Math.floor(Math.random()*
					(endpointsLeaf.size()-request.getNumberLeaves()));
			endpointsLeaf = endpointsLeaf.subList(index,index+request.getNumberLeaves());
		}
		
		
		//serialize the Endpoints to a byte []
		int bytesPerResult = 6;
		if (request.asks4ConnectionTime())
			bytesPerResult+=2;
		if (request.asks4LocaleInfo())
			bytesPerResult+=2;
		byte [] result = new byte[(endpointsUP.size()+endpointsLeaf.size())*
								  bytesPerResult+3];
		
		//write out metainfo
		result[0] = (byte)endpointsUP.size();
		result[1] = (byte)endpointsLeaf.size();
		result[2] = format;
		
		//cat the two lists
		endpointsUP.addAll(endpointsLeaf);
		
		//cache the call to currentTimeMillis() cause its not always cheap
		long now = System.currentTimeMillis();
		
		int index = 3;
		iter = endpointsUP.iterator();
		while(iter.hasNext()) {
			//pack each entry into a 6 byte array and add it to the result.
			Connection c = (Connection)iter.next();
			System.arraycopy(
					packIPAddress(c.getInetAddress(),c.getPort()),
					0,
					result,
					index,
					6);
			index+=6;
			//add connection time if asked for
			//represent it as a short with the # of minutes
			if (request.asks4ConnectionTime()) {
				long uptime = now - c.getConnectionTime();
				short packed = (short) ( uptime / (60*1000));
				ByteOrder.short2leb(packed, result, index);
				index+=2;
			}
				
			if (request.asks4LocaleInfo()){
				//TODO: when locale pref gets merged put the 2-byte language code here
			}
			
		}
		
		return result;
	}
	
	
	/**
	 * copy/pasted from PushProxyRequest.  This should go to NetworkUtils imho
	 * @param addr address of the other person
	 * @param port the port
	 * @return 6-byte value representing the address and port.
	 */
	private static byte[] packIPAddress(InetAddress addr, int port) {
        try {
            // i do it during construction....
            QueryReply.IPPortCombo combo = 
                new QueryReply.IPPortCombo(addr.getHostAddress(), port);
            return combo.toBytes();
        } catch (UnknownHostException uhe) {
            throw new IllegalArgumentException(uhe.getMessage());
        }
    }
	
	/**
	 * see parent comment
	 */
	public UPListVendorMessage(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_ULTRAPEER_LIST, version, payload);
		
		
		_ultrapeers = new LinkedList();
		_leaves = new LinkedList();
		
		
		int numberUP = payload[0];
		int numberLeaves = payload[1];
		//we mask the received results with our capabilities mask because
		//we should not be receiving more features than we asked for, even
		//if the other side supports them.
		_format = (byte) (payload[2] & GiveUPVendorMessage.FEATURE_MASK);
		
		_connectionTime = ((_format & GiveUPVendorMessage.CONNECTION_TIME)
			== (int)GiveUPVendorMessage.CONNECTION_TIME);
		_localeInfo = (_format & GiveUPVendorMessage.LOCALE_INFO)
			== (int)GiveUPVendorMessage.LOCALE_INFO;
		
		int bytesPerResult = 6;
		
		if (_connectionTime)
			bytesPerResult+=2;
		if (_localeInfo)
			bytesPerResult+=2;
		
		//check if the payload is legal length
		if (payload.length!= (numberUP+numberLeaves)*bytesPerResult+3) //evil evil
			throw new BadPacketException("size is "+payload.length+ 
					" but should have been "+ (numberUP+numberLeaves)*bytesPerResult+2);
		
		//parse the up ip addresses
		for (int i = 3;i<numberUP*bytesPerResult;i+=bytesPerResult) {
			
			byte [] current = new byte[6];
			System.arraycopy(payload,i,current,0,6);
			
			QueryReply.IPPortCombo combo = 
	            QueryReply.IPPortCombo.getCombo(current);
			
			if (combo == null || combo.getInetAddress() == null)
				throw new BadPacketException("parsing of ip:port failed. "+
						" dump of current byte block: "+current);
			
			//store the result in an ExtendedEndpoint
			ExtendedEndpoint result; 
			
			if(_connectionTime)  //FIXME: when the changes to EE get merged, add a setter rather than doing this 
				result= new ExtendedEndpoint(combo.getAddress(),
								combo.getPort(),
								ByteOrder.leb2short(payload,i+6));
			else 
				result= new ExtendedEndpoint(combo.getAddress(),
						combo.getPort());
			
			if (_localeInfo) {
				//add the locale info to the extendedEndpoint
			}
			 _ultrapeers.add(result);
			
			
		}
		
		//parse the leaf ip addresses
		for (int i = numberUP*bytesPerResult+3;i<payload.length;i+=bytesPerResult) {
			byte [] current = new byte[6];
			System.arraycopy(payload,i,current,0,6);
			
			QueryReply.IPPortCombo combo = 
	            QueryReply.IPPortCombo.getCombo(current);
			
			if (combo == null || combo.getInetAddress() == null)
				throw new BadPacketException("parsing of ip:port failed. "+
						" dump of current byte block: "+current);
			
			ExtendedEndpoint result; 
			
			if(_connectionTime) 
				result= new ExtendedEndpoint(combo.getAddress(),
								combo.getPort(),
								ByteOrder.leb2short(payload,i+6));
			else 
				result= new ExtendedEndpoint(combo.getAddress(),
						combo.getPort());
			
			if (_localeInfo) {
				//add the locale info to the extendedEndpoint
			}
			 _leaves.add(result);
		}
		
		//do the check whether we got as many UPs as requested elsewhere.
	}
	/**
	 * @return Returns the List of Ultrapeers contained in the message.
	 */
	public List getUltrapeers() {
		return _ultrapeers;
	}
	
	/**
	 * @return Returns the List of Leaves contained in the message.
	 */
	public List getLeaves() {
		return _leaves;
	}
	/**
	 * @return whether the set of results contains connection uptime
	 */
	public boolean hasConnectionTime() {
		return _connectionTime;
	}
	/**
	 * @return whether the set of results contains locale information
	 */
	public boolean hasLocaleInfo() {
		return _localeInfo;
	}
}
