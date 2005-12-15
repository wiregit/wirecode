
// Commented for the Learning branch

package com.limegroup.gnutella.handshaking;

/**
 * A new UltrapeerHeaders object contains all the handshake headers we send as an ultrapeer.
 *  
 * UltrapeerHeaders is a hash table of strings with keys like "User-Agent" and values like "LimeWire/4.9.33".
 * These are the headers we, as an ultrapper, will tell a remote computer in the handshake.
 * When you make a new UltrapeerHeaders object, its constructor fills in all the Gnutella headers and values that we send.
 * Use UltrapeerHeaders when we are an ultrapeer, because one of the headers in it is "X-Ultrapeer: true".
 */
public class UltrapeerHeaders extends DefaultHeaders {

    // We currently support version 0.1 of probes
	// If probes become a part of dynamic querying, we won't need a separate header for it anymore
    public final static String PROBE_VERSION = "0.1";

    /**
     * Make a new UltrapeerHeaderes object.
     * It will contain a Properties hash table of strings that list all the Gnutella headers we tell remote computers.
     * 
     * @param remoteIP The IP address of the remote computer we are shaking hands with
     */
    public UltrapeerHeaders(String remoteIP) {

    	// Call the DefaultHeaders constructor, filling the hash table here with Gnutella headers
    	super(remoteIP);

        // Add the header "X-Ultrapeer: True" to indicate that we are an ultrapeer right now
    	put(HeaderNames.X_ULTRAPEER, "True");

    	// Also include a header like "X-Ext-Probes: 0.1" to indicate that we can do this
    	put(HeaderNames.X_PROBE_QUERIES, PROBE_VERSION);
    }
}
