/*
 * An ACK used for checking whether a promotion request was issued
 * and a response that it was or wasn't.
 * 
 * doesn't have any fields or state really.
 */
package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.*;


public class PromotionACKVendorMessage extends VendorMessage {
	
	public static final int VERSION = 1;
	
	/**
	 * constructs an ACK with data from the network
	 */
	public PromotionACKVendorMessage(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_PROMOTION_ACK, VERSION, payload);
		
		//there should be no payload.
		if (getVersion()==VERSION && payload.length!=0)
			throw new BadPacketException("payload in promotion ACK");
		
		//if a new version introduces payload..well.. ignore it.
	}
	
	/**
	 * creates a new ACK message.
	 */
	public PromotionACKVendorMessage(GUID guid) {
		super(F_LIME_VENDOR_ID,F_PROMOTION_ACK,VERSION,new byte[0]);
		setGUID(guid);
	} 
	
}
