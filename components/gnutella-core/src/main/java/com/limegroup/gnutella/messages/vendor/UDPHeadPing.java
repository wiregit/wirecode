
package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.BadPacketException;
import java.io.IOException;

/**
 * An UDP equivalent of the HEAD request method.
 * 
 */

public class UDPHeadPing extends VendorMessage {
	public static final int VERSION = 1;
	
	private final URN _urn;
	
	/**
	 * creates a message object with data from the network.
	 */
	protected UDPHeadPing(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PING, version, payload);
		
		//see if the payload is valid
		if (getVersion() == VERSION && (payload == null || payload.length == 0))
			throw new BadPacketException();
		
		String urnStr = new String(payload);
		
		if (!URN.isUrn(urnStr))
			throw new BadPacketException("udp head request did not contain an urn");
		
		URN urn = null;
		try {
			urn = URN.createSHA1Urn(urnStr);
		}catch(IOException oops) {
			throw new BadPacketException("failed to parse an urn");
		}finally {
			_urn = urn;
		}
		
	}
	
	/**
	 * creates a new udp head request.
	 * @param sha1 the urn to get information about.
	 */
	public UDPHeadPing(URN urn) {
		 super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, VERSION,
		 		urn.toString().getBytes());
		 _urn = urn;
	}
	
	
	/**
	 * 
	 * @return the URN carried in this head request.
	 */
	public URN getUrn() {
		return _urn;
	}
}
