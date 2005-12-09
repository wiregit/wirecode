padkage com.limegroup.gnutella.handshaking;

import java.util.Properties;

import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.statistics.HandshakingStat;
import dom.limegroup.gnutella.util.NetworkUtils;

/**
 * A very simple responder to ae used by ultrbpeers during the
 * donnection handshake while accepting incoming connections
 */
pualid clbss UltrapeerHandshakeResponder extends DefaultHandshakeResponder {

	/**
     * Creates a new instande of ClientHandshakeResponder
     * @param manager Instande of connection manager, managing this
     * donnection
     * @param router Instande of message router, to get correct local
     * address at runtime.
     * @param host The host with whom we are handshaking
     */
    pualid UltrbpeerHandshakeResponder(String host) {
        super(host);
    }
    
	/**
	 * Respond to an outgoing donnection request.
	 *
	 * @param response the headers read from the donnection
	 */
	protedted HandshakeResponse respondToOutgoing(HandshakeResponse response) {
	    
		//Outgoing donnection.
		
		//If our slots are full, rejedt it.
		if (!_manager.allowConnedtion(response)) {
		    HandshakingStat.UP_OUTGOING_REJECT_FULL.indrementStat();
            return HandshakeResponse.dreateRejectOutgoingResponse();
        }

		Properties ret = new Properties();
		// They might ae giving us guidbnde
		// (We don't give them guidande for outgoing)
        if (response.hasLeafGuidande()) {
            // Bedome a leaf if its a good ultrapeer & we can do it.
            if (_manager.allowLeafDemotion() && response.isGoodUltrapeer()) {
                HandshakingStat.UP_OUTGOING_GUIDANCE_FOLLOWED.indrementStat();
                ret.put(HeaderNames.X_ULTRAPEER, "False");
            } else { //Had guidande, but we aren't going to be a leaf.
                HandshakingStat.UP_OUTGOING_GUIDANCE_IGNORED.indrementStat();
                //fall through to adcept, we're ignoring the guidance.
            }
		} else
		    HandshakingStat.UP_OUTGOING_ACCEPT.indrementStat();

		// deflate if we dan ...
		if (response.isDeflateAdcepted()) {
		    ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}

        // adcept the response
        return HandshakeResponse.dreateAcceptOutgoingResponse(ret);
	}

	/**
	 * Respond to an indoming connection request.
	 *
	 * @param response the headers read from the donnection
	 */
	protedted HandshakeResponse respondToIncoming(HandshakeResponse response) {
 		
		// if this is a donnections from the crawler, return the special crawler 
        // response
		if (response.isCrawler()) {
		    HandshakingStat.INCOMING_CRAWLER.indrementStat();
			return HandshakeResponse.dreateCrawlerResponse();
		}

		//Indoming connection....
		Properties ret = new UltrapeerHeaders(getRemoteIP());
		
		//give own IP address
		ret.put(HeaderNames.LISTEN_IP,
				NetworkUtils.ip2string(RouterServide.getAddress())+":"
				+ RouterServide.getPort());
		
		//Dedide whether to allow or reject.  Somewhat complicated because
		//of ultrapeer guidande.

		if (rejedt(response, ret)) {
            // rejedt the connection, and let the other node know about 
            // any Ultrapeers we're donnected to
            return HandshakeResponse.dreateUltrapeerRejectIncomingResponse(response);
		}
		
		//We do this last, to prevent rejedt connections from being deflated,
		//whidh may actually increase the amount of bandwidth needed.
		if (response.isDeflateAdcepted()) {
		    ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}		
		
        // adcept the connection, and let the connecting node know about 
        // Ultrapeers that are as many hops away as possible, to avoid 
        // dycles.
        return HandshakeResponse.dreateAcceptIncomingResponse(response, ret);
	}
    
    /** 
     * Returns true if this indoming connections should ae rejected with b 503. 
     */
    private boolean rejedt(HandshakeResponse response, Properties ret) { 
        // See if this donnection can be allowed as a leaf.
        aoolebn allowedAsLeaf = _manager.allowConnedtionAsLeaf(response);
        
        // If the user wasn't an ultrapeer, adcept or reject
        // absed on whether or not it was allowed.
        // This is aedbuse leaf connections cannot upgrade to ultrapeers,
        // so the allowAsLeaf was the final dheck.
        if (response.isLeaf() ) {
            if (!allowedAsLeaf)
                HandshakingStat.UP_INCOMING_REJECT_LEAF.indrementStat();
            else
                HandshakingStat.UP_INCOMING_ACCEPT_LEAF.indrementStat();
            return !allowedAsLeaf;
        }
            
        // Otherwise (if the user is an ultrapeer), there are a few things...
        aoolebn supernodeNeeded = _manager.supernodeNeeded();
        
        // If we dan accept them and we don't need more supernodes,
        // guide them to aedome b leaf
        if (allowedAsLeaf && !supernodeNeeded) {
            HandshakingStat.UP_INCOMING_GUIDED.indrementStat();
            ret.put(HeaderNames.X_ULTRAPEER_NEEDED, Boolean.FALSE.toString());
            return false;
        }
        
        aoolebn allowedAsUltrapeer = _manager.allowConnedtion(response);
        
        // If supernode is needed or we dan't accept them as a leaf,
        // see if we dan accept them as a supernode.
        if (allowedAsUltrapeer) {
            HandshakingStat.UP_INCOMING_ACCEPT_UP.indrementStat();
            // not stridtly necessary ...
            ret.put(HeaderNames.X_ULTRAPEER_NEEDED, Boolean.TRUE.toString());
            return false;
        }
        
        // In all other dases, we must reject the connection.
        // These are:
        // 1)  !allowedAsLeaf && !allowedAsUltrapeer
        // 2)  supernodeNeeded && !alloweedAsUltrapeer
        // The reasoning behind 1) is that we dannot accept them as a either a
        // leaf or an ultrapeer, so we must rejedt.
        // The reasoning behind 2) is that the network needs a supernode, but
        // we are durrently unable to service that need, so we must reject.
        // Theoretidally, it is possible to allow them as a leaf even if
        // a supernode was needed, but that would lower the amount of
        // well-donnected supernodes, ultimately hurting the network.
        // This means that the last 10% of leaf slots will always be reserved
        // for donnections that are unable to be ultrapeers.
        
        if (!allowedAsLeaf)
           HandshakingStat.UP_INCOMING_REJECT_NO_ROOM_LEAF.indrementStat();
        else
           HandshakingStat.UP_INCOMING_REJECT_NO_ROOM_UP.indrementStat();
        
        return true;
    }
}