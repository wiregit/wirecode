/*
 * A class which forces the X-Ultrapeer : True header to be written
 * in a handshake.
 */
package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.util.Properties;


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
}
