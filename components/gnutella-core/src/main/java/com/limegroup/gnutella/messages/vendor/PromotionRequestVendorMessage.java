/*
 * This class represents a promotion request message that an ultrapeer
 * sends to a candidate leaf.  This message is send in-band and is routed
 * by any ultrapeers along the path.
 */
package com.limegroup.gnutella.messages.vendor;


import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.upelection.Candidate;

import java.net.UnknownHostException;

public class PromotionRequestVendorMessage extends VendorMessage {
	
	
	public static final int VERSION = 1;
	/**
	 * the ultrapeer requesting the promotion and the candidate.
	 */
	final QueryReply.IPPortCombo _requestor, _candidate;
	
	/**
	 * how long this message has travelled already.  Identical to hops, 
	 * except that is used only when verifying and parsing.
	 * 
	 * valid values are 0, 1 and 2.
	 */
	final int _distance;
	
	/**
	 * constructs a new message with data from the network.
	 */
	protected PromotionRequestVendorMessage(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_PROMOTION_REQUEST, version, payload);
		
		//check if the payload is valid length
		if (payload.length!=13)
			throw new BadPacketException("invalid length payload");
		
		//see how long this message has travelled
		_distance = ByteOrder.ubyte2int(payload[0]);
		
		if (_distance <0 || _distance > 2)
			throw new BadPacketException("invalid distance value for the promotion request");
		
		try {
			_candidate = new QueryReply.IPPortCombo(NetworkUtils.ip2string(payload,1),
						ByteOrder.ubytes2int(ByteOrder.leb2short(payload, 5)));
			_requestor = new QueryReply.IPPortCombo(NetworkUtils.ip2string(payload,7),
						ByteOrder.ubytes2int(ByteOrder.leb2short(payload, 11)));
		} catch(UnknownHostException uhex) {
			//this should have been checked before sending, consider packet bad
			throw new BadPacketException("invalid hosts in promotion request");
		}	
	}
	
	public PromotionRequestVendorMessage(Candidate candidate) {
		super(F_LIME_VENDOR_ID,F_PROMOTION_REQUEST,VERSION,derivePayload(candidate));
		setGUID(new GUID(GUID.makeGuid()));
		
		//these fields should not be used when constructing the object this way.
		_candidate = null;
		_requestor = null;
		_distance = 0;
	}
	
	protected static byte [] derivePayload(Candidate candidate) {
		byte [] payload = new byte[13];
		
		//the first byte is 0 in this case
		payload[0]=0;
		
		//the next 6 bytes are the serialized candidate
		System.arraycopy(candidate.toBytes(),0,payload,1,6);
		
		//the last 6 bytes are our address
		System.arraycopy(RouterService.getExternalAddress(),0,payload,7,4);
		ByteOrder.short2leb((short)RouterService.getPort(),payload,11);  //TODO:check if this is the UDP port
		
		return payload;
	}
}
