package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;
import java.util.Properties;
import java.io.IOException;

/**
 * A very simple responder to be used by leaf-nodes during the
 * connection handshake while accepting incoming connections
 */
public final class LeafHandshakeResponder 
    extends AuthenticationHandshakeResponder {    
    
    /**
     * Creates a new instance of LeafHandshakeResponder
     * @param manager Instance of connection manager, managing this
     * connection
     * @param router Instance of message router, to get correct local
     * address at runtime.
     * @param host The host with whom we are handshaking
     */
    public LeafHandshakeResponder(String host) {
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

        // leaves should never accept connections to other leaves
        if(response.isLeaf()) {
            return HandshakeResponse.createRejectOutgoingResponse(new Properties());
        }
        // let the Ultrapeer know of any high-hops Ultrapeers
        // we're aware of
        return HandshakeResponse.createAcceptOutgoingResponse(new Properties());
    }

    
    /**
     * Responds to an incoming connection handshake.
     *
     * @return the <tt>HandshakeResponse</tt> with the handshake 
     *  headers to send in response to the connection attempt
     */
    private HandshakeResponse 
        respondToIncoming(HandshakeResponse hr) {
        Properties props = new LeafHeaders(getRemoteIP());
        

        if (RouterService.isLeaf()) {
            //a) If we're already a leaf, reject
            return HandshakeResponse.createRejectIncomingResponse(props);
        } 

        //b) We're not a leaf yet, so accept the incoming connection
        return HandshakeResponse.createAcceptIncomingResponse(props);
    }
}

