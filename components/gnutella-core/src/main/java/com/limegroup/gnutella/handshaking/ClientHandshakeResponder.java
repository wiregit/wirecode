package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;
import java.util.Properties;
import java.io.IOException;

/**
 * A very simple responder to be used by client-nodes during the
 * connection handshake while accepting incoming connections
 */
public final class ClientHandshakeResponder 
    extends AuthenticationHandshakeResponder {    
    
    /**
     * Creates a new instance of ClientHandshakeResponder
     * @param manager Instance of connection manager, managing this
     * connection
     * @param router Instance of message router, to get correct local
     * address at runtime.
     * @param host The host with whom we are handshaking
     */
    public ClientHandshakeResponder(String host) {
        super(RouterService.getConnectionManager(), host);
    }
    
    //inherit doc comment
    protected HandshakeResponse respondUnauthenticated(
        HandshakeResponse response, boolean outgoing) throws IOException {
        if (outgoing) return respondToOutgoing(response);
        return respondToIncoming(response);
    }


    /**
     * Responds to an outgoing connection handshake.
     *
     * @return the <tt>HandshakeResponse</tt> with the handshake 
     *  headers to send in response to the connection attempt
     */
    private HandshakeResponse 
        respondToOutgoing(HandshakeResponse response) {
        // let the Ultrapeer know of any high-hops Ultrapeers
        // we're aware of
        return HandshakeResponse.createAcceptResponse(new Properties());
    }

    
    /**
     * Responds to an incoming connection handshake.
     *
     * @return the <tt>HandshakeResponse</tt> with the handshake 
     *  headers to send in response to the connection attempt
     */
    private HandshakeResponse 
        respondToIncoming(HandshakeResponse response) {
        Properties props = new ClientProperties(getRemoteIP());
        
        if (RouterService.isLeaf()) {
            //b) Incoming, with ultrapeer connection: reject (redirect)
            return HandshakeResponse.createRejectResponse(props);
        } else {
            //c) Incoming, no ultrapeer: accept...until I find one
            return HandshakeResponse.createAcceptResponse(props);
        }
    }
}

