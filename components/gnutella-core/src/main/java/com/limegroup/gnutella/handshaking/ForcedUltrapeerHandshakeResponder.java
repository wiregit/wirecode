/*
 * A class which forces the X-Ultrapeer : True header to be written
 * in a handshake.
 */
package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.util.Properties;

import com.limegroup.gnutella.statistics.HandshakingStat;


public class ForcedUltrapeerHandshakeResponder
		extends
			UltrapeerHandshakeResponder {
	public ForcedUltrapeerHandshakeResponder(String str) {
		super(str);
	}
	
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.handshaking.AuthenticationHandshakeResponder#respondUnauthenticated(com.limegroup.gnutella.handshaking.HandshakeResponse, boolean)
	 */
	protected HandshakeResponse respondUnauthenticated(
			HandshakeResponse response, boolean outgoing) throws IOException {
		
		Properties props = super.respondUnauthenticated(response, outgoing).props();
		props.put("X-Ultrapeer","True");
		
		if (outgoing)
			return HandshakeResponse.createAcceptOutgoingResponse(props);
		else
			return HandshakeResponse.createAcceptIncomingResponse(response,props);
	}
	
	/**
	 * overriden to accept the connection unless the other side asks us to 
	 * become a leaf; disregards whether we have a free connection slot or not.
	 */
	private HandshakeResponse respondToOutgoing(HandshakeResponse response) {
		
	
		Properties ret = new Properties();
		
		// They supposedly requested our promotion and are now giving us guidance?
		// reject the connection.
        if(response.hasLeafGuidance()) {
        	
        	return HandshakeResponse.createRejectOutgoingResponse();
        	
		} else if( RECORD_STATS )
            HandshakingStat.UP_OUTGOING_ACCEPT.incrementStat();

		// deflate if we can ...
		if(response.isDeflateAccepted()) {
		    ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}

        // accept the response
        return HandshakeResponse.createAcceptOutgoingResponse(ret);

			
	}
	
}
