
package com.limegroup.gnutella.upelection;

import java.net.UnknownHostException;
import java.io.IOException;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.sun.java.util.collections.Comparator;
import com.sun.java.util.collections.Iterator;
import com.limegroup.gnutella.util.*;


/**
 * As an UP:
 * cycles through the UP connections and checks whether they need updating.
 * 
 * As a leaf:
 * sends out Features VMs to the connected UPs at specific intervals.
 */
public class CandidateAdvertiser extends ManagedThread {
	
	/**
	 * how long to wait until the first advertisement.
	 */
	public static long INITIAL_DELAY = 2*60*60*1000; 
	
	/**
	 * how often to send FeaturesMessages as a leaf.
	 */
	public static long LEAF_INTERVAL = 15*60*1000; //15 minutes
	
	/**
	 * how often to propagate changes when an UP.
	 */
	public static long UP_INTERVAL = 10*1000; //10 seconds
	
	/**
	 * the <tt>BestCandidatesVendorMessage</tt> to send.
	 * LOCKING: obtain this.
	 */
	private  BestCandidatesVendorMessage _bcvm;
	
	
	
	public void managedRun() {
		
		try{
			Thread.sleep(INITIAL_DELAY);
		
			//initialize the best candidates table.
			BestCandidates.initialize();
			
			while(true) {
			
				if (!RouterService.isSupernode()) { 
					sendFeaturesMessage();
					Thread.sleep(LEAF_INTERVAL);
				}
				else {
					sendCandidatesMessage();
					Thread.sleep(UP_INTERVAL);
				}
			}
		
		}catch(InterruptedException hmm) {
			//this thread should not be interrupted.
			//unless you want to stop it for testing reasons.
			return;
		}
	}
	
	
	/**
	 * sends a <tt>FeaturesVendorMessage</tt> to all connected
	 * Ultrapeers.
	 */
	private void sendFeaturesMessage() {
		FeaturesVendorMessage fvm = new FeaturesVendorMessage();
		
		for(Iterator iter = RouterService.getConnectionManager().
				getInitializedConnections().iterator();iter.hasNext();)
			try {
				Connection c = (Connection)iter.next();
				c.send(fvm);
			}catch(IOException tooBad) {} //nothing we can do.  continue with next UP.
	}
	
	/**
	 * @param _msg The message that should be sent out next time.
	 */
	public synchronized void setMsg(BestCandidatesVendorMessage msg) {
		_bcvm = msg;
	}
	
	/**
	 * sends a <tt>BestCandidatesVendorMessage</tt> to those 
	 * ultrapeer connections which need updating.
	 * 
	 * synchronized so that bcvm doesn't change while we're sending
	 */
	private synchronized void sendCandidatesMessage() {
		for(Iterator iter = RouterService.getConnectionManager().
				getInitializedConnections().iterator();iter.hasNext();)
			try {
				Connection c = (Connection)iter.next();
				if (c.remoteHostSupportsBestCandidates() > 0)
					c.handleBestCandidatesMessage(_bcvm);
			}catch(IOException tooBad) {} //nothing we can do.  continue with next UP.
		
		_bcvm = null;
	}
}