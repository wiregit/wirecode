package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;
import java.util.Properties;
import java.io.IOException;

/**
 * A very simple responder to be used by supernodes during the
 * connection handshake while accepting incoming connections
 */
public class SupernodeHandshakeResponder 
    extends AuthenticationHandshakeResponder
{
    private ConnectionManager _manager;
    
    /**
     * Creates a new instance of ClientHandshakeResponder
     * @param manager Instance of connection manager, managing this
     * connection
     * @param router Instance of message router, to get correct local
     * address at runtime.
     * @param host The host with whom we are handshaking
     */
    public SupernodeHandshakeResponder(ConnectionManager manager, 
		String host)
    {
        super(manager, host);
        this._manager = manager;
    }
    
    //inherit doc comment
    protected HandshakeResponse respondUnauthenticated(
        HandshakeResponse response, boolean outgoing) throws IOException
    {
        
        if(!outgoing)
        {
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
            if (reject(outgoing, response.getHeaders())) {
                return new HandshakeResponse(
                    HandshakeResponse.SLOTS_FULL,
                    HandshakeResponse.SLOTS_FULL_MESSAGE,
                    ret);
            } else {
                return new HandshakeResponse(ret);
            }
        } else
        {
            //Outgoing connection.  If the other guy is ultrapeer unaware and I
            //already have enough old-fashioned connections, reject it.  We've
            //already given ultrapeer guidance at this point, so there's no
            //"second chance" like in the reject(..) method.
            Properties ret=new Properties();

			Properties headers = response.getHeaders();			
			String ultrapeerHeader =
				headers.getProperty(ConnectionHandshakeHeaders.X_SUPERNODE);

			boolean isUltrapeer=ConnectionHandshakeHeaders.isTrue(ultrapeerHeader);
			int connections = getNumIntraUltrapeerConnections(headers, isUltrapeer);
            if (!_manager.allowConnection(outgoing,
										  ultrapeerHeader,
                                          headers.getProperty(
                                              ConnectionHandshakeHeaders.USER_AGENT),
										  connections)) {
                return new HandshakeResponse(
                    HandshakeResponse.SLOTS_FULL,
                    HandshakeResponse.SLOTS_FULL_MESSAGE,
                    ret);
            }
            //Did the server request we become a leaf?
            String neededS=response.getHeaders().
            getProperty(ConnectionHandshakeHeaders.X_SUPERNODE_NEEDED);
            if (neededS!=null
                && !Boolean.valueOf(neededS).booleanValue()
                && _manager.allowClientMode())
            {
                //Fine, we'll become a leaf.
                ret.put(ConnectionHandshakeHeaders.X_SUPERNODE,
                "False");
            }
            return new HandshakeResponse(ret);
        }
    }
    
    /** 
     * Returns true if this incoming connections should be rejected with a 503. 
     */
    private boolean reject(boolean outgoing, Properties headers) { 
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
        
        String useragentHeader =
			headers.getProperty(ConnectionHandshakeHeaders.USER_AGENT);
        String ultrapeerHeader =
			headers.getProperty(ConnectionHandshakeHeaders.X_SUPERNODE);
        boolean isUltrapeer=ConnectionHandshakeHeaders.isTrue(ultrapeerHeader);

		int degree = getNumIntraUltrapeerConnections(headers, isUltrapeer);

        boolean allowedNow=_manager.allowConnection(
            outgoing, ultrapeerHeader, useragentHeader, degree);
        boolean allowedAsLeaf=_manager.allowConnection(
            outgoing, ConnectionHandshakeHeaders.FALSE, useragentHeader, degree);

        //Reject if not allowed now and guidance not possible.
        return ! (allowedNow || (isUltrapeer && allowedAsLeaf));
    }    


	/**
	 * Accessor for the reported number of intra-Ultrapeer connections
	 * this connection attempts to maintain.  If the node is not an
	 * Ultrapeer, this returns 0.  If it is an Ultrapeer but does not
	 * support this header, we assume that it tries to maintain 6 intra-
	 * Ultrapeer connections.
	 *
	 * @return the number of intra-Ultrapeer connections the connected node
	 *  attempts to maintain, as reported in the X-Degree handshake header
	 *  or guessed at otherwise
	 */
	private int getNumIntraUltrapeerConnections(Properties headers, 
												boolean isUltrapeer) {
		String connections = 
			headers.getProperty(ConnectionHandshakeHeaders.X_DEGREE);
		if(connections == null) {
			if(isUltrapeer) return 6;
			return 0;
		}
		return Integer.valueOf(connections).intValue();
	}
}

