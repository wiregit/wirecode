package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.BadPacketException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * An UDP equivalent of the HEAD request method with a twist.
 * 
 * It can be routed like a push request to firewalled alternate locations.
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
	
	
	/**
	 * whether this ping should be routed like a push request
	 */
	public static final int PUSH_PING=0x8;
	
	/**
	 * the feature mask.
	 */
	public static final int FEATURE_MASK=0xF;
	
	private final URN _urn;
	
	private final byte _features;
	
	private final GUID _guid;
	
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
		
		//parse the client guid if this is a push request
		if ((_features & PUSH_PING) == PUSH_PING) {
			byte [] guidBytes = new byte[16];
			System.arraycopy(payload,42,guidBytes,0,16);
			_guid = new GUID(guidBytes);
		}
		else _guid=null;
		
	}
	
	/**
	 * creates a new udp head request.
	 * @param sha1 the urn to get information about.
	 * @param features which features to include in the response
	 */
	public HeadPing(URN urn, int features) {
		 this (urn, null, features);
	}
	
	/**
	 * creates a HeadPing that will be routed like a push request once it
	 * reaches the push proxy.  
	 * 
	 * @param sha1 the sha1 urn of the file we want info about
	 * @param clientGuid the clientGuid of the firewalled host
	 * @param features the format of the pong.  
	 */
	public HeadPing(URN sha1,GUID clientGuid,int features) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, VERSION,
		 		derivePayload(sha1, features,clientGuid));
		
		//make sure the push flag is set if pushing
		if (clientGuid!=null)
			features = features | PUSH_PING;
		
		features = features & FEATURE_MASK;
		
		_features = (byte)features;
		_guid =clientGuid;
		_urn = sha1;
		
	}
	
	
	/**
	 * creates a plain udp head request
	 */
	public HeadPing (URN urn) {
		this(urn, PLAIN);
	}
	
	/**
	 * creates a ping that should be forwarded to a shielded leaf.
	 * both messages have the same guid.
	 * 
	 * @param original the original ping received from the pinger
	 * @return the new ping, with stripped clientGuid and updated features.
	 */
	public HeadPing createForwardPing(HeadPing original) {
		
		HeadPing ret = 
			new HeadPing(original.getUrn(),
					original.getFeatures() & ~PUSH_PING);
		
		ret.setGUID(new GUID(original.getGUID()));
		
		return ret;
	}
	
	private static byte [] derivePayload(URN urn, int features, GUID clientGuid) {
		 
		//make sure the push flag is set if pushing
		if (clientGuid!=null)
			features = features | PUSH_PING;
		
		features = features & FEATURE_MASK;
		
		
		String urnStr = urn.httpStringValue();
		int urnlen = urnStr.getBytes().length;
		int totalLen = urnlen;
		totalLen= totalLen+ (clientGuid!=null ? 
					clientGuid.bytes().length+1 : 1);
		
		byte []ret = new byte[totalLen];
		
		ret[0]=(byte)features;
		
		System.arraycopy(urnStr.getBytes(),0,ret,1,urnlen);
		
		if (clientGuid!=null)
			System.arraycopy(clientGuid.bytes(),0,ret,urnlen+1,16);
		
		
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
	
	public byte getFeatures() {
		return _features;
	}
	
	/**
	 * 
	 * @return the client guid that this ping should be 
	 * forwarded to.  if null, this is not a push ping.
	 */
	public GUID getClientGuid() {
		return _guid;
	}
}
