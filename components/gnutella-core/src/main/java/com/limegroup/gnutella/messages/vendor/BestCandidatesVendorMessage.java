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
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		Writer wr = new OutputStreamWriter(bos);

		try{
			bestCandidates[0].write(wr); //my best leaf
			if (bestCandidates[1]!=null) 
				bestCandidates[1].write(wr); //best leaf at ttl 1
			wr.flush();
			bos.flush();
		}catch(IOException iox) {
			//weird.  can't really do much except report it.
			ErrorService.getErrorCallback().error(iox);
		}
		
		return bos.toByteArray();
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
		

		if (payload==null || payload.length==0)
			throw new BadPacketException("empty payload for message with GUID "+new String(getGUID()));
		
		_bestCandidates = new Candidate[2];
		
		//split the payload in two lines
		String both = new String(payload);
		
		if (both.indexOf("\n")==-1)
			throw new BadPacketException();
		
		if (both.indexOf("\n")==both.lastIndexOf("\n")) {
			//seems like we either have an invalid packet or the other side knows of only one peer.
			try{
				_bestCandidates[0] = new Candidate(both);
				_bestCandidates[1] = null;
			} catch (ParseException pex){
				//invalid packet
				throw new BadPacketException();
			}
		} //otherwise, parse both
		else {
			String ttl0 = both.substring(0,both.indexOf("\n"));
			String ttl1 = both.substring(both.indexOf("\n")+1,both.length());
		
			try {
				_bestCandidates[0] = new Candidate(ttl0);
				_bestCandidates[1] = new Candidate(ttl1);
			}catch(ParseException pex) {
				throw new BadPacketException();
			}
			
		}
	}
	/**
	 * @return Returns the _bestCandidates.
	 */
	public Candidate[] getBestCandidates() {
		return _bestCandidates;
	}
}
