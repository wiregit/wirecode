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
	
	

	
	//this message is sent only as a reply to a request message, so when 
	//constructing it we need the object representing the request message
	
	public UPListVendorMessage(GiveUPVendorMessage request){
		super(F_LIME_VENDOR_ID,F_ULTRAPEER_LIST, VERSION, derivePayload(request));
		setGUID(new GUID(request.getGUID()));
	}
	
	private static byte [] derivePayload(GiveUPVendorMessage request) {
		
		//get a list of all ultrapeers and leafs we have connections to
		List endpointsUP = new LinkedList();
		List endpointsLeaf = new LinkedList();
		
		Iterator iter = RouterService.getConnectionManager()
			.getInitializedConnections().iterator();
		
		//add only good ultrapeers
		while(iter.hasNext()) {
			Connection c = (Connection)iter.next();
			if (c.isGoodUltrapeer()) 
				endpointsUP.add( new Endpoint(c.getAddress(), c.getPort()));
		}
		
		iter = RouterService.getConnectionManager()
			.getInitializedClientConnections().iterator();
		
		//add all leaves.. or not?
		while(iter.hasNext()) {
			Connection c = (Connection)iter.next();
			//if (c.isGoodLeaf()) //uncomment if you decide you want only good leafs 
				endpointsLeaf.add( new Endpoint(c.getAddress(), c.getPort()));
		}
		
		//then see how many of each kind the client requested, if necessary trim
		if (request.getNumberUP() != request.ALL && 
				request.getNumberUP() < endpointsUP.size())
			endpointsUP = endpointsUP.subList(0,request.getNumberUP());
		if (request.getNumberLeaves() != request.ALL && 
				request.getNumberLeaves() < endpointsLeaf.size())
			endpointsLeaf = endpointsLeaf.subList(0,request.getNumberLeaves());
		
		//cat the two lists
		endpointsUP.addAll(endpointsLeaf);
		
		//serialize the Endpoints to a byte []
		StringBuffer res = new StringBuffer();
		synchronized(res) {
			res.append((byte)endpointsUP.size());
			res.append((byte)endpointsLeaf.size());
			iter = endpointsUP.iterator();
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
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_ULTRAPEER_LIST, version, payload);
		
		// check if the payload is legal length
		byte numberUP = payload[0];
		byte numberLeaves = payload[1];
		if (payload.length!= (numberUP+numberLeaves)*6+2) //evil evil
			throw new BadPacketException();
		
		//parse the up ip addresses
		for (int i = 2;i<numberUP*6;i+=6) {
			byte [] current = new byte[6];
			System.arraycopy(payload,i,current,0,6);
			QueryReply.IPPortCombo combo = 
	            QueryReply.IPPortCombo.getCombo(current);
			_ultrapeers.add(new Endpoint(combo.getAddress(),combo.getPort()));
		}
		
		//parse the leaf ip addresses
		for (int i = numberUP*6+2;i<payload.length;i+=6) {
			byte [] current = new byte[6];
			System.arraycopy(payload,i,current,0,6);
			QueryReply.IPPortCombo combo = 
	            QueryReply.IPPortCombo.getCombo(current);
			_leaves.add(new Endpoint(combo.getAddress(),combo.getPort()));
		}
		
		//leave the check whether we got as many UPs as requested elsewhere.
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
}
