package com.limegroup.gnutella.handshaking;

import java.io.IOException;

import com.limegroup.gnutella.http.ReadHeadersIOState;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.statistics.HandshakingStat;

/**
 * Superclass for HandshakeStates that are reading.
 */
abstract class ReadHandshakeState extends ReadHeadersIOState {
    
    protected ReadHandshakeState(HandshakeSupport support) {
        super(support,
              BandwidthStat.GNUTELLA_HEADER_DOWNSTREAM_BANDWIDTH,
              ConnectionSettings.MAX_HANDSHAKE_HEADERS.getValue(),
              ConnectionSettings.MAX_HANDSHAKE_LINE_SIZE.getValue());
    }
    
    /** The first state in an incoming handshake. */
    static class ReadRequestState extends ReadHandshakeState {
        ReadRequestState(HandshakeSupport support) {
            super(support);
        }

        /**
         * Ensures the initial connect line is GNUTELLA/0.6
         * or a higher version of the protocol.
         */
        protected void processConnectLine() throws IOException {
            if (!((HandshakeSupport)support).notLessThan06(connectLine))
                throw new IOException("not a valid connect string!");
        }

        /** Does nothing. */
        protected void processHeaders() throws IOException {}
    }
    
    /** The third state in an incoming handshake, or the second state in an outgoing handshake. */
    static class ReadResponseState extends ReadHandshakeState {
        ReadResponseState(HandshakeSupport support) {
            super(support);
        }
        
        /** Ensures that the connect line began with GNUTELLA/0.6 */
        protected void processConnectLine() throws IOException {
            // We do this here, as opposed to in other states, so that
            // our response to the crawler can go through the wire prior
            // to closing the connection.
            // In the case of a crawler, this will normally go:
            // ReadRequestState -> WriteResponseState -> ReadResponseState
            // Normally, ReadResponseState will never get hit because the
            // crawler won't respond & the connection will timeout.
            // However, if it does get hit, we need to close the connection
            if(((HandshakeSupport)support).getReadHandshakeResponse().isCrawler())
                throw new IOException("crawler");
            
            if (!((HandshakeSupport)support).isConnectLineValid(connectLine)) {
                HandshakingStat.INCOMING_BAD_CONNECT.incrementStat();
                throw new IOException("Bad connect string");
            }
        }

        /** Ensures that the response contained a valid status code. */
        protected void processHeaders() throws IOException {
            HandshakeResponse theirResponse = ((HandshakeSupport)support).createRemoteResponse(connectLine);
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
}
