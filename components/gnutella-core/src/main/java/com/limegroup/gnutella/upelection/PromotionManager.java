/*
 * This class maintains state for the election process.
 * 
 * TODO: Decide what to do when a promotion request arrives while we are currently promoting
 * ourselves.  Keep in mind the best leaf is consistent within ttl 2, which is 30x30 = 900 Ultrapeers!
 * This means the best candidate may end up receiving requests quite often.
 * 
 * So either A) add the promoter to the list because we know for sure that
 * that UP needs is running out of slots, so we will connect to it or B) ignore it. 
 * 
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.*;

import com.sun.java.util.collections.*;

import java.net.InetAddress;

public class PromotionManager {
	
	/**
	 * a list of <tt>Endpoint</tt>'s of people requesting promotion.  
	 */
	private List _requestors = Collections.synchronizedList(new ArrayList());
	
	/**
	 * if we don't get a pong from the request[or|ee] within 30 secs, discard.
	 */
	private static long REQUEST_TIMEOUT = 30*1000;
	
	/**
	 * a ref to the thread which resets the state
	 */
	private Thread _expirer;

	/**
	 * keeps a list of the people who advertised their best candidate.  
	 * These should be only neighboring ultrapeers.  
	 * Furthermore they shouldn't do it too often. 
	 * TODO: export the values to the proper constants. 
	 * Note: the also is not final so that the tests won't take forever.
	 */
	private FixedSizeExpiringSet _candidateAdvertisers = 
		new FixedSizeExpiringSet(ConnectionManager.ULTRAPEER_CONNECTIONS, 14 * 60 * 1000);
	
	/**
	 * object to lock on when manipulating this's data structures.
	 * its easier this way rather than exporting the sync blocks in their own methods,
	 * and since these methods will be called from core threads they cannot be 
	 * completely synchronized.
	 */
	private Object _promotionLock = new Object();
	/**
	 * the endpoint of either the requestor or the candidate for promotion LOCKING: obtain _promotionLock
	 */
	private Endpoint _promotionPartner;
	/**
	 * keeps a list of the people who have requested our connection lists. used to make sure we don't get ping-flooded. not final so that tests won't take forever.
	 */
	private FixedSizeExpiringSet _UDPListRequestors = new FixedSizeExpiringSet(200, 5 * 1000);
	/**
	 * some constants related to the propagation of the best candidates.   should eventually be determined by experiments.
	 */
	public final static int CANDIDATE_PROPAGATION_DELAY = 2 * 60 * 60 * 1000;
	public final static int CANDIDATE_PROPAGATION_INTERVAL = 15 * 60 * 1000;
	
	/**
	 * initiates the promotion process.  It sends out an udp ping to the
     * original requestor and when the ack comes back it the listener will 
     * start the crawl in a separate thread.
     * 
     * TODO: decide what to do if a new request arrives while we're already promoting.
     */
    public void initiatePromotion(PromotionRequestVendorMessage msg) {
    	
    	//set the promotion partner
    	Endpoint requestor = new Endpoint (
				msg.getRequestor().getAddress(),
				msg.getRequestor().getPort());
    	
    	synchronized(_promotionLock) {
    		if (_promotionPartner == null)
    			_promotionPartner = requestor; 
    		else
    			_requestors.add(requestor);
    	}
    	
    	
    	//schedule the expiration of the process
    	_expirer = new ManagedThread(new Expirer());
    	_expirer.setDaemon(true);
    	_expirer.start();
    	
    	//ping the original requestor
    	PromotionACKVendorMessage ping = new PromotionACKVendorMessage();
    	UDPService.instance().send( ping, _promotionPartner);
    }
	/**
	 * @return Returns the _candidateAdvertisers.
	 */
	public FixedSizeExpiringSet getCandidateAdvertisers() {
		return _candidateAdvertisers;
	}
	/**
	 * @return Returns the _UDPListRequestors.
	 */
	public FixedSizeExpiringSet getUDPListRequestors() {
		return _UDPListRequestors;
	}
	
	/**
	 * @return whether this node is currently in process of promoting itself.
	 */
	public boolean isPromoting() {
		synchronized(_promotionLock) {
			return _promotionPartner!=null;
		}
	}
	
	/**
	 * Checks whether the current pair of InetAddress and port
	 * match those of the promotion partner.
	 * final as will be called often.
	 */
	public final boolean isPromoting(InetAddress addr, int port) {
		//I'm not locking the first check as it will 
		//1) happen too often
		//2) its no big deal if the value gets changed by
		//   a different thread.
		if (_promotionPartner == null)
			return false;
		
		//allow locals for testing
		if (!ConnectionSettings.LOCAL_IS_PRIVATE.getValue())
			return true;
		
		synchronized(_promotionLock) {
			//check for null again if it got nulled in the meantime
			return _promotionPartner != null &&
					_promotionPartner.getInetAddress().equals(addr) &&
					_promotionPartner.getPort() == port;
		}
	}
	
	public void handleACK(PromotionACKVendorMessage message, Endpoint sender) {
		//cache the current status
		boolean isSupernode = RouterService.isSupernode();
		
		//first see if anyone is indeed a promotion partner
    	
    	
    	synchronized(_promotionLock) {
    		if (_promotionPartner == null)  
    			return;
    	
    		//check if we received the ACK from the right person
    		//also allow for loopback addresses for testing 
    		if (!sender.equals(_promotionPartner) && 
    				!sender.getInetAddress().isLoopbackAddress())
    			return;
    			
    		//set the promotion partner to null
    		//have an extra check here to avoid re-acquiring the lock
			if (!isSupernode)
				_promotionPartner=null;
    	}
    	
    	//*************************
    	//we know we have received a proper ACK.
    	//stop the expiration thread
    	_expirer.interrupt();
    	_expirer = null;    	
    	
    	
    	//then, proceed as appropriate:
    	
    	//if we are a leaf, start the promotion process
    	if (!isSupernode) {
    		Thread promoter = new ManagedThread(
    				new Promoter(sender));
    		promoter.setDaemon(true);
    		promoter.start();
    	} 
    	else {
    		//we are the originally requesting UP, ACK back.
    		PromotionACKVendorMessage pong = new PromotionACKVendorMessage();
    		UDPService.instance().send(pong, sender);
			
			//postpone the timeout of the promotion for a while
			_expirer = new ManagedThread(new Expirer());
			_expirer.setDaemon(true);
			_expirer.start();
    	}
	}
	
	/**
	 * sends a promotion request to the best candidate.
	 */
	public void requestPromotion() {
		
		Endpoint candidate = new Endpoint (BestCandidates.getBest().getInetAddress().getAddress(),
							BestCandidates.getBest().getPort());
		
		synchronized(_promotionLock) {
			//check if we are already requesting promotion and if so,
			if (_promotionPartner!=null)
				return;
			
			//set the promotion partner 
			_promotionPartner = candidate;
			
			//start a resetting thread
			_expirer = new ManagedThread(new Expirer());
			_expirer.setDaemon(true);
			_expirer.start();
		}
		
		//*******
		//send a PromotionRequestVM to the appropriate route
		PromotionRequestVendorMessage msg = new PromotionRequestVendorMessage(BestCandidates.getBest());
		
		RouterService.getMessageRouter().forwardPromotionRequest(msg);	
		
	}
	
	/**
	 * expires the partner if we don't get an ACK.
	 * interrupt to cancel.
	 * 
	 */
	private class Expirer implements Runnable {
		public void run() {
			try {
				//sleep some time
				Thread.sleep(REQUEST_TIMEOUT);
			
				//if we didn't get interrupted by now the candidate
				//has failed to reply or has successfully promoted
				//itself to an UP.  Either way,
				if (RouterService.isSupernode())
					BestCandidates.fail(_promotionPartner);
				
				//and clear the state
				synchronized(_promotionLock) {
					_promotionPartner=null;
				}
			}catch(InterruptedException iex) {
				//end the thread.
			}
		}
	}
}
