/*
 * Created on Apr 5, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.limegroup.gnutella.upelection;

import java.net.UnknownHostException;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.vendor.BestCandidatesVendorMessage;
import com.sun.java.util.collections.Iterator;


/**
 * sends out a vendor message to all UP connections containing our best candidates.
 * ExtendedEndpoints are created from open ManagedConnection objects; the field dailyUptime in this 
 * case means the lifetime of the connection.
 */
public class CandidateAdvertiser implements Runnable {
	
	
	public void run() {
		
		//first update our best leaf, i.e. find the one with the longest uptime
		//that is also not firewalled, isGoodLeaf, etc.
		Candidate best = electBest();
		
		//if we don't have a candidate, do not advertise.
		if (best==null)
			return;
		
		
		BestCandidates.update(best);
		
		//create a vendor message with the new info
		BestCandidatesVendorMessage bcvm = new BestCandidatesVendorMessage(
				BestCandidates.getCandidates());
		
		//broadcast!
		for (Iterator iter = RouterService.getConnectionManager().getInitializedConnections().iterator();iter.hasNext();) {
			ManagedConnection current = (ManagedConnection)iter.next();
			if (current.isGoodUltrapeer() &&
					current.remoteHostSupportsBestCandidates() >= 1);
				current.send(bcvm);
		}
	}
	
	/**
	 * elects the best ultrapeer candidate amongst our leaf connections
	 */
	private Candidate electBest() {
		Candidate best = null;
		
		for (Iterator iter = RouterService.getConnectionManager().getInitializedClientConnections().iterator();
			iter.hasNext();){
			ManagedConnection current = (ManagedConnection)iter.next();
			if (current.isGoodLeaf() &&
					current.isStable() && 
					current.isLimeWire() &&
					current.isUDPCapable()) //unsolicited udp
				//add more criteria here
				try {
					Candidate currentCandidate = new Candidate(current);
					if (currentCandidate.compareTo(best) > 0)
						best = currentCandidate;
				}catch (UnknownHostException ignored) {
					//if the leaf doesn't have valid address it should be rightfully ignored.
				}
		}
		
		return best;
	}
}