/*
 * a message which contains this ultrapeer's best leaf and the best leaf of the
 * ultrapeers at hops 1.
 *
 * GUID really doesn't matter, since it will be sent at a specific rate. 
 */
package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.upelection.*;

import java.io.*;
import java.text.ParseException;


public class BestCandidatesVendorMessage extends VendorMessage {
	
	public static final int VERSION = 1;
	
	private Candidate [] _bestCandidates;
	
	/**
	 * creates a new message containing the best candidate ultrapeers at 0 1 hops
	 * respectively.  They are in serialized ExtendedEndpoint form.
	 * 
	 * @param bestCandidates an array of ExtendedEndpoints.  If we only have data about our
	 * own best leaf, make the second element null.
	 */
	public BestCandidatesVendorMessage(Candidate []bestCandidates) {
		super(F_LIME_VENDOR_ID, F_BEST_CANDIDATE, VERSION, derivePayload(bestCandidates));
		_bestCandidates = bestCandidates;
	}
	
	
	
	private static byte [] derivePayload(Candidate []bestCandidates){
		
		byte [] networkData;
		
		if (bestCandidates[1]!=null) { 
			networkData = new byte [16];
			System.arraycopy(bestCandidates[1].toBytes(),0,networkData,8,8);
		}
		else 
			networkData = new byte[8];

		System.arraycopy(bestCandidates[0].toBytes(),0,networkData,0,8); 
		
		return networkData;
	}
	
	
	/**
	 * see superclass
	 */
	protected BestCandidatesVendorMessage(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_BEST_CANDIDATE, version, payload);
		parseCandidates(payload);
	}
	
	
	/**
	 * parses the best candidates as reported by the peer.  
	 * @throws BadPacketException if the data is invalid
	 */
	private void parseCandidates(byte [] payload) throws BadPacketException {
		
		//check if the message has the proper size payload
		if (payload==null || 
				!(payload.length==8 || payload.length==16))
			throw new BadPacketException("invalid length payload for message with GUID "+new String(getGUID()));
		
		_bestCandidates = new Candidate[2];
		
		//we have at least one candidate
		_bestCandidates[0] = new Candidate(payload,0);
		
		//if the size of the payload is 16 we have two candidates
		if (payload.length==16) 
			_bestCandidates[1]= new Candidate(payload,8);
		else //otherwise the other peer advertised just a single candidate
			_bestCandidates[1]=null;
		
	}
	/**
	 * @return Returns the _bestCandidates.
	 */
	public Candidate[] getBestCandidates() {
		return _bestCandidates;
	}
}
