package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocDigest;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * An UDP equivalent of the HEAD request method with a twist.
 * 
 * It is routed like a push request to firewalled alternate locations. 
 * As long as the pinging host can receive solicited udp it can be 
 * firewalled as well.
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
 * The ping carries a GGEP map with one specific mapping - a mapping whose value
 * gives a hint of which other mappings can be found in the map.  This value is used
 * to convey information to the remote host about what features the pinger supports,
 * as well as any potential metadata about those features.
 * 
 * The value itself is a bitvector with unlimited length. If a bit N is set, then we might
 * find two other mappings in the map :
 * Nm - key containing metadata about the feature such as version, etc.
 * Nd - key containing actual data associated with the feature (optional).
 * 
 * For example, if a pinger wants to convey that it supports bloom filters up to version 2
 * and at the same time send its own bloom filter, and if the bit indicating bloom filter
 * support is the least-significant digit, the ggep map would contain the following:
 * 
 * "P" : 00000.....1
 * "1m" : 0x2
 * "1d" : <... binary data> 
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
	 * the feature mask.
	 */
	public static final int FEATURE_MASK=0x1F;
	
	//// Various constants related to the properties ggep //////
	/** 
	 * a ggep field name containing the client guid of the node we would like
	 * this ping routed to.
	 */
	private static final String GGEP_PUSH = "PUSH";
	
	/**
	 * the ggep field name containing the various ggep features supported by the headping
	 */
	static final String GGEP_PROPS = "P";
	
	/**
	 * a flag whose presence in the GGEP_PROPS value indicates the sender supports bloom filters.
	 * if the key has a value, than that value is the serialized filter.
	 */
	static final int GGEP_BLOOM = 0x1;
	static final int GGEP_PUSH_BLOOM = 0x1 << 1;
	
	//////////

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
	 * The bloom filter carried by this ping, if any
	 */
	private final AltLocDigest _digest,_pushDigest;
	
	/**
	 * Whether the pinger supports bloom filters.
	 */
	private final boolean _supportsBloom, _supportsPushBloom;
	
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
		boolean supportsBloom = false;
		boolean supportsPushBloom = false;
		if (_ggep != null) {
			try {
				clientGuid = new GUID(_ggep.getBytes(GGEP_PUSH));
			} catch (BadGGEPPropertyException noGuid) {}
			
			try {
			    byte [] props = _ggep.getBytes(GGEP_PROPS);
			    int propSet = (int)props[props.length-1];
			    if ((propSet & GGEP_BLOOM) == GGEP_BLOOM)
			        supportsBloom = true;
			    if ((propSet & GGEP_PUSH_BLOOM) == GGEP_PUSH_BLOOM)
			        supportsPushBloom = true;
			} catch (BadGGEPPropertyException bloomNotSupported) {}
			
			//TODO: parse bloom
        } 
		
        _clientGUID=clientGuid;
        _supportsBloom = supportsBloom;
        _supportsPushBloom = supportsPushBloom;
		_digest = null;
		_pushDigest = null;
	}
	
	/**
	 * creates a new udp head request.
	 * @param sha1 the urn to get information about.
	 * @param features which features to include in the response
	 */

	public HeadPing(URN sha1, int features) {
		this (sha1, null, null, features);
	}
	
	
	public HeadPing(URN sha1, GUID clientGUID, AltLocDigest []digest, int features) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, VERSION,
		 		derivePayload(sha1, clientGUID, digest, features));
		_features = (byte)(features & FEATURE_MASK);
		_urn = sha1;
		_clientGUID = clientGUID;
		
		if (digest == null || digest.length < 2) {
		    _digest = null;
		    _pushDigest = null;
		} else {
		    _digest = digest[0];
		    _pushDigest = digest[1];
		}
		
		_supportsBloom=true;
		_supportsPushBloom=true;
	}

	
	/**
	 * creates a plain udp head request
	 */
	public HeadPing (URN urn) {
		this(urn, PLAIN);
	}
	

	
	private static byte [] derivePayload(URN urn, GUID clientGUID, AltLocDigest []filter, int features) {

		features = features & FEATURE_MASK;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream daos = new DataOutputStream(baos);
		
		String urnStr = urn.httpStringValue();
		
		//make sure we indicate we'll have ggep.
		GGEP ggep = new GGEP(true);
		features |= GGEP_PING;
		
		// we always support bloom filters, even if we don't carry one
		ggep.put(GGEP_PROPS, new byte[]{(byte)(GGEP_BLOOM | GGEP_PUSH_BLOOM)});
		
		// is this a push ping?
		if (clientGUID != null) 
			ggep.put(GGEP_PUSH,clientGUID.bytes());  
		
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
	
	/**
	 * @return whether the pinger indicates that it supports altloc digests
	 */
	public boolean supportsDigests() {
	    return _supportsBloom; 
	}
	
	/**
	 * @return whether the pinger indicates that it supports pushloc digests
	 */
	public boolean supportsPushDigests() {
	    return _supportsPushBloom;
	}
	
	/**
	 * @return the altloc digest contained in this ping.  null if none
	 */
	public AltLocDigest getDigest() {
	    return _digest; 
	}
	
	/**
	 * @return the pushloc digest contained in this ping.  null if none
	 */
	public AltLocDigest getPushDigest() {
	    return _digest; 
	}

}
