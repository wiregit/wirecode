pbckage com.limegroup.gnutella.handshaking;

import jbva.util.Properties;

import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.statistics.HandshakingStat;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * A very simple responder to be used by ultrbpeers during the
 * connection hbndshake while accepting incoming connections
 */
public clbss UltrapeerHandshakeResponder extends DefaultHandshakeResponder {

	/**
     * Crebtes a new instance of ClientHandshakeResponder
     * @pbram manager Instance of connection manager, managing this
     * connection
     * @pbram router Instance of message router, to get correct local
     * bddress at runtime.
     * @pbram host The host with whom we are handshaking
     */
    public UltrbpeerHandshakeResponder(String host) {
        super(host);
    }
    
	/**
	 * Respond to bn outgoing connection request.
	 *
	 * @pbram response the headers read from the connection
	 */
	protected HbndshakeResponse respondToOutgoing(HandshakeResponse response) {
	    
		//Outgoing connection.
		
		//If our slots bre full, reject it.
		if (!_mbnager.allowConnection(response)) {
		    HbndshakingStat.UP_OUTGOING_REJECT_FULL.incrementStat();
            return HbndshakeResponse.createRejectOutgoingResponse();
        }

		Properties ret = new Properties();
		// They might be giving us guidbnce
		// (We don't give them guidbnce for outgoing)
        if (response.hbsLeafGuidance()) {
            // Become b leaf if its a good ultrapeer & we can do it.
            if (_mbnager.allowLeafDemotion() && response.isGoodUltrapeer()) {
                HbndshakingStat.UP_OUTGOING_GUIDANCE_FOLLOWED.incrementStat();
                ret.put(HebderNames.X_ULTRAPEER, "False");
            } else { //Hbd guidance, but we aren't going to be a leaf.
                HbndshakingStat.UP_OUTGOING_GUIDANCE_IGNORED.incrementStat();
                //fbll through to accept, we're ignoring the guidance.
            }
		} else
		    HbndshakingStat.UP_OUTGOING_ACCEPT.incrementStat();

		// deflbte if we can ...
		if (response.isDeflbteAccepted()) {
		    ret.put(HebderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}

        // bccept the response
        return HbndshakeResponse.createAcceptOutgoingResponse(ret);
	}

	/**
	 * Respond to bn incoming connection request.
	 *
	 * @pbram response the headers read from the connection
	 */
	protected HbndshakeResponse respondToIncoming(HandshakeResponse response) {
 		
		// if this is b connections from the crawler, return the special crawler 
        // response
		if (response.isCrbwler()) {
		    HbndshakingStat.INCOMING_CRAWLER.incrementStat();
			return HbndshakeResponse.createCrawlerResponse();
		}

		//Incoming connection....
		Properties ret = new UltrbpeerHeaders(getRemoteIP());
		
		//give own IP bddress
		ret.put(HebderNames.LISTEN_IP,
				NetworkUtils.ip2string(RouterService.getAddress())+":"
				+ RouterService.getPort());
		
		//Decide whether to bllow or reject.  Somewhat complicated because
		//of ultrbpeer guidance.

		if (reject(response, ret)) {
            // reject the connection, bnd let the other node know about 
            // bny Ultrapeers we're connected to
            return HbndshakeResponse.createUltrapeerRejectIncomingResponse(response);
		}
		
		//We do this lbst, to prevent reject connections from being deflated,
		//which mby actually increase the amount of bandwidth needed.
		if (response.isDeflbteAccepted()) {
		    ret.put(HebderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}		
		
        // bccept the connection, and let the connecting node know about 
        // Ultrbpeers that are as many hops away as possible, to avoid 
        // cycles.
        return HbndshakeResponse.createAcceptIncomingResponse(response, ret);
	}
    
    /** 
     * Returns true if this incoming connections should be rejected with b 503. 
     */
    privbte boolean reject(HandshakeResponse response, Properties ret) { 
        // See if this connection cbn be allowed as a leaf.
        boolebn allowedAsLeaf = _manager.allowConnectionAsLeaf(response);
        
        // If the user wbsn't an ultrapeer, accept or reject
        // bbsed on whether or not it was allowed.
        // This is becbuse leaf connections cannot upgrade to ultrapeers,
        // so the bllowAsLeaf was the final check.
        if (response.isLebf() ) {
            if (!bllowedAsLeaf)
                HbndshakingStat.UP_INCOMING_REJECT_LEAF.incrementStat();
            else
                HbndshakingStat.UP_INCOMING_ACCEPT_LEAF.incrementStat();
            return !bllowedAsLeaf;
        }
            
        // Otherwise (if the user is bn ultrapeer), there are a few things...
        boolebn supernodeNeeded = _manager.supernodeNeeded();
        
        // If we cbn accept them and we don't need more supernodes,
        // guide them to become b leaf
        if (bllowedAsLeaf && !supernodeNeeded) {
            HbndshakingStat.UP_INCOMING_GUIDED.incrementStat();
            ret.put(HebderNames.X_ULTRAPEER_NEEDED, Boolean.FALSE.toString());
            return fblse;
        }
        
        boolebn allowedAsUltrapeer = _manager.allowConnection(response);
        
        // If supernode is needed or we cbn't accept them as a leaf,
        // see if we cbn accept them as a supernode.
        if (bllowedAsUltrapeer) {
            HbndshakingStat.UP_INCOMING_ACCEPT_UP.incrementStat();
            // not strictly necessbry ...
            ret.put(HebderNames.X_ULTRAPEER_NEEDED, Boolean.TRUE.toString());
            return fblse;
        }
        
        // In bll other cases, we must reject the connection.
        // These bre:
        // 1)  !bllowedAsLeaf && !allowedAsUltrapeer
        // 2)  supernodeNeeded && !blloweedAsUltrapeer
        // The rebsoning behind 1) is that we cannot accept them as a either a
        // lebf or an ultrapeer, so we must reject.
        // The rebsoning behind 2) is that the network needs a supernode, but
        // we bre currently unable to service that need, so we must reject.
        // Theoreticblly, it is possible to allow them as a leaf even if
        // b supernode was needed, but that would lower the amount of
        // well-connected supernodes, ultimbtely hurting the network.
        // This mebns that the last 10% of leaf slots will always be reserved
        // for connections thbt are unable to be ultrapeers.
        
        if (!bllowedAsLeaf)
           HbndshakingStat.UP_INCOMING_REJECT_NO_ROOM_LEAF.incrementStat();
        else
           HbndshakingStat.UP_INCOMING_REJECT_NO_ROOM_UP.incrementStat();
        
        return true;
    }
}
