package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.limegroup.gnutella.statistics.HandshakingStat;

/**
 * An incoming handshaker that blocks while handshaking.
 */
public class BlockingIncomingHandshaker implements Handshaker {
    
    private HandshakeResponder responder;
    private BlockingHandshakeSupport support;

    /**
     * Constructs a new BlockingIncomingHandshaker using the given responder
     * to calculate our response, and the Socket/InputStream/OutputStream for i/o.
     *  
     * @param responder
     * @param socket
     * @param in
     * @param out
     */
    public BlockingIncomingHandshaker(HandshakeResponder responder, Socket socket, InputStream in, OutputStream out) {
        this.responder = responder;
        this.support = new BlockingHandshakeSupport(socket, in, out);
    }

    /** Performs an incoming handshake. */
    public void shake() throws IOException, BadHandshakeException, NoGnutellaOkException {
        initializeIncoming();
        concludeIncomingHandshake();
    }

    /** Returns all headers we read while connecting. */
    public HandshakeResponse getReadHeaders() {
        return support.getReadHandshakeResponse();
    }

    /** Returns all headers we wrote while connecting. */
    public HandshakeResponse getWrittenHeaders() {
        return support.getWrittenHandshakeResponse();
    }
    
    /** 
     * Sends and receives handshake strings for incoming connections,
     * throwing exception if any problems. 
     * 
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK
     * @exception IOException if there's an unexpected connect string or
     *  any other problem
     */
    private void initializeIncoming() throws IOException {
        String connectString = support.readLine();
        if (support.notLessThan06(connectString))
            support.readHeaders();
        else
            throw new IOException("Unexpected connect string: "+connectString);
    }
    
    /**
     * Responds to the handshake from the host on the other
     * end of the connection, till a conclusion reaches. Handshaking may
     * involve multiple steps.
     * 
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK
     * @exception IOException any other error.
     */
    private void concludeIncomingHandshake() throws IOException {
        HandshakeResponse theirRequest = support.getReadHandshakeResponse();
        HandshakeResponse ourResponse = responder.respond(theirRequest, false);
        support.writeResponse(ourResponse);
        
        // if it was the crawler, leave early.
        if(theirRequest.isCrawler()) {
            // read one response, just to make sure they got ours.
            support.readLine();
            throw new IOException("crawler");
        }
        
        switch(ourResponse.getStatusCode()) {
        case HandshakeResponse.OK:
            break;
        case HandshakeResponse.SLOTS_FULL:
            HandshakingStat.INCOMING_CLIENT_REJECT.incrementStat();
            throw NoGnutellaOkException.CLIENT_REJECT;
        default: 
            HandshakingStat.INCOMING_CLIENT_UNKNOWN.incrementStat();
            throw NoGnutellaOkException.createClientUnknown(ourResponse.getStatusCode());
        }
                
        String connectLine = support.readLine();
        if (!support.isConnectLineValid(connectLine)) {
            HandshakingStat.INCOMING_BAD_CONNECT.incrementStat();
            throw new IOException("Bad connect string");
        }

        support.readHeaders();
        HandshakeResponse theirResponse = support.createRemoteResponse(connectLine);
        switch(theirResponse.getStatusCode()) {
        case HandshakeResponse.OK:
            HandshakingStat.SUCCESSFUL_INCOMING.incrementStat();
            break;
        default:
            HandshakingStat.INCOMING_SERVER_UNKNOWN.incrementStat();
            throw NoGnutellaOkException.createServerUnknown(theirResponse.getStatusCode());
        }
    }

}
