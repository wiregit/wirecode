/*
 * This class maintains state for the election process.
 * 
 * If a promotion request arrives while we are currently promoting
 * ourselves, add the promoter to the list because we know for sure that
 * that UP needs is running out of slots, so we will connect to it.
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.vendor.*;

import com.sun.java.util.collections.*;

public class PromotionManager {
	
	/**
	 * a list of <tt>Endpoint</tt>'s of people requesting promotion.  
	 */
	private List _requestors = Collections.synchronizedList(new ArrayList());
	
	/**
	 * if we don't get a pong from the request[or|ee] within 30 secs, discard.
	 */
	private static final long REQUEST_TIMEOUT = 30*1000;
	
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
	
	public void handleACK(PromotionACKVendorMessage message, Endpoint sender) {
		//first see if anyone is indeed a promotion partner
    	Endpoint partner = null;
    	
    	synchronized(_promotionLock) {
    		if (_promotionPartner == null)  
    			return;
    	
    		//check if we received the ACK from the right person
    		if (!sender.equals(_promotionPartner)) 
					return;
    		
    		//set the promotion partner to null if that's the case
    		partner = _promotionPartner;
    		_promotionPartner = null;
    	}
    	
    	//*************************
    	//we know we have received a proper ACK.
    	
    	
    	//first, stop the expiration thread
    	_expirer.interrupt();
    	
    	//then, proceed as appropriate:
    	
    	//if we are a leaf, start the promotion process
    	if (!RouterService.isSupernode()) {
    		Thread promoter = new ManagedThread(
    				new Promoter(sender));
    		promoter.setDaemon(true);
    		promoter.start();
    	} 
    	else {
    		//we are the originally requesting UP, ACK back.
    		PromotionACKVendorMessage pong = new PromotionACKVendorMessage();
    		UDPService.instance().send(pong, sender);
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
		}
		
		//*******
		//send a PromotionRequestVM to the appropriate route
		PromotionRequestVendorMessage msg = new PromotionRequestVendorMessage(BestCandidates.getBest());
		
		RouterService.getMessageRouter().forwardPromotionRequest(msg);
		
		//start a cleaner thread
		_expirer = new ManagedThread(new Expirer());
		_expirer.setDaemon(true);
		_expirer.start();
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
			
				//clear the state
				synchronized(_promotionLock) {
					_promotionPartner=null;
				}
			}catch(InterruptedException iex) {
				//end the thread.
			}
		}
	}
}
