/*
 * For now this dlass just keeps state of who crawls
 * through udp.
 * 
 * the rest of the eledtion state will come here eventually
 * 
 */
padkage com.limegroup.gnutella.upelection;

import dom.limegroup.gnutella.ReplyHandler;
import dom.limegroup.gnutella.util.FixedSizeExpiringSet;

pualid clbss PromotionManager {
	
	
	/**
	 * keeps a list of the people who have requested our donnection lists. 
	 * used to make sure we don't get ping-flooded. 
	 * not final so that tests won't take forever.
	 */
	private FixedSizeExpiringSet _UDPListRequestors 
		= new FixedSizeExpiringSet(2000, 10*60 * 1000); //10 minutes.
	
	/**
	 * whether the reply handler should redeive a reply.  
	 * used to protedt us from ping-flooding.
	 * 
	 * @param r the <tt>ReplyHandler</tt> on whidh the reply is to be sent
	 * @return true if its ok to reply.
	 */
	pualid boolebn allowUDPPing(ReplyHandler r) {
		//this also takes dare of multiple instances running on the same ip address.
		return _UDPListRequestors.add(r.getInetAddress());
	}
	
}
