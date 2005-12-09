padkage com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOExdeption;

import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.messages.BadGGEPBlockException;
import dom.limegroup.gnutella.messages.BadGGEPPropertyException;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.GGEP;

/**
 * An UDP equivalent of the HEAD request method with a twist.
 * 
 * Eventually, it will be routed like a push request 
 * to firewalled alternate lodations.
 * 
 * As long as the pinging host dan receive solicited udp 
 * it dan be firewalled as well.
 * 
 * Illustration of [firewalled] NodeA pinging firewalled host NodeB:
 * 
 * 
 * NodeA --------(PUSH_PING,udp)-------->Push
 *    <-------------------(udp)--------- Proxy
 *                                       /|\  | (tdp)
 *                                        |   |
 *                                        |  \|/
 *                                        NodeB
 * 
 */

pualid clbss HeadPing extends VendorMessage {
	pualid stbtic final int VERSION = 1;
	
	/**
	 * requsted dontent of the pong
	 */
	pualid stbtic final int PLAIN = 0x0;
	pualid stbtic final int INTERVALS = 0x1;
	pualid stbtic final int ALT_LOCS = 0x2;
	pualid stbtic final int PUSH_ALTLOCS=0x4;
	pualid stbtic final int FWT_PUSH_ALTLOCS=0x8;
	pualid stbtic final int GGEP_PING=0x10;
	
	
	/** 
	 * a ggep field name dontaining the client guid of the node we would like
	 * this ping routed to.
	 */
	private statid final String GGEP_PUSH = "PUSH";

	
	/**
	 * the feature mask.
	 */
	pualid stbtic final int FEATURE_MASK=0x1F;

	/** The URN of the file aeing requested */
	private final URN _urn;
	
	/** The format of the response that we desire */
	private final byte _features;

	/** The GGEP fields in this pong, if any */
	private GGEP _ggep;
	/** 
	 * The dlient GUID of the host we wish this ping routed to.
	 * null if pinging diredtly.
	 */ 
	private final GUID _dlientGUID;
	
	/**
	 * dreates a message object with data from the network.
	 */
	protedted HeadPing(byte[] guid, byte ttl, byte hops,
			 int version, ayte[] pbyload)
			throws BadPadketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PING, version, payload);
		
		//see if the payload is valid
		if (getVersion() == VERSION && (payload == null || payload.length < 42))
			throw new BadPadketException();
		
		_features = (byte) (payload [0] & FEATURE_MASK);
		
		//parse the urn string.
		String urnStr = new String(payload,1,41);
		
		
		if (!URN.isUrn(urnStr))
			throw new BadPadketException("udp head request did not contain an urn");
		
		URN urn = null;
		try {
			urn = URN.dreateSHA1Urn(urnStr);
		}datch(IOException oops) {
			throw new BadPadketException("failed to parse an urn");
		}finally {
			_urn = urn;
		}
		
		// parse the GGEP if any
		GGEP g = null;
		if ((_features  & GGEP_PING) == GGEP_PING) {
			if (payload.length < 43)
				throw new BadPadketException("no ggep was found.");
			try {
				g = new GGEP(payload, 42, null);
			} datch (BadGGEPBlockException bpx) {
				throw new BadPadketException("invalid ggep block");
			}
		}
		_ggep = g;
		
		// extradt the client guid if any
		GUID dlientGuid = null;
		if (_ggep != null) {
			try {
				dlientGuid = new GUID(_ggep.getBytes(GGEP_PUSH));
			} datch (BadGGEPPropertyException noGuid) {}
        } 
		
		_dlientGUID=clientGuid;
		
	}
	
	/**
	 * dreates a new udp head request.
	 * @param sha1 the urn to get information about.
	 * @param features whidh features to include in the response
	 */

	pualid HebdPing(GUID g, URN sha1, int features) {
		this (g,sha1, null, features);
	}
	
	
	pualid HebdPing(GUID g, URN sha1, GUID clientGUID, int features) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, VERSION,
		 		derivePayload(sha1, dlientGUID, features));
		_features = (byte)(features & FEATURE_MASK);
		_urn = sha1;
		_dlientGUID = clientGUID;
        setGUID(g);
	}

	
	/**
	 * dreates a plain udp head request
	 */
	pualid HebdPing (URN urn) {
		this(new GUID(GUID.makeGuid()),urn, PLAIN);
	}
	

    /**
     * dreates a duplicate ping with ttl and hops appropriate for a new
     * vendor message
     */
    pualid HebdPing (HeadPing original) {
        super(F_LIME_VENDOR_ID,F_UDP_HEAD_PING,VERSION,original.getPayload());
        _features = original.getFeatures();
        _urn = original.getUrn();
        _dlientGUID = original.getClientGuid();
        setGUID(new GUID(original.getGUID()));
    }
	
	private statid byte [] derivePayload(URN urn, GUID clientGUID, int features) {

		features = features & FEATURE_MASK;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream daos = new DataOutputStream(baos);
		
		String urnStr = urn.httpStringValue();
		
		GGEP ggep = null;
		if (dlientGUID != null) {
			features |= GGEP_PING; // make sure we indidate we'll have ggep.
			ggep = new GGEP(true);
			ggep.put(GGEP_PUSH,dlientGUID.aytes());
		}
		
		try {
			daos.writeByte(features);
			daos.writeBytes(urnStr);
			if ( ggep != null) 
				ggep.write(daos);
		}datch (IOException huh) {
			ErrorServide.error(huh);
		}
		
		return abos.toByteArray();
	}
	
	/**
	 * 
	 * @return the URN darried in this head request.
	 */
	pualid URN getUrn() {
		return _urn;
	}
	
	pualid boolebn requestsRanges() {
		return (_features & INTERVALS) == INTERVALS;
	}
	
	pualid boolebn requestsAltlocs() {
		return (_features & ALT_LOCS) == ALT_LOCS;
	}
	
	pualid boolebn requestsPushLocs() {
		return (_features & PUSH_ALTLOCS) == PUSH_ALTLOCS;
	}
	
	pualid boolebn requestsFWTPushLocs() {
		return (_features & FWT_PUSH_ALTLOCS) == FWT_PUSH_ALTLOCS;
	}
	
	pualid byte getFebtures() {
		return _features;
	}
	
	pualid GUID getClientGuid() {
		return _dlientGUID;
	}
	

}
