/*
 * A class which forces the X-Ultrapeer : True header to be written
 * in a handshake.
 */
package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.util.Properties;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.statistics.HandshakingStat;
import com.limegroup.gnutella.util.*;


public class ForcedUltrapeerHandshakeResponder
		extends
			AuthenticationHandshakeResponder {
	
	public ForcedUltrapeerHandshakeResponder(String str) {
		super(RouterService.getConnectionManager(), str);
	}
	
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.handshaking.AuthenticationHandshakeResponder#respondUnauthenticated(com.limegroup.gnutella.handshaking.HandshakeResponse, boolean)
	 */
	protected HandshakeResponse respondUnauthenticated(
			HandshakeResponse response, boolean outgoing) throws IOException {
		
		if(outgoing) return respondToOutgoing(response);
		return respondToIncoming(response);	
	}
	
	/**
	 * Accepts a connection from the client which we are promoting.
	 * We must accept this connection disregarding the number of free slots.
	 */
	private HandshakeResponse respondToIncoming(HandshakeResponse response) throws IOException {
		
		//if this is a connections from the crawler, freak out 
		if(response.isCrawler()) 
			throw new IOException("candidate leaf sent crawler header???");
		
		//the other side should be a leaf posing as ultrapeer.
		if (!response.isUltrapeer()) 
			return HandshakeResponse.createRejectOutgoingResponse();
	
		//Incoming connection....
		Properties ret = new UltrapeerHeaders(getRemoteIP());
		
		//give own IP address
		ret.put(HeaderNames.LISTEN_IP,
				NetworkUtils.ip2string(RouterService.getAddress())+":"
				+ RouterService.getPort());
		
		
		//deflate
		if(response.isDeflateAccepted()) {
		    ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}
		
		// accept the connection, and let the connecting node know about 
        // Ultrapeers that are as many hops away as possible, to avoid 
        // cycles.
        return HandshakeResponse.createAcceptIncomingResponse(response, ret);
		
		
	}
	
	/**
	 * overriden to accept the connection unless the other side asks us to 
	 * become a leaf; disregards whether we have a free connection slot or not.
	 */
	private HandshakeResponse respondToOutgoing(HandshakeResponse response) {
		
		// They supposedly requested our promotion and are now giving us guidance?
		// reject the connection.
        if(response.hasLeafGuidance() || response.isLeaf() ) 
           	return HandshakeResponse.createRejectOutgoingResponse();
		 else if( RECORD_STATS )
            HandshakingStat.UP_OUTGOING_ACCEPT.incrementStat();

		Properties ret = new Properties();
		 
		// deflate if we can ...
		if(response.isDeflateAccepted()) {
		    ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}
		
		//add the UP header
		ret.put(HeaderNames.X_ULTRAPEER,"True");
		
		return HandshakeResponse.createAcceptOutgoingResponse(ret);
			
	}
	
}
