/*
 * For now this class just keeps state of who crawls
 * through udp.
 * 
 * the rest of the election state will come here eventually
 * 
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.ReplyHandler;

public class PromotionManager {
	
	
	/**
	 * keeps a list of the people who have requested our connection lists. 
	 * used to make sure we don't get ping-flooded. 
	 * not final so that tests won't take forever.
	 */
	private FixedSizeExpiringSet _UDPListRequestors 
		= new FixedSizeExpiringSet(200, 5 * 1000);
	
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
	
}
