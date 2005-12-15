
// Commented for the Learning branch

package com.limegroup.gnutella.handshaking;

import java.io.IOException;

/**
 * This interface defines a method, called respond, which another class can implement to become a handshake responder.
 * The respond method takes a group of Gnutella handshake headers and composes the next group.
 * Both groups are stored in HandshakeResponse objects.
 * 
 * Provides a servent ways to set connection handshake responses in response to 
 * a connection handshake response just received.  Note, however, incoming 
 * connections and outgoing connections will differ in the use 
 * of this interface.  
 * 
 * Outgoing connections use the interface after receiving a handshake response
 * from a remote host that it tried to connect to.  Here is a typical anonymous 
 * implementation of HandshakeResponder for outgoing connections:
 * <pre>
 * new HandshakeResponder() {
 *     public HandshakeResponse respond(HandshakeResponse response, 
 *                                      boolean outgoing) {
 *          //Checks for a "200 OK" response and sends a "userid" header
 *          //otherwise, returns null.  Also, checks to make sure the 
 *          //the connection is an outgoing one.
 *          if (!outgoing)
 *              return null;
 *          if (response.getStatusCode() == HandshakeResponse.OK) {
 *              Properties headers = new Properties();
 *              headers.setProperty("Userid", "Limewire");
 *              return new HandshakeResponse(headers);
 *          }
 *          return null;
 *     }
 * }
 * </pre>
 *
 * Incoming connections use the interface after reading headers from a remote
 * host.  Hence, they don't care about the status code and status response, only
 * the headers they received.  Here is a typical anonymous implementation of 
 * HandshakeResponder for incoming connections:
 * 
 * <pre>
 * new HandshakeResponder() {
 *     public HandshakeResponse respond(HandshakeResponse response, 
 *                                      boolean outgoing) {
 *          //first, checks to make sure the connection is an incoming one.  
 *          //Also, checks for a "userid" header and if not sets the 
 *          //"Authorization" header and appropriate status code.
 *          if (outgoing)
 *              return null;
 *          Properties read = response.getHeaders();
 *          if (read.getProperty("userid")== null) {
 *              Properties headers = new Properties();
 *              headers.setProperty("Authorization", "needed");
 *              return new HandshakeResponse(401, "Unauthorized", headers);
 *          }
 *          //return "200 OK" with no headers
 *          return new HandshakeResponse();
 *     }
 * }
 * </pre>
 */
public interface HandshakeResponder {
	
    /** 
     * Use when we receive a group of handshake headers from a remote computer.
     * Returns the group of handshake headers we should send in response.
     * Implementations need to respond differently depending on which computer initiated the connection.
     * 
     * @param response The group of handshake headers the remote computer sent us
     * @param outgoing True if we connected to the remote computer, false if it connected to us
     */
    public HandshakeResponse respond(HandshakeResponse response, boolean outgoing) throws IOException;

    /**
     * A Gnutella program can send a header like "X-Locale-Pref: en" to say it is configured for the English language
     * Call setLocalePreferencing(true) to make this handshake responder refuse connections from foreign language remote computers
     * This method is optional, when implementing this interface, you don't have to write code for it.
     * This method should throw an UnsupportedOperationException.
     */
    public void setLocalePreferencing(boolean b);
}
