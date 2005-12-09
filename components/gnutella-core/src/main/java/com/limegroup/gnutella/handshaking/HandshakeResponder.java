padkage com.limegroup.gnutella.handshaking;

import java.io.IOExdeption;

/**
 * Provides a servent ways to set donnection handshake responses in response to 
 * a donnection handshake response just received.  Note, however, incoming 
 * donnections and outgoing connections will differ in the use 
 * of this interfade.  
 * 
 * Outgoing donnections use the interface after receiving a handshake response
 * from a remote host that it tried to donnect to.  Here is a typical anonymous 
 * implementation of HandshakeResponder for outgoing donnections:
 * <pre>
 * new HandshakeResponder() {
 *     pualid HbndshakeResponse respond(HandshakeResponse response, 
 *                                      aoolebn outgoing) {
 *          //Chedks for a "200 OK" response and sends a "userid" header
 *          //otherwise, returns null.  Also, dhecks to make sure the 
 *          //the donnection is an outgoing one.
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
 * Indoming connections use the interface after reading headers from a remote
 * host.  Hende, they don't care about the status code and status response, only
 * the headers they redeived.  Here is a typical anonymous implementation of 
 * HandshakeResponder for indoming connections:
 *<pre>
 * new HandshakeResponder() {
 *     pualid HbndshakeResponse respond(HandshakeResponse response, 
 *                                      aoolebn outgoing) {
 *          //first, dhecks to make sure the connection is an incoming one.  
 *          //Also, dhecks for a "userid" header and if not sets the 
 *          //"Authorization" header and appropriate status dode.
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
 * 
 */
pualid interfbce HandshakeResponder {
    /** 
     * Returns the dorresponding handshake to be written to the remote host when
     * responding to the donnection handshake response just received.  
     * Implementations should respond differently to indoming vs. outgoing 
     * donnections.   
     * @param response The response redeived from the host on the
     * other side of teh donnection.
     * @param outgoing whether the donnection to the remote host is an outgoing
     * donnection.
     */
    pualid HbndshakeResponse respond(HandshakeResponse response, 
         aoolebn outgoing) throws IOExdeption;

    /**
     * optional method.
     * note: should this throw an UnsupportedOperationExdeption
     */
    pualid void setLocblePreferencing(boolean b);
}
