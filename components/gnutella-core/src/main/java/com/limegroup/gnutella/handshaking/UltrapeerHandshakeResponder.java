
// Commented for the Learning branch

package com.limegroup.gnutella.handshaking;

import java.util.Properties;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.statistics.HandshakingStat;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * Use when we're an ultrapeer.
 * Given a group of handshake headers from the remote computer, this class composes our response.
 */
public class UltrapeerHandshakeResponder extends DefaultHandshakeResponder {

	/**
	 * Make an UltrapeerHandshakeResponder object that can read a group of headers and compose our response
	 * 
	 * @param host The IP address of the remote computer
     */
    public UltrapeerHandshakeResponder(String host) {
    	
    	// Call the DefaultHandshakeResponder constructor to set _manager and _host
        super(host); // It saves the remote computer's IP address in _host
    }

    /**
     * We are an ultrapeer.
     * We initiated a connection and sent stage 1 headers.
     * Then, the remote computer sent its stage 2 headers, passed in here as response.
     * Now, this method composes the stage 3 headers we'll send to finish the handshake.
     * 
     * @param response The stage 2 headers the remote computer sent us in response to our connection and headers
     * @return         The stage 3 headers we'll send to finish the handshake
     */
    protected HandshakeResponse respondToOutgoing(HandshakeResponse response) {

    	// Ask the connection manager if we have an open slot for this connection
		if (!_manager.allowConnection(response)) {
			
			// All our slots are full
		    HandshakingStat.UP_OUTGOING_REJECT_FULL.incrementStat(); // Record we don't have room for another ultrapeer connection
            return HandshakeResponse.createRejectOutgoingResponse(); // Send rejection headers without "X-Try-Ultrapeers"
        }
		
		// Make a new local hash table of strings called ret to hold Gnutella headers
		Properties ret = new Properties();

		// The remote computer said "X-Ultrapeer-Needed: false"
        if (response.hasLeafGuidance()) {

        	// If we could become a leaf and this looks like a good ultrapeer
            if (_manager.allowLeafDemotion() && response.isGoodUltrapeer()) {

            	// Record we demoted ourselves to leaf status one more time
                HandshakingStat.UP_OUTGOING_GUIDANCE_FOLLOWED.incrementStat();
                
                // Add "X-Ultrapeer: False" to the local ret table of headers
                ret.put(HeaderNames.X_ULTRAPEER, "False");
                
            // The remote computer invited us to drop down to leaf mode, but we're not going to do it
            } else {
            	
            	// Record we ignored one more invitation to become a leaf
                HandshakingStat.UP_OUTGOING_GUIDANCE_IGNORED.incrementStat();
                
                // Keep going in this method to accept the connection
            }

        // The remote computer didn't say it has too many ultrapeer connections
		} else {
			
			// Record we accepted one more outgoing connection to an ultrapeer that didn't tell us to become a leaf
			HandshakingStat.UP_OUTGOING_ACCEPT.incrementStat();
		}

        // The remote computer's stage 2 headers say it can accept compressed data
		if (response.isDeflateAccepted()) {
			
			// Tell it we will be sending it compressed data
		    ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE); // "Content-Encoding: deflate"
		}

		// We're going to accept the connection, compose and return stage 3 headers
		return HandshakeResponse.createAcceptOutgoingResponse(ret); // Include "X-Ultrapeer: False" and "Content-Encoding: deflate" we may have added here
	}

    /**
     * We are an ultrapeer.
     * The remote computer connected to us and sent stage 1 headers.
     * Now, this method composes the stage 2 headers of our response.
     * 
     * @param response A HandshakeResponse object with the stage 1 headers from the remote computer
     * @return         A HandshakeResponse object with the stage 2 headers we'll send
     */
    protected HandshakeResponse respondToIncoming(HandshakeResponse response) {
    	
    	// The crawler connected to us
		if (response.isCrawler()) {
			
			// Record that the crawler connected to us again, and compose our response to it
		    HandshakingStat.INCOMING_CRAWLER.incrementStat();
			return HandshakeResponse.createCrawlerResponse();
		}
		
		// Make ret, a hash table of stage 2 headers we'll send as an ultrapeer
		Properties ret = new UltrapeerHeaders(getRemoteIP()); // Tell the remote computer its IP address
		
		// Tell the remote computer our IP address with a header like "Listen-IP: 10.254.0.101:20158"
		ret.put(HeaderNames.LISTEN_IP, NetworkUtils.ip2string(RouterService.getAddress()) + ":" + RouterService.getPort());
		
		// Decide if we should make this connection or not, including the fact that we can tell an ultrapeer to become a leaf
		if (reject(response, ret)) { // Give it their stage 1 headers and our stage 2 headers

			// Reject the connection, but tell the remote computer other ultrapeers it could try
            return HandshakeResponse.createUltrapeerRejectIncomingResponse(response);
		}

		// We do this last to prevent reject connections from being compressed
		// Compressing reject connections might actually increase the amount of bandwidth needed
		// TODO: This doesn't make any sense, if we reject the connection there won't be a content body to be compressed or not

		// In its stage 1 headers, the remote computer said it accepts compressed data
		if (response.isDeflateAccepted()) {

			// Say we will be sending compressed data
			ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE); // "Content-Encoding: deflate"
		}
		
		// Accept the connection
		// Tell the remote computer about ultrapeers many hops away to avoid messages that loop back
        return HandshakeResponse.createAcceptIncomingResponse(response, ret);
	}

    /**
     * Decides whether we should accept this connection or not.
     * Balances leaf and ultrapeer numbers for us and for the network.
     * Calls the connection manager to find out if we have a slot for this incoming connection, and if the network needs a supernode.
     * Can tell the remote computer to drop down to leaf status.
     * 
     * @param response The stage 1 headers from the remote computer
     * @param ret      The stage 2 headers we're composing in response
     * @return         True if we should reject this connection with a 503, false to accept with 200
     */
    private boolean reject(HandshakeResponse response, Properties ret) {
    	
    	// Ask the connection manager if we should connect to this computer as a leaf
    	
    	// Ask the connection manager if we have an open slot for this connection, telling it the remote computer is a leaf
        boolean allowedAsLeaf = _manager.allowConnectionAsLeaf(response); // Give it the headers the remote computer connected to us with
        
        // If the remote computer is a leaf, we should accept or reject the connection based on what the connection manager just said
        // We can't ask a leaf to become an ultrapeer, so the connection manager was the final say
        if (response.isLeaf() ) {

        	// Go with what the connection manager said
        	if (!allowedAsLeaf) HandshakingStat.UP_INCOMING_REJECT_LEAF.incrementStat(); // Record the reject or accept
            else                HandshakingStat.UP_INCOMING_ACCEPT_LEAF.incrementStat();
            return !allowedAsLeaf; // Return the result
        }

        // The remote computer is an ultrapeer
        
        // Ask the connection manager if we need more connections to ultrapeers
        boolean supernodeNeeded = _manager.supernodeNeeded();
        
        // If we have space for another connection but don't need more ultrapeers, tell the remote computer to drop down to leaf mode
        if (allowedAsLeaf &&    // We have fewer than 30 leaves, and
        	!supernodeNeeded) { // We have fewer than 27 leaves filling our 30 leaf slots

        	// Record we told one more ultrapeer to become a leaf
            HandshakingStat.UP_INCOMING_GUIDED.incrementStat();
            
            // Add "X-Ultrapeer-Needed: false" to the stage 2 headers we'll send
            ret.put(HeaderNames.X_ULTRAPEER_NEEDED, Boolean.FALSE.toString());

            // Accept this connection
            return false;
        }
        
        // Ask the connection manager if we have an open slot for this connection
        boolean allowedAsUltrapeer = _manager.allowConnection(response);
        
        // If we can't accept a leaf and need an ultrapeer, see if we can accept the remote computer as an ultrapeer
        if (allowedAsUltrapeer) {

        	// Record we, as an ultrapeer, have accepted one more incoming connection from another ultrapeer
        	HandshakingStat.UP_INCOMING_ACCEPT_UP.incrementStat();

        	// Include a header like "X-Ultrapeer-Needed: true" in our response
            ret.put(HeaderNames.X_ULTRAPEER_NEEDED, Boolean.TRUE.toString()); // Sending this header isn't required

            // Accept this connection
            return false;
        }
        
        // In all other cases, we must reject the connection
        // These cases are:
        // (1) !allowedAsLeaf  && !allowedAsUltrapeer, the connection manager says we have no slots at all
        // (2) supernodeNeeded && !allowedAsUltrapeer, the network needs a supernode, but we have no slot for one
        // In 1, we can't accept the remote computer as a leaf or ultrapeer, so we must reject the connection
        // In 2, the network needs a supernode, but we can't fill that need
        // Theoretically, a program could allow them as a leaf even if a supernode was needed
        // But, that would lower the number well-connected supernodes, and hurt the network
        // This means that the last 10% of leaf slots will always be reserved for remote computers that can't be ultrapeers

        // Record one more rejected leaf or ultrapeer
        if (!allowedAsLeaf) HandshakingStat.UP_INCOMING_REJECT_NO_ROOM_LEAF.incrementStat();
        else                HandshakingStat.UP_INCOMING_REJECT_NO_ROOM_UP.incrementStat();

        // Reject this connection
        return true;
    }
}
