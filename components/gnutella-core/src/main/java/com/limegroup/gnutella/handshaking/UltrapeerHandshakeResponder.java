package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;
import java.util.Properties;
import java.io.IOException;

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

		if(!_manager.allowConnection(response)) {			
            return HandshakeResponse.createRejectOutgoingResponse(new Properties());
		}

		Properties ret = new Properties();
        if(response.hasLeafGuidance() &&
           isNotBearshare(response) &&
           _manager.allowLeafDemotion()) {
			//Fine, we'll become a leaf.
			ret.put(HeaderNames.X_ULTRAPEER, "False");
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
		//Incoming connection....
		Properties ret = new UltrapeerHeaders(getRemoteIP());
		
		//guide the incoming connection to be a ultrapeer/clientnode
		ret.put(HeaderNames.X_ULTRAPEER_NEEDED,
				(new Boolean(_manager.supernodeNeeded())).toString());
		
		//give own IP address
		ret.put(HeaderNames.LISTEN_IP,
				NetworkUtils.ip2string(RouterService.getAddress())+":"
				+ RouterService.getPort());
		
		
		//Decide whether to allow or reject.  Somewhat complicated because
		//of ultrapeer guidance.

		// TODO::add special cases for the type of client we're connecting to
		if (reject(response)) {
            // reject the connection, and let the other node know about 
            // any Ultrapeers we're connected to
            return HandshakeResponse.createRejectIncomingResponse(ret);
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
    private boolean reject(HandshakeResponse response) { 
        //Under some circumstances, we can decide to reject a connection during
        //handshaking because no slots are available.  You might think you could
        //reject the connection if !_manager.allowConnection(A, L), where A
        //is true if the connection is the connection is ultrapeer-aware and L
        //is true if the user is a leaf.  Unfortunately this fails when the
        //incoming connection is an ultrapeer (A&&!L) because of ultrapeer
        //guidance; we don't know whether they'll become a leaf node or not.  So
        //we use the following conservative test, and depend on the
        //old-fashioned reject connection mechanism in ConnectionManager for the
        //other cases.       

        boolean allowedNow = _manager.allowConnection(response);
		if(allowedNow) return false;

		boolean allowedAsLeaf = _manager.allowConnectionAsLeaf(response);

        //Reject if not allowed now and guidance not possible.
        return !(response.isUltrapeer() && allowedAsLeaf);
    }
}

