package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * An UDP equivalent of the HEAD request method with a twist.
 * 
 * Eventually, it will be routed like a push request 
 * to firewalled alternate locations.
 * 
 * As long as the pinging host can receive solicited udp 
 * it can be firewalled as well.
 * 
 * Illustration of [firewalled] NodeA pinging firewalled host NodeB:
 * 
 * 
 * NodeA --------(PUSH_PING,udp)-------->Push
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
	public static final int GGEP_PING=0x10;
	
	
	/** 
	 * a ggep field name containing the client guid of the node we would like
	 * this ping routed to.
	 */
	private static final String GGEP_PUSH = "PUSH";

	
	/**
	 * the feature mask.
	 */
	public static final int FEATURE_MASK=0x1F;

	/** The URN of the file being requested */
	private final URN _urn;
	
	/** The format of the response that we desire */
	private final byte _features;

	/** The GGEP fields in this pong, if any */
	private GGEP _ggep;
	/** 
	 * The client GUID of the host we wish this ping routed to.
	 * null if pinging directly.
	 */ 
	private final GUID _clientGUID;
	
	/**
	 * creates a message object with data from the network.
	 */
	protected HeadPing(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PING, version, payload);
		
		//see if the payload is valid
		if (getVersion() == VERSION && (payload == null || payload.length < 42))
			throw new BadPacketException();
		
		_features = (byte) (payload [0] & FEATURE_MASK);
		
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
		
		// parse the GGEP if any
		GGEP g = null;
		if ((_features  & GGEP_PING) == GGEP_PING) {
			if (payload.length < 43)
				throw new BadPacketException("no ggep was found.");
			try {
				g = new GGEP(payload, 42, null);
			} catch (BadGGEPBlockException bpx) {
				throw new BadPacketException("invalid ggep block");
			}
		}
		_ggep = g;
		
		// extract the client guid if any
		GUID clientGuid = null;
		if (_ggep != null) {
			try {
				clientGuid = new GUID(_ggep.getBytes(GGEP_PUSH));
			} catch (BadGGEPPropertyException noGuid) {}
        } 
		
		_clientGUID=clientGuid;
		
	}
	
	/**
	 * creates a new udp head request.
	 * @param sha1 the urn to get information about.
	 * @param features which features to include in the response
	 */

	public HeadPing(URN sha1, int features) {
		this (sha1, null, features);
	}
	
	
	public HeadPing(URN sha1, GUID clientGUID, int features) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, VERSION,
		 		derivePayload(sha1, clientGUID, features));
		_features = (byte)(features & FEATURE_MASK);
		_urn = sha1;
		_clientGUID = clientGUID;
	}

	
	/**
	 * creates a plain udp head request
	 */
	public HeadPing (URN urn) {
		this(urn, PLAIN);
	}
	

	
	private static byte [] derivePayload(URN urn, GUID clientGUID, int features) {

		features = features & FEATURE_MASK;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream daos = new DataOutputStream(baos);
		
		String urnStr = urn.httpStringValue();
		
		GGEP ggep = null;
		if (clientGUID != null) {
			features |= GGEP_PING; // make sure we indicate we'll have ggep.
			ggep = new GGEP(true);
			ggep.put(GGEP_PUSH,clientGUID.bytes());
		}
		
		try {
			daos.writeByte(features);
			daos.writeBytes(urnStr);
			if ( ggep != null) 
				ggep.write(daos);
		}catch (IOException huh) {
			ErrorService.error(huh);
		}
		
		return baos.toByteArray();
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
	
	public GUID getClientGuid() {
		return _clientGUID;
	}
	

}
