
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
public class CandidateAdvertiser implements Runnable {
	
	/**
	 * whether this timer task is cancelled
	 */
	boolean _cancelled = false;
	
	/**
	 * whether to initialize the candidates table
	 */
	boolean _initialize = false;
	
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
	
	
	private BestCandidates _bestCandidates = BestCandidates.instance();

	/**
	 * creates a new CandidateAdvertiser
	 * 
	 * @param init whether this is called on startup.  If true,
	 * the thread will be scheduled to run after INITIAL_DELAY and
	 * it will initialize the BestCandidates table.
	 */
	public CandidateAdvertiser(boolean init) {
		_initialize = init;
		
		if (init)
			RouterService.schedule(this,INITIAL_DELAY,0);
	}
	
	public void run() {
		if (_cancelled)
			return;
		
		Worker worker = new Worker();
		worker.setName("candidate advertiser");
		worker.setDaemon(true);
		worker.start();
	}
	
	/**
	 * @param _msg The message that should be sent out next time.
	 */
	public void setMsg(BestCandidatesVendorMessage msg) {
		_bcvm = msg;
	}
	
	/**
	 * cancels this task
	 */
	public void cancel() {
		_cancelled=true;
	}
	
	/**
	 * 
	 * The thread that does the advertisement.  It does a lot of network i/o
	 * so it cannot be put in the timer task.
	 */
	private class Worker extends ManagedThread {
	
		public void managedRun() {

			if (_initialize)
				_bestCandidates.initialize();
			
			if (!RouterService.isSupernode()) { 
				sendFeaturesMessage();
				RouterService.schedule(new CandidateAdvertiser(false),LEAF_INTERVAL,0);
			}
			else {
				sendCandidatesMessage();
				RouterService.schedule(new CandidateAdvertiser(false),UP_INTERVAL,0);
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
		 * sends a <tt>BestCandidatesVendorMessage</tt> to those 
		 * ultrapeer connections which need updating.
		 * 	
		 * keeps a separate ref to the best candidates message.
		 */
		private void sendCandidatesMessage() {
		
			BestCandidatesVendorMessage bcvm = _bcvm;
			
			if (bcvm !=null)
				for(Iterator iter = RouterService.getConnectionManager().
						getInitializedConnections().iterator();iter.hasNext();)
					try {
						Connection c = (Connection)iter.next();
						if (c.remoteHostSupportsBestCandidates() > 0)
							c.handleBestCandidatesMessage(bcvm);
					}catch(IOException tooBad) {} //nothing we can do.  continue with next UP.
			
		
		}
	}
	
}

