/*
 * Created on Apr 5, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.limegroup.gnutella.upelection;

import java.net.UnknownHostException;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.sun.java.util.collections.Comparator;
import com.sun.java.util.collections.Iterator;


/**
 * As an UP:
 * sends out a vendor message to all UP connections containing our best candidates.
 * ExtendedEndpoints are created from open ManagedConnection objects; the field dailyUptime in this 
 * case means the lifetime of the connection.
 * 
 * As a leaf:
 * sends out Features VMs to the connected UPs at specific intervals.
 */
public class CandidateAdvertiser implements Runnable {
	
	ConnectionComparator _comparator = new ConnectionComparator();
	
	public void run() {
		
		//the vendor message to be sent out.
		VendorMessage vm;
		
		//if we are a leaf we send out our features.
		if (!RouterService.isSupernode())		
			vm = new FeaturesVendorMessage();
		
		else {
			//first update our best leaf, i.e. find the one with the longest uptime
			//that is also not firewalled, isGoodLeaf, etc.
			Candidate best = electBest();
		
			//if we don't have a candidate, do not advertise.
			if (best==null)
				return;
		
			//update our candidates table
			BestCandidates.update(best);
		
			//create a vendor message with the new info
			vm = new BestCandidatesVendorMessage(BestCandidates.getCandidates());
		}
		
		//broadcast!
		for (Iterator iter = RouterService.getConnectionManager().getInitializedConnections().iterator();iter.hasNext();) {
			ManagedConnection current = (ManagedConnection)iter.next();
			if (current.isGoodUltrapeer() &&
					current.remoteHostSupportsBestCandidates() >= 1);	
				current.send(vm);
		}
	}
	
	/**
	 * elects the best ultrapeer candidate amongst our leaf connections
	 */
	private Candidate electBest() {
		ManagedConnection best = null;
		
		for (Iterator iter = RouterService.getConnectionManager().getInitializedClientConnections().iterator();
			iter.hasNext();){
			ManagedConnection current = (ManagedConnection)iter.next();
			if (current.isGoodLeaf() &&
					current.isStable() && 
					current.remoteHostSupportsBestCandidates() >=1 &&
					current.isUDPCapable() && //unsolicited udp
					current.hasRequestedOOB() && //double-check
					current.isTCPCapable() ) //incoming tcp
					//add more criteria here
				{
					//filter out any old windowses
					if (current.getOS().startsWith("windows") && 
						current.getOS().indexOf("xp") ==-1 &&
						current.getOS().indexOf("2000") ==-1)
						continue;
					
					//and mac os 9
					if (current.getOS().startsWith("mac os") &&
							!current.getOS().endsWith("x"))
						continue;
					
					//also, jre 1.4.0 is really really bad
					if (current.getJVM().indexOf("1.4.0") != -1)
						continue;
				
					//get the best connection.	
					if (_comparator.compare(current,best) > 0)
							best = current;
					
				}
		}
		//couldn't elect anybody.
		if (best==null)
			return null;
		
		//return the selected candidate
		Candidate ret = null;
		try {
			ret = new Candidate(best);
		}catch(UnknownHostException ignored){}
		
		return ret;
	}
	
	/**
	 * compares connections with regard to their potential for
	 * being good ultrapeers.  
	 *
	 * The most important one is uptime.  The available bandwidth
	 * and the number of shared files are also taken in account.
	 * 
	 * Proposed formula:
	 * score = uptime(minutes) + upbandwidth(kb)*3 - sharedFiles/4
	 * 
	 * Example: 
	 * a node has been up for 7 hours, with 20k outgoing bw sharing
	 * 400 files.
	 * score = 7*60 + 60 - 100 = 380
	 * 
	 * will score better than a node that has been up for 8 hours and a half,
	 * with 10k outgoing link and sharing 800 files.
	 * score = 8*60 + 30 -200 = 310
	 *
	 */
	private class ConnectionComparator implements Comparator {
		public int compare(Object a, Object b){
			if (a==null)
				return -1;
			if (b==null)
				return 1;
			
			Connection conn1 = (Connection)a;
			Connection conn2 = (Connection)b;
			
			int score1 = (int)(System.currentTimeMillis() - conn1.getConnectionTime()) / (1000*60) + 
						conn1.getBandwidth()*3 - conn1.getFileShared()/4;
			
			int score2 = (int)(System.currentTimeMillis() - conn2.getConnectionTime()) / (1000*60) + 
				conn2.getBandwidth()*30 - conn2.getFileShared()/4;
			
			return score1-score2;
			
		}
	}
}