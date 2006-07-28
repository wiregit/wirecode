
// Commented for the Learning branch

package com.limegroup.gnutella.handshaking;

import java.util.Properties;

import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.statistics.HandshakingStat;

/**
 * Use when we're a leaf.
 * Given a group of handshake headers from the remote computer, this class composes our response.
 */
public final class LeafHandshakeResponder extends DefaultHandshakeResponder {
    
    /**
     * Make a LeafHandshakeResponder object that can read a group of headers and compose a response.
     * 
     * @param host The IP address of the remote computer
     */
    public LeafHandshakeResponder(String host) {
    	
    	// Call the DefaultHandshakeResponder constructor to setup this object
        super(host); // The remote computer's IP address will be kept in the member variable named _host
    }

    /**
     * We are a leaf.
     * We initiated a connection and sent stage 1 headers.
     * Then, the remote computer sent its stage 2 headers, passed in here as response.
     * Now, this method composes the stage 3 headers we'll send to finish the handshake.
     * 
     * @param response The stage 2 headers the remote computer sent us in response to our connection and headers
     * @return         The stage 3 headers we'll send to finish the handshake
     */
    protected HandshakeResponse respondToOutgoing(HandshakeResponse response) {

    	// We're a leaf, so we can't connect to another leaf
    	// If the remote computer says it's not an ultrapeer, we need to reject the connection
        if (!response.isUltrapeer()) {

        	/*
        	 * Tour Point: We're a leaf, and accidentally connected to another leaf.
        	 * It responded with stage 2 headers.
        	 * We'll send a stage 3 of "503 I am a shielded leaf node".
        	 */

        	// Record that one more leaf tried to connect to us
            HandshakingStat.LEAF_OUTGOING_REJECT_LEAF.incrementStat();

            // Compose and return stage 3 rejection headers
            return HandshakeResponse.createLeafRejectOutgoingResponse();
        }

        // If this is a preferenced connection
        if (getLocalePreferencing()) {

        	// TODO: Add the statistic HandshakingStat.LEAF_OUTGOING_REJECT_LOCALE.incrementStat();
        	
        	// If the langauge setting here, like English, and the language setting for the remote computer don't match
            if (!ApplicationSettings.LANGUAGE.getValue().equals(response.getLocalePref())) {
            	
            	// Reject the connection because the languages don't match
                return HandshakeResponse.createLeafRejectLocaleOutgoingResponse();
            }
        }

        // Give the stage 2 headers to the connection manager to see if it has a problem with them
        if (!_manager.allowConnection(response)) {

        	// Record that this is another bad ultrapeer we're going to avoid
        	HandshakingStat.LEAF_OUTGOING_REJECT_OLD_UP.incrementStat();

            // Compose and return stage 3 rejection headers
        	return HandshakeResponse.createLeafRejectOutgoingResponse();
        }
        
        // Make a new local hash table of strings named ret
        Properties ret = new Properties();

        // If the remote computer's stage 2 headers say that it accepts compressed data
		if (response.isDeflateAccepted()) {

			// Add the header "Content-Encoding: deflate" to ret
			ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}
		
		// Record that as a leaf, we accepted one more connection
        HandshakingStat.LEAF_OUTGOING_ACCEPT.incrementStat();

        // Compose stage 3 headers, including the "Content-Encoding: deflate" one we might have just added above
        return HandshakeResponse.createAcceptOutgoingResponse(ret);
    }

    /**
     * We are a leaf.
     * A remote computer connected to us and sent stage 1 headers.
     * Now, this method composes the stage 2 headers of our response.
     * 
     * @param hr A HandshakeResponse object with the stage 1 headers from the remote computer
     * @return   A HandshakeResponse object with the stage 2 headers we'll send
     */
    protected HandshakeResponse respondToIncoming(HandshakeResponse hr) {
    	
    	// The crawler connected to us
		if (hr.isCrawler()) {

			// Record that the crawler connected to us again, and compose our response to it
			HandshakingStat.INCOMING_CRAWLER.incrementStat();
			return HandshakeResponse.createCrawlerResponse();
		}
		
		// We're a leaf, so we can't connect to other leaves
		// If the remote computer connected to us and said it's a leaf
        if (!hr.isUltrapeer()) {

        	/*
        	 * Tour Point: We're a leaf, and an ultrapeer tried to connect to us.
        	 * Leaves don't accept connections, so we'll send a stage 2 rejection of "503 I am a shielded leaf node".
        	 */

        	// Record that another leaf tried to connect to us
            HandshakingStat.LEAF_INCOMING_REJECT.incrementStat();

            // Compose and return stage 2 rejection headers
            return HandshakeResponse.createLeafRejectOutgoingResponse();
        }		

        // Make a local hash table with the Gnutella headers we send, including "X-Ultrapeer: false"
        Properties ret = new LeafHeaders(getRemoteIP()); // Get the remote computer's IP address

        // If we have enough connections to ultrapeers already
        if (!_manager.allowConnection(hr)) {
        	
        	// Reject the connection
            HandshakingStat.LEAF_INCOMING_REJECT.incrementStat();          // Record we rejected one more connection as a leaf
            return HandshakeResponse.createLeafRejectIncomingResponse(hr); // Reject the connection as a leaf
        }
        
        // If the remote compuer says it can accept compressed data
		if (hr.isDeflateAccepted()) {
			
			// Tell the remote computer we'll be sending the data compressed
		    ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE); // "Content-Encoding: deflate"
		}         

		// Record we accepted one more connection as a leaf
        HandshakingStat.LEAF_INCOMING_ACCEPT.incrementStat();

        // We accept the incoming connection and send stage 2 headers
        return HandshakeResponse.createAcceptIncomingResponse(hr, ret); // Give it the stage 1 headers and our composed response
    }
}
