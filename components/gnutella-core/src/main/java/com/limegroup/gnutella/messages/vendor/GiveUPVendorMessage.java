
package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.GUID;

import com.limegroup.gnutella.*;

/**
 * a request for a given ultrapeer's ultrapeer connections.
 * Useful for crawling.
 * 
 * Currently it doesn't do any validation of the source, i.e. we 
 * sent our list of ultrapeer to whomever requests it.
 */
public class GiveUPVendorMessage extends VendorMessage {
	
	public static final int VERSION = 1;
	
	public static final int ALL=-1;
	
	/**
	 * the number of requested ultrapeer results.  
	 */
	private int _numberUP;
	
	/**
	 * the number of requested leaf results.  
	 */
	private int _numberLeaves;
	
	/**
	 * constructs a new ultrapeer request message.
	 * @param guid the guid of the message
	 * @param number the number of ultrapeers desired
	 */
	public GiveUPVendorMessage(GUID guid, int numberUP, int numberLeaves) {
	      super(F_LIME_VENDOR_ID, F_GIVE_ULTRAPEER, VERSION,
	            derivePayload(numberUP,numberLeaves));
	      setGUID(guid);
	      _numberUP = numberUP;
	      _numberLeaves = numberLeaves;
	}
	
	
	
	/**
	 * constructs a new ultrapeer request message, asking for
	 * all ultrapeers and leafs of the other guy.
	 * @param guid the guid of the message
	 */
	public GiveUPVendorMessage(GUID guid) {
	      this(guid,ALL,ALL);
	}
	
	private static byte [] derivePayload(int numberUP, int numberLeaves) {
		//we don't expect to have more than 255 connections soon

		if (numberUP > 255)
			numberUP=255;
		if (numberLeaves > 255)
			numberLeaves=255;
		
		byte [] temp = new byte [2];
		byte [] payload = new byte[2];
		
		ByteOrder.short2leb((short)numberUP,temp,0);
		payload[0]=temp[0];
		ByteOrder.short2leb((short)numberLeaves,temp,0);
		payload[1] = temp[0];
		return payload;
	}
	
	/**
	 * see superclass comment
	 * 
	 * note this does not have upper limit to the number of requested results 
	 * (other than the 255 byte limit).  One day we may have many more connections..
	 */
	protected GiveUPVendorMessage(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_GIVE_ULTRAPEER, version, payload);
		
		//see if the payload is valid
		if (payload == null || payload.length != 2)
			throw new BadPacketException();
		
		_numberUP = ByteOrder.ubyte2int(payload[0]);
		_numberLeaves = ByteOrder.ubyte2int(payload[1]);
		
		if (_numberUP < ALL || _numberLeaves < ALL) //corrupted packet
			throw new BadPacketException();
		
	}
	
	
	/**
	 * @return Returns the number of UP neighbor addresses that were requested
	 * with this message.
	 */
	public int getNumberUP() {
		return _numberUP;
	}
	
	/**
	 * @return Returns the number of Leaf neighbor addresses that were requested
	 * with this message.
	 */
	public int getNumberLeaves() {
		return _numberLeaves;
	}
}
