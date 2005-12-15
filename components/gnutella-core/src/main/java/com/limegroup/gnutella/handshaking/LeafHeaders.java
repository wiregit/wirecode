
// Commented for the Learning branch

package com.limegroup.gnutella.handshaking;

/**
 * A new LeafHeaders object contains all the handshake headers we send as a leaf.
 * 
 * LeafHeaders is a hash table of strings with keys like "User-Agent" and values like "LimeWire/4.9.33".
 * These are the headers we, as a leaf, will tell a remote computer during the handshake.
 * When you make a new LeafHeaders object, its constructor fills in all the Gnutella headers and values that we send.
 * Use LeafHeaders when we are a leaf, because one of the headers in it is "X-Ultrapeer: false".
 */
public class LeafHeaders extends DefaultHeaders {

    /**
     * Make a new LeafHeaders object.
     * It will contain a Properties hash table of strings that list all the Gnutella headers we tell remote computers.
     * 
     * @param remoteIP The IP address of the remote computer we are shaking hands with
     */
    public LeafHeaders(String remoteIP) {
    	
    	// Call the DefaultHeaders constructor, filling the hash table here with Gnutella headers
        super(remoteIP); // We'll tell the remote computer its IP address in a header like "Remote-IP: 67.41.102.170"

        // Add the header "X-Ultrapeer: false" to indicate that we are a leaf right now
        put(HeaderNames.X_ULTRAPEER, "False");
    }
}
