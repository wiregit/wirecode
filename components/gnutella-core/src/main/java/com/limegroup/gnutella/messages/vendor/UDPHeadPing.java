
package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.BadPacketException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * An UDP equivalent of the HEAD request method.
 * 
 */

public class UDPHeadPing extends VendorMessage {
	public static final int VERSION = 1;
	
	public static final int PLAIN = 0x0;
	public static final int INTERVALS = 0x1;
	public static final int ALT_LOCS = 0x2;
	
	public static final int FEATURE_MASK=0x3;
	
	private final URN _urn;
	
	private final byte _features;
	
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
		
		_features = (byte) (payload [0] & FEATURE_MASK);
		
		String urnStr = null;
		try {
			urnStr = new String(payload,1,payload.length-1,"US-ASCII");
		}catch(UnsupportedEncodingException impossible) {
			ErrorService.error(impossible);
		}
		
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
	public UDPHeadPing(URN urn, int features) {
		 super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, VERSION,
		 		derivePayload(urn, features));
		 _urn = urn;
		 _features = (byte) (features & FEATURE_MASK);
	}
	
	
	private static byte [] derivePayload(URN urn, int features) {
		features = features & FEATURE_MASK;
		String urnStr = urn.httpStringValue();
		byte [] ret = new byte[urnStr.length()+1];
		ret[0]=(byte)features;
		System.arraycopy(urnStr.getBytes(),0,ret,1,ret.length-1);
		return ret;
	}
	
	/**
	 * 
	 * @return the URN carried in this head request.
	 */
	public URN getUrn() {
		return _urn;
	}
	
	public boolean requestsRanges() {
		return (_features & INTERVALS) == INTERVALS;
	}
	
	public boolean requestsAltlocs() {
		return (_features & ALT_LOCS) == ALT_LOCS;
	}
	
	public byte getFeatures() {
		return _features;
	}
}
