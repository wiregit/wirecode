/*
 * This class keeps state related to the promotion process.
 * 
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.*;

import java.net.InetAddress;

public class PromotionManager {
	
	/**
	 * the leaf promotion timeout is a quarter of the ultrapeer timeout.
	 */
	private static long UP_REQUEST_TIMEOUT = Constants.MINUTE;
	private static long LEAF_REQUEST_TIMEOUT = UP_REQUEST_TIMEOUT/4;
	
	/**
	 * ref to the UDP service.  Not final so that tests can change it.
	 */
	static UDPService _udpService = UDPService.instance();
	
	/**
	 * LOCKS _promotionPartner, _guid
	 */
	private Object _promotionLock = new Object();
	
	/**
	 * the endpoint of either the requestor or the candidate for promotion 
	 * LOCKING: obtain _promotionLock
	 */
	private IpPort _promotionPartner;
	
	/**
	 * the GUID of the promotion request message and the ack's
	 * LOCKING: obtain _promotionLock
	 */
	private GUID _guid;
	
	/**
	 * keeps a list of the people who have requested our connection lists. 
	 * used to make sure we don't get ping-flooded. 
	 * not final so that tests won't take forever.
	 */
	private FixedSizeExpiringSet _UDPListRequestors 
		= new FixedSizeExpiringSet(2000, 10*60 * 1000); //10 minutes.
	
	/**
	 * whether the reply handler should receive a reply.  
	 * used to protect us from ping-flooding.
	 * 
	 * @param r the <tt>ReplyHandler</tt> on which the reply is to be sent
	 * @return true if its ok to reply.
	 */
	public boolean allowUDPPing(ReplyHandler r) {
		//this also takes care of multiple instances running on the same ip address.
		return _UDPListRequestors.add(r.getInetAddress());
	}
	
	/**
	 * a ref to the current Expirer thread.
	 */
	private Expirer _expirer;
	
	/**
	 * a ref to the best candidates table
	 */
	private BestCandidates _bestCandidates = BestCandidates.instance();
	
	/**
	 * initiates the promotion process.  It sends out an udp ping to the
     * original requestor and when the ack comes back it the listener will 
     * start the crawl in a separate thread.
     * 
     * TODO: decide what to do if a new request arrives while we're already promoting.
     * At the moment we just drop it.
     */
    public void initiatePromotion(PromotionRequestVendorMessage msg) {
    	
    	//set the promotion partner
    	IpPortPair requestor = new IpPortPair (
				msg.getRequestor().getAddress(),
				msg.getRequestor().getPort());
    	 
    	
    	synchronized(_promotionLock) {
    		if (_promotionPartner == null) {
    			_promotionPartner = requestor;
    			_guid = new GUID(msg.getGUID());
    		}
    		else
    			//_requestors.add(requestor);
    			return; //just drop the promotion request for now.
    	}
    	
    	
    	//schedule the expiration of the process
    	_expirer = new Expirer(LEAF_REQUEST_TIMEOUT);
    	
    	//ping the original requestor
    	PromotionACKVendorMessage ping = new PromotionACKVendorMessage(_guid);
    	_udpService.send( ping, _promotionPartner);
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
	public final boolean isPromoting(IpPort other) {
		//I'm not locking the first check as it will 
		//1) happen too often
		//2) its ok if the value gets changed by
		//   a different thread.
		if (_promotionPartner == null)
			return false;
		
		//allow locals for testing
		if (!ConnectionSettings.LOCAL_IS_PRIVATE.getValue())
			return true;
		
		synchronized(_promotionLock) {
			return other.isSame(_promotionPartner);
					
		}
	}
	
	public void handleACK(IpPort sender, GUID guid) {
		//cache the current status
		boolean isSupernode = RouterService.isSupernode();
		
		//first see if anyone is indeed a promotion partner
    	
    	
    	synchronized(_promotionLock) {
    		//check if we received the ACK from the right person
    		//also allow for loopback addresses for testing 
    		if (!sender.isSame(_promotionPartner) ||
    				!guid.equals(_guid)) //&& 
    				//!sender.getInetAddress().isLoopbackAddress())
    			return;
    	}
    	
    	//*************************
    	//we know we have received a proper ACK.
    	//postpone the expiration 
    	if (_expirer!=null)
    		_expirer.cancel();    	
    	
    	
    	//then, proceed as appropriate:
    	
    	//if we are a leaf, start the promotion process
    	if (!isSupernode) {
    		Thread promoter = new ManagedThread(
    				new Promoter(sender));
    		promoter.setName("Promotion thread");
    		promoter.setDaemon(true);
    		promoter.start();
    	} 
    	else {
    		//we are the originally requesting UP, ACK back.
    		PromotionACKVendorMessage pong = new PromotionACKVendorMessage(_guid);
    		_udpService.send(pong, sender);
    		_expirer = new Expirer(UP_REQUEST_TIMEOUT);

    	}
	}
	
	/**
	 * sends a promotion request to the best candidate.
	 */
	public void requestPromotion() {
		
		PromotionRequestVendorMessage msg;
		
		synchronized(_promotionLock) {
			//check if we are already requesting promotion and if so,
			if (_promotionPartner!=null)
				return;
			
			//set the promotion partner 
			_promotionPartner = _bestCandidates.getBest();
			
			//start a resetting thread
			_expirer = new Expirer(UP_REQUEST_TIMEOUT);
			
			msg = 
				new PromotionRequestVendorMessage((Candidate)_promotionPartner);
			
			_guid = new GUID(msg.getGUID());
		}
		
		//*******
		//send a PromotionRequestVM to the appropriate route	
		RouterService.getMessageRouter().forwardPromotionRequest(msg);	
		
	}
	
	/**
	 * expires the partner if we don't get an ACK.
	 * interrupt to cancel.
	 * 
	 */
	private class Expirer implements Runnable {
		
		boolean _cancelled = false;
		
		public void cancel() {
			_cancelled = true;
		}
		/**
		 * creates a new expiring thread which interrupts
		 * any currently running expiring threads.
		 */
		public Expirer(long timeout) {
			
			if (_expirer!=null)
				_expirer.cancel();
			
			_expirer=this;
			
			RouterService.schedule(this,timeout,0);

		}
		
		public void run() {
			if (!_cancelled)
				synchronized(_promotionLock) {
					_promotionPartner=null;
					_guid=null;
				}
		}
	}
	
}

