package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.util.Properties;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * A very simple responder to be used by ultrapeers during the
 * connection handshake while accepting incoming connections
 */
public class UltrapeerHandshakeResponder 
    extends AuthenticationHandshakeResponder {

	/**
	 * Handle to the <tt>ConnectionManager</tt> for use in determining
	 * whether or not connections should be accepted.
	 */
    private final ConnectionManager _manager;
    
    /**
     * Creates a new instance of ClientHandshakeResponder
     * @param manager Instance of connection manager, managing this
     * connection
     * @param router Instance of message router, to get correct local
     * address at runtime.
     * @param host The host with whom we are handshaking
     */
    public UltrapeerHandshakeResponder(String host) {
        super(RouterService.getConnectionManager(), host);
        this._manager = RouterService.getConnectionManager();
    }
    
    //inherit doc comment
    protected HandshakeResponse respondUnauthenticated(
        HandshakeResponse response, boolean outgoing) throws IOException {
        
		if(outgoing) return respondToOutgoing(response);
		return respondToIncoming(response);				
	}

	/**
	 * Respond to an outgoing connection request.
	 *
	 * @param response the headers read from the connection
	 */
	private HandshakeResponse respondToOutgoing(HandshakeResponse response) {
		//Outgoing connection.  If the other guy is ultrapeer unaware and I
		//already have enough old-fashioned connections, reject it.  We've
		//already given ultrapeer guidance at this point, so there's no
		//"second chance" like in the reject(..) method.
		if(!response.isUltrapeer())
		    return HandshakeResponse.createRejectOutgoingResponse();

		if(!_manager.allowConnection(response))
            return HandshakeResponse.createRejectOutgoingResponse();

		Properties ret = new Properties();
        if(response.hasLeafGuidance()) {
            // Become a leaf if its a good ultrapeer & we can do it.
            if(_manager.allowLeafDemotion() && response.isGoodUltrapeer())
                ret.put(HeaderNames.X_ULTRAPEER, "False");
            else //Had guidance, but we aren't going to be a leaf.
                return HandshakeResponse.createRejectOutgoingResponse();
		}

		// deflate if we can ...
		if(response.isDeflateAccepted()) {
		    ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}
		    
        // accept the response
        return HandshakeResponse.createAcceptOutgoingResponse(ret);
	}

	/**
	 * Respond to an incoming connection request.
	 *
	 * @param response the headers read from the connection
	 */
	private HandshakeResponse respondToIncoming(HandshakeResponse response) {
 		
		// if this is a connections from the crawler, return the special crawler response
		if(response.isCrawler()) {
			return HandshakeResponse.createCrawlerResponse();
		}

		//Incoming connection....
		Properties ret = new UltrapeerHeaders(getRemoteIP());
		
		//give own IP address
		ret.put(HeaderNames.LISTEN_IP,
				NetworkUtils.ip2string(RouterService.getAddress())+":"
				+ RouterService.getPort());
		
		//Decide whether to allow or reject.  Somewhat complicated because
		//of ultrapeer guidance.

		if (reject(response, ret)) {
            // reject the connection, and let the other node know about 
            // any Ultrapeers we're connected to
            return HandshakeResponse.createRejectIncomingResponse();
		}
		
		//We do this last, to prevent reject connections from being deflated,
		//which may actually increase the amount of bandwidth needed.
		if(response.isDeflateAccepted()) {
		    ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}		
		
        // accept the connection, and let the connecting node know about 
        // Ultrapeers that are as many hops away as possible, to avoid 
        // cycles.
        return HandshakeResponse.createAcceptIncomingResponse(ret);
	}


    /**
	 * Returns whether or not the set of connection headers denotes that the
	 * connecting host is a BearShare.  Since BearShare does not allow
	 * non-BearShare leaves, we ignore it's attempt to demote us to
	 * a leaf.
	 *
	 * @param headers the connection headers
	 */
	private boolean isNotBearshare(HandshakeResponse headers) {
        String userAgent = headers.getUserAgent();
		if(userAgent == null) return true;
		return !userAgent.startsWith("BearShare");
	}
    
    /** 
     * Returns true if this incoming connections should be rejected with a 503. 
     */
    private boolean reject(HandshakeResponse response, Properties ret) { 
        // See if this connection can be allowed as a leaf.
        boolean allowedAsLeaf = _manager.allowConnectionAsLeaf(response);
        
        // If the user wasn't an ultrapeer, accept or reject
        // based on whether or not it was allowed.
        // This is because leaf connections cannot upgrade to ultrapeers,
        // so the allowAsLeaf was the final check.
        if( response.isLeaf() )
            return !allowedAsLeaf;
            
        // Otherwise (if the user is an ultrapeer), there are a few things...
        boolean supernodeNeeded = _manager.supernodeNeeded();
        
        // If we can accept them and we don't need more supernodes,
        // guide them to become a leaf
        if( allowedAsLeaf && !supernodeNeeded) {
            ret.put(HeaderNames.X_ULTRAPEER_NEEDED, Boolean.FALSE.toString());
            return false;
        }
        
        boolean allowedAsUltrapeer = _manager.allowConnection(response);
        
        // If supernode is needed or we can't accept them as a leaf,
        // see if we can accept them as a supernode.
        if( allowedAsUltrapeer ) {
            // not strictly necessary ...
            ret.put(HeaderNames.X_ULTRAPEER_NEEDED, Boolean.TRUE.toString());
            return false;
        }
        
        // In all other cases, we must reject the connection.
        // These are:
        // 1)  !allowedAsLeaf && !allowedAsUltrapeer
        // 2)  supernodeNeeded && !alloweedAsUltrapeer
        // The reasoning behind 1) is that we cannot accept them as a either a
        // leaf or an ultrapeer, so we must reject.
        // The reasoning behind 2) is that the network needs a supernode, but
        // we are currently unable to service that need, so we must reject.
        // Theoretically, it is possible to allow them as a leaf even if
        // a supernode was needed, but that would lower the amount of
        // well-connected supernodes, ultimately hurting the network.
        // This means that the last 10% of leaf slots will always be reserved
        // for connections that are unable to be ultrapeers.
        return true;
    }
}

