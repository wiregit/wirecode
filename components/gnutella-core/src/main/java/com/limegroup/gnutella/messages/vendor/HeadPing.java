package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.BadPacketException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * An UDP equivalent of the HEAD request method with a twist.
 * 
 * Eventually, it will be routed like a push request 
 * to firewalled alternate locations.
 * 
 * As long as the pinging host can receive solicited udp 
 * it can be firewalled as well.
 * 
 * Illustration of NodeA pinging firewalled host NodeB:
 * 
 * 
 * NodeB --------(PUSH_PING,udp)-------->Push
 *    <-------------------(udp)--------- Proxy
 *                                       /|\  | (tcp)
 *                                        |   |
 *                                        |  \|/
 *                                        NodeB
 * 
 */

public class HeadPing extends VendorMessage {
	public static final int VERSION = 1;
	
	/**
	 * requsted content of the pong
	 */
	public static final int PLAIN = 0x0;
	public static final int INTERVALS = 0x1;
	public static final int ALT_LOCS = 0x2;
	public static final int PUSH_ALTLOCS=0x4;
	public static final int FWT_PUSH_ALTLOCS=0x8;
	
	
	
	/**
	 * the feature mask.
	 */
	public static final int FEATURE_MASK=0xF;
	
	private final URN _urn;
	
	private final byte _features;
	
	
	
	/**
	 * creates a message object with data from the network.
	 */
	protected HeadPing(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PING, version, payload);
		
		//see if the payload is valid
		if (getVersion() == VERSION && (payload == null || payload.length < 41))
			throw new BadPacketException();
		
		byte features;
		features = (byte) (payload [0] & FEATURE_MASK);
		
		
		_features = features;
		
		
		//parse the urn string.
		String urnStr = new String(payload,1,41);
		
		
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
	 * @param features which features to include in the response
	 */
	public HeadPing(URN sha1, int features) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, VERSION,
		 		derivePayload(sha1, features));
		
		_features = (byte)(features & FEATURE_MASK);
		
		_urn = sha1;
	}
	

	
	
	/**
	 * creates a plain udp head request
	 */
	public HeadPing (URN urn) {
		this(urn, PLAIN);
	}
	
	
	private static byte [] derivePayload(URN urn, int features) {
		 
		
		features = features & FEATURE_MASK;
		
		
		String urnStr = urn.httpStringValue();
		int urnlen = urnStr.getBytes().length;
		int totalLen = urnlen+1;
		
		byte []ret = new byte[totalLen];
		
		ret[0]=(byte)features;
		
		System.arraycopy(urnStr.getBytes(),0,ret,1,urnlen);
		
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
	
	public boolean requestsPushLocs() {
		return (_features & PUSH_ALTLOCS) == PUSH_ALTLOCS;
	}
	
	public boolean requestsFWTPushLocs() {
		return (_features & FWT_PUSH_ALTLOCS) == FWT_PUSH_ALTLOCS;
	}
	
	public byte getFeatures() {
		return _features;
	}
	

}
