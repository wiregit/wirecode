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
	
	List _endpoints;
	
	

	
	//this message is sent only as a reply to a request message, so when 
	//constructing it we need the object representing the request message
	
	public UPListVendorMessage(GiveUPVendorMessage request){
		super(F_LIME_VENDOR_ID,F_ULTRAPEER_LIST, VERSION, derivePayload(request));
		setGUID(new GUID(request.getGUID()));
	}
	
	private static byte [] derivePayload(GiveUPVendorMessage request) {
		
		//get a list of all ultrapeers we have connections to
		List endpoints = new LinkedList();
		
		Iterator iter = RouterService.getConnectionManager()
			.getInitializedConnections().iterator();
		
		while(iter.hasNext()) {
			Connection c = (Connection)iter.next();
			if (c.isGoodUltrapeer())
				endpoints.add( new Endpoint(c.getAddress(), c.getPort()));
		}
		
		//then see how many the client requested, if necessary trim
		if (request.getNumber() != request.ALL && 
				request.getNumber() < endpoints.size())
			endpoints = endpoints.subList(0,request.getNumber());
		
		//serialize the Endpoints to a byte []
		iter = endpoints.iterator();
		StringBuffer res = new StringBuffer();
		synchronized(res) {
			res.append((byte)endpoints.size());
			while (iter.hasNext()) {
				Endpoint e = (Endpoint) iter.next();
				res.append(packIPAddress(e.getInetAddress(),e.getPort()));
			}
		}
		return res.toString().getBytes();
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
			byte[] vendorID, int selector, int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, vendorID, selector, version, payload);
		
		// check if the payload is legal length
		byte number = payload[0];
		if (payload.length!= number*6+1) //evil evil
			throw new BadPacketException();
		
		//parse the ip addresses
		for (int i = 1;i<payload.length;i+=6) {
			byte [] current = new byte[6];
			System.arraycopy(payload,i,current,0,6);
			QueryReply.IPPortCombo combo = 
	            QueryReply.IPPortCombo.getCombo(current);
			_endpoints.add(new Endpoint(combo.getAddress(),combo.getPort()));
		}
		
		//leave the check whether we got as many UPs as requested elsewhere.
	}
	/**
	 * @return Returns the Endpoints contained in the message.
	 */
	public List getEndpoints() {
		return _endpoints;
	}
}
