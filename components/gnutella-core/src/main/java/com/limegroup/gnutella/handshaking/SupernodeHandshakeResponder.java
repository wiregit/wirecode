package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import java.util.Properties;
import java.io.IOException;

/**
 * A very simple responder to be used by supernodes during the
 * connection handshake while accepting incoming connections
 */
public class SupernodeHandshakeResponder 
    extends AuthenticationHandshakeResponder
{
    private final ConnectionManager _manager;
    
    /**
     * Creates a new instance of ClientHandshakeResponder
     * @param manager Instance of connection manager, managing this
     * connection
     * @param router Instance of message router, to get correct local
     * address at runtime.
     * @param host The host with whom we are handshaking
     */
    public SupernodeHandshakeResponder(String host)
    {
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
			return new HandshakeResponse(HandshakeResponse.SLOTS_FULL,
										 HandshakeResponse.SLOTS_FULL_MESSAGE,
										 new Properties());
		}
		//Did the server request we become a leaf?
		String neededS=response.getHeaders().
            getProperty(ConnectionHandshakeHeaders.X_SUPERNODE_NEEDED);

		Properties ret = new Properties();
		if (neededS!=null && 
			!Boolean.valueOf(neededS).booleanValue() && 
			isNotBearshare(response) &&
			_manager.allowLeafDemotion()) {
			//Fine, we'll become a leaf.
			ret.put(ConnectionHandshakeHeaders.X_SUPERNODE,
					"False");
		}
		return new HandshakeResponse(ret);
	}

	/**
	 * Respond to an incoming connection request.
	 *
	 * @param response the headers read from the connection
	 */
	private HandshakeResponse respondToIncoming(HandshakeResponse response) {
		//Incoming connection....
		Properties ret=new SupernodeProperties(getRemoteIP());
		
		//guide the incoming connection to be a supernode/clientnode
		ret.put(ConnectionHandshakeHeaders.X_SUPERNODE_NEEDED,
				(new Boolean(_manager.supernodeNeeded())).toString());
		
		//give own IP address
		ret.put(ConnectionHandshakeHeaders.LISTEN_IP,
				Message.ip2string(RouterService.getAddress())+":"
				+ RouterService.getPort());
		
		//also add some host addresses in the response
		addHostAddresses(ret, _manager);
		
		//Decide whether to allow or reject.  Somewhat complicated because
		//of ultrapeer guidance.

		// TODO::add special cases for the type of client we're connecting to
		if (reject(response)) {
			return new HandshakeResponse(HandshakeResponse.SLOTS_FULL,
										 HandshakeResponse.SLOTS_FULL_MESSAGE,
										 ret);
		}
		
		return new HandshakeResponse(ret);
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
		if(userAgent == null) return false;
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
        //incoming connection is an ultrapeer (A&&!L) because of supernode
        //guidance; we don't know whether they'll become a leaf node or not.  So
        //we use the following conservative test, and depend on the
        //old-fashioned reject connection mechanism in ConnectionManager for the
        //other cases.       

        boolean allowedNow = _manager.allowConnection(response);

		boolean allowedAsLeaf = _manager.allowConnectionAsLeaf(response);

        //Reject if not allowed now and guidance not possible.
        return ! (allowedNow || (response.isSupernodeConnection() && allowedAsLeaf));
    }
}

