
package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.GUID;

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
	private int _number;
	
	/**
	 * constructs a new ultrapeer request message.
	 * @param guid the guid of the message
	 * @param number the number of ultrapeers desired
	 */
	public GiveUPVendorMessage(GUID guid, int number) {
	      super(F_LIME_VENDOR_ID, F_GIVE_ULTRAPEER, VERSION,
	            derivePayload(number));
	      setGUID(guid);
	      _number = number;
	}
	
	
	
	/**
	 * constructs a new ultrapeer request message, asking for
	 * all ultrapeers of the other guy.
	 * @param guid the guid of the message
	 */
	public GiveUPVendorMessage(GUID guid) {
	      this(guid,ALL);
	}
	
	private static byte [] derivePayload(int number) {
		//we don't expect to have more than 255 UP connections soon
		byte [] payload = new byte[1];
		payload[0] = (byte)number;
		return payload;
	}
	
	/**
	 * see superclass comment
	 * 
	 */
	public GiveUPVendorMessage(byte[] guid, byte ttl, byte hops,
			byte[] vendorID, int selector, int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, vendorID, selector, version, payload);
		//see if the payload is valid
		if (payload == null || payload.length != 1)
			throw new BadPacketException();
		
		_number = payload[0];
		if (_number < ALL) //corrupted packet
			throw new BadPacketException();
		
	}
	
	
	/**
	 * @return Returns the number of UP neighbor addresses that were requested
	 * with this message.
	 */
	public int getNumber() {
		return _number;
	}
}
