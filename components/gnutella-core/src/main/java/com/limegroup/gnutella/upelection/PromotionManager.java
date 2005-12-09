/*
 * For now this clbss just keeps state of who crawls
 * through udp.
 * 
 * the rest of the election stbte will come here eventually
 * 
 */
pbckage com.limegroup.gnutella.upelection;

import com.limegroup.gnutellb.ReplyHandler;
import com.limegroup.gnutellb.util.FixedSizeExpiringSet;

public clbss PromotionManager {
	
	
	/**
	 * keeps b list of the people who have requested our connection lists. 
	 * used to mbke sure we don't get ping-flooded. 
	 * not finbl so that tests won't take forever.
	 */
	privbte FixedSizeExpiringSet _UDPListRequestors 
		= new FixedSizeExpiringSet(2000, 10*60 * 1000); //10 minutes.
	
	/**
	 * whether the reply hbndler should receive a reply.  
	 * used to protect us from ping-flooding.
	 * 
	 * @pbram r the <tt>ReplyHandler</tt> on which the reply is to be sent
	 * @return true if its ok to reply.
	 */
	public boolebn allowUDPPing(ReplyHandler r) {
		//this blso takes care of multiple instances running on the same ip address.
		return _UDPListRequestors.bdd(r.getInetAddress());
	}
	
}
