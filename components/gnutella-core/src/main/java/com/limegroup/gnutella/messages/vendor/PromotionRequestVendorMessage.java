/*
 * This class represents a promotion request message that an ultrapeer
 * sends to a candidate leaf.  This message is sent in-band and is routed
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
	
	/**
	 * creates an outgoing promotion request based on the best candidate we know of.
	 * @param candidate the leaf within ttl 2 that we know is the best candidate for an UP.
	 */
	public PromotionRequestVendorMessage(Candidate candidate) {
		super(F_LIME_VENDOR_ID,F_PROMOTION_REQUEST,VERSION,derivePayload(candidate));
		setGUID(new GUID(GUID.makeGuid()));
		
		//these fields should not be used when constructing the object this way,
		//but lets initialize them just in case
		_candidate = candidate.getAddress();
		
		//(stupid final beurocracy)
		QueryReply.IPPortCombo requestor;
		
		try {
			requestor = new QueryReply.IPPortCombo(
				NetworkUtils.ip2string(RouterService.getAddress()),
				RouterService.getPort()); 
		}catch (UnknownHostException yeah) {
			requestor=null;
		}
		_requestor = requestor;
		_distance = 0;
	}
	
	/**
	 * creates a message as a result of a received promotion request.
	 * This message is meant to be forwarded to the appropriate candidate advertiser
	 * Note: the check whether the message should be forwarded or not needs to happen
	 * elsewhere. 
	 * @param original the message received from someone on the network.
	 */
	public PromotionRequestVendorMessage(PromotionRequestVendorMessage original) {
		super(F_LIME_VENDOR_ID,F_PROMOTION_REQUEST,VERSION,derivePayload(original));
		setGUID(new GUID(original.getGUID()));
		_candidate = original.getCandidate();
		_requestor = original.getRequestor();
		_distance = original.getDistance()+1;
	}
	
	/**
	 * constructor which creates a vendor message with the specified attributes.
	 * It does not check for valididty because its meant only for testing.
	 * @param requestor the requestor of the promotion
	 * @param candidate the candidate for promotion
	 * @param distance how long has the message travelled.
	 */
	public PromotionRequestVendorMessage(QueryReply.IPPortCombo candidate,
							QueryReply.IPPortCombo requestor,
							int distance){
		super(F_LIME_VENDOR_ID, F_PROMOTION_REQUEST,VERSION,derivePayload(requestor,candidate,distance));
		_requestor = requestor;
		_candidate= candidate;
		_distance = distance;
	}
	
	
	protected static byte [] derivePayload(Candidate candidate) {
		byte [] payload = new byte[13];
		
		//the first byte is 0 in this case
		payload[0]=0;
		
		//the next 6 bytes are the serialized candidate
		System.arraycopy(candidate.toBytes(),0,payload,1,6);
		
		//the last 6 bytes are our address
		System.arraycopy(RouterService.getExternalAddress(),0,payload,7,4);
		ByteOrder.short2leb((short)RouterService.getPort(),payload,11);  
		
		return payload;
	}
	
	protected static byte [] derivePayload(PromotionRequestVendorMessage other) {
		byte [] ret = new byte [13];
		
		byte [] received = other.getPayload();
		
		//keep everything the same except the distance value
		System.arraycopy(received,1,ret,1,12);
		
		//which is incremented by 1
		int distance = received[0];  
		distance++;
		ret[0] = (byte)distance;
		
		return ret;
	}
	
	protected static byte[] derivePayload(QueryReply.IPPortCombo requestor,
								QueryReply.IPPortCombo candidate,
								int distance) {
		byte []res = new byte[13];
		
		//put in the values blindly - so never use this other than for testing.
		res[0] = (byte)distance;
		System.arraycopy(candidate.toBytes(),0,res,1,6);
		System.arraycopy(requestor.toBytes(),0,res,7,6);
		return res;
	}
	/**
	 * @return Returns the _candidate.
	 */
	public QueryReply.IPPortCombo getCandidate() {
		return _candidate;
	}
	/**
	 * @return Returns the _distance.
	 */
	public int getDistance() {
		return _distance;
	}
	/**
	 * @return Returns the _requestor.
	 */
	public QueryReply.IPPortCombo getRequestor() {
		return _requestor;
	}

}
