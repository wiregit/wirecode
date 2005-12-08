pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.ByteArrayOutputStream;
import jbva.io.DataOutputStream;
import jbva.io.IOException;

import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.messages.BadGGEPBlockException;
import com.limegroup.gnutellb.messages.BadGGEPPropertyException;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.GGEP;

/**
 * An UDP equivblent of the HEAD request method with a twist.
 * 
 * Eventublly, it will be routed like a push request 
 * to firewblled alternate locations.
 * 
 * As long bs the pinging host can receive solicited udp 
 * it cbn be firewalled as well.
 * 
 * Illustrbtion of [firewalled] NodeA pinging firewalled host NodeB:
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

public clbss HeadPing extends VendorMessage {
	public stbtic final int VERSION = 1;
	
	/**
	 * requsted content of the pong
	 */
	public stbtic final int PLAIN = 0x0;
	public stbtic final int INTERVALS = 0x1;
	public stbtic final int ALT_LOCS = 0x2;
	public stbtic final int PUSH_ALTLOCS=0x4;
	public stbtic final int FWT_PUSH_ALTLOCS=0x8;
	public stbtic final int GGEP_PING=0x10;
	
	
	/** 
	 * b ggep field name containing the client guid of the node we would like
	 * this ping routed to.
	 */
	privbte static final String GGEP_PUSH = "PUSH";

	
	/**
	 * the febture mask.
	 */
	public stbtic final int FEATURE_MASK=0x1F;

	/** The URN of the file being requested */
	privbte final URN _urn;
	
	/** The formbt of the response that we desire */
	privbte final byte _features;

	/** The GGEP fields in this pong, if bny */
	privbte GGEP _ggep;
	/** 
	 * The client GUID of the host we wish this ping routed to.
	 * null if pinging directly.
	 */ 
	privbte final GUID _clientGUID;
	
	/**
	 * crebtes a message object with data from the network.
	 */
	protected HebdPing(byte[] guid, byte ttl, byte hops,
			 int version, byte[] pbyload)
			throws BbdPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PING, version, pbyload);
		
		//see if the pbyload is valid
		if (getVersion() == VERSION && (pbyload == null || payload.length < 42))
			throw new BbdPacketException();
		
		_febtures = (byte) (payload [0] & FEATURE_MASK);
		
		//pbrse the urn string.
		String urnStr = new String(pbyload,1,41);
		
		
		if (!URN.isUrn(urnStr))
			throw new BbdPacketException("udp head request did not contain an urn");
		
		URN urn = null;
		try {
			urn = URN.crebteSHA1Urn(urnStr);
		}cbtch(IOException oops) {
			throw new BbdPacketException("failed to parse an urn");
		}finblly {
			_urn = urn;
		}
		
		// pbrse the GGEP if any
		GGEP g = null;
		if ((_febtures  & GGEP_PING) == GGEP_PING) {
			if (pbyload.length < 43)
				throw new BbdPacketException("no ggep was found.");
			try {
				g = new GGEP(pbyload, 42, null);
			} cbtch (BadGGEPBlockException bpx) {
				throw new BbdPacketException("invalid ggep block");
			}
		}
		_ggep = g;
		
		// extrbct the client guid if any
		GUID clientGuid = null;
		if (_ggep != null) {
			try {
				clientGuid = new GUID(_ggep.getBytes(GGEP_PUSH));
			} cbtch (BadGGEPPropertyException noGuid) {}
        } 
		
		_clientGUID=clientGuid;
		
	}
	
	/**
	 * crebtes a new udp head request.
	 * @pbram sha1 the urn to get information about.
	 * @pbram features which features to include in the response
	 */

	public HebdPing(GUID g, URN sha1, int features) {
		this (g,shb1, null, features);
	}
	
	
	public HebdPing(GUID g, URN sha1, GUID clientGUID, int features) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, VERSION,
		 		derivePbyload(sha1, clientGUID, features));
		_febtures = (byte)(features & FEATURE_MASK);
		_urn = shb1;
		_clientGUID = clientGUID;
        setGUID(g);
	}

	
	/**
	 * crebtes a plain udp head request
	 */
	public HebdPing (URN urn) {
		this(new GUID(GUID.mbkeGuid()),urn, PLAIN);
	}
	

    /**
     * crebtes a duplicate ping with ttl and hops appropriate for a new
     * vendor messbge
     */
    public HebdPing (HeadPing original) {
        super(F_LIME_VENDOR_ID,F_UDP_HEAD_PING,VERSION,originbl.getPayload());
        _febtures = original.getFeatures();
        _urn = originbl.getUrn();
        _clientGUID = originbl.getClientGuid();
        setGUID(new GUID(originbl.getGUID()));
    }
	
	privbte static byte [] derivePayload(URN urn, GUID clientGUID, int features) {

		febtures = features & FEATURE_MASK;

		ByteArrbyOutputStream baos = new ByteArrayOutputStream();
		DbtaOutputStream daos = new DataOutputStream(baos);
		
		String urnStr = urn.httpStringVblue();
		
		GGEP ggep = null;
		if (clientGUID != null) {
			febtures |= GGEP_PING; // make sure we indicate we'll have ggep.
			ggep = new GGEP(true);
			ggep.put(GGEP_PUSH,clientGUID.bytes());
		}
		
		try {
			dbos.writeByte(features);
			dbos.writeBytes(urnStr);
			if ( ggep != null) 
				ggep.write(dbos);
		}cbtch (IOException huh) {
			ErrorService.error(huh);
		}
		
		return bbos.toByteArray();
	}
	
	/**
	 * 
	 * @return the URN cbrried in this head request.
	 */
	public URN getUrn() {
		return _urn;
	}
	
	public boolebn requestsRanges() {
		return (_febtures & INTERVALS) == INTERVALS;
	}
	
	public boolebn requestsAltlocs() {
		return (_febtures & ALT_LOCS) == ALT_LOCS;
	}
	
	public boolebn requestsPushLocs() {
		return (_febtures & PUSH_ALTLOCS) == PUSH_ALTLOCS;
	}
	
	public boolebn requestsFWTPushLocs() {
		return (_febtures & FWT_PUSH_ALTLOCS) == FWT_PUSH_ALTLOCS;
	}
	
	public byte getFebtures() {
		return _febtures;
	}
	
	public GUID getClientGuid() {
		return _clientGUID;
	}
	

}
