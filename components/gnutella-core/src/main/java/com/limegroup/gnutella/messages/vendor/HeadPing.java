package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;

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
	public static final int PUSH_PING=0x10;

	
	/**
	 * the feature mask.
	 */
	public static final int FEATURE_MASK=0x1F;

	/** The URN of the file being requested */
	private final URN _urn;
	
	/** The format of the response that we desire */
	private final byte _features;
	
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
		if (getVersion() == VERSION && (payload == null || payload.length < 41))
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
		
		// parse the client guid if any
		GUID g;
		if ((_features & PUSH_PING) == PUSH_PING) {
			if (payload.length < 57)
				throw new BadPacketException();
			
            byte [] guidBytes = new byte[16]; 
            System.arraycopy(payload,42,guidBytes,0,16); 
            g = new GUID(guidBytes); 
        } 
        else g=null;
		
		_clientGUID=g;
		
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

		String urnStr = urn.httpStringValue();
		int urnlen = urnStr.getBytes().length;

		int totalLen = urnlen+1;
		
		if (clientGUID != null)
			totalLen += clientGUID.bytes().length;
		
		byte []ret = new byte[totalLen];
		
		ret[0]=(byte)features;
		
		System.arraycopy(urnStr.getBytes(),0,ret,1,urnlen);
		
		if (clientGUID!=null) 
            System.arraycopy(clientGUID.bytes(),0,ret,urnlen+1,16);

		return ret;
	}
	
	/** 
     * creates a ping that should be forwarded to a shielded leaf. 
     * both messages have the same guid. 
     *  
     * @param original the original ping received from the pinger 
     * @return the new ping, with stripped clientGuid and updated features. 
     */ 
    public static HeadPing createForwardPing(HeadPing original) { 
         
        HeadPing ret =  
            new HeadPing(original.getUrn(), 
                    original.getFeatures() & ~PUSH_PING); 
         
        ret.setGUID(new GUID(original.getGUID())); 
         
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
