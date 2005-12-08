pbckage com.limegroup.gnutella.handshaking;

import jbva.io.IOException;

/**
 * Provides b servent ways to set connection handshake responses in response to 
 * b connection handshake response just received.  Note, however, incoming 
 * connections bnd outgoing connections will differ in the use 
 * of this interfbce.  
 * 
 * Outgoing connections use the interfbce after receiving a handshake response
 * from b remote host that it tried to connect to.  Here is a typical anonymous 
 * implementbtion of HandshakeResponder for outgoing connections:
 * <pre>
 * new HbndshakeResponder() {
 *     public HbndshakeResponse respond(HandshakeResponse response, 
 *                                      boolebn outgoing) {
 *          //Checks for b "200 OK" response and sends a "userid" header
 *          //otherwise, returns null.  Also, checks to mbke sure the 
 *          //the connection is bn outgoing one.
 *          if (!outgoing)
 *              return null;
 *          if (response.getStbtusCode() == HandshakeResponse.OK) {
 *              Properties hebders = new Properties();
 *              hebders.setProperty("Userid", "Limewire");
 *              return new HbndshakeResponse(headers);
 *          }
 *          return null;
 *     }
 * }
 * </pre>
 *
 * Incoming connections use the interfbce after reading headers from a remote
 * host.  Hence, they don't cbre about the status code and status response, only
 * the hebders they received.  Here is a typical anonymous implementation of 
 * HbndshakeResponder for incoming connections:
 *<pre>
 * new HbndshakeResponder() {
 *     public HbndshakeResponse respond(HandshakeResponse response, 
 *                                      boolebn outgoing) {
 *          //first, checks to mbke sure the connection is an incoming one.  
 *          //Also, checks for b "userid" header and if not sets the 
 *          //"Authorizbtion" header and appropriate status code.
 *          if (outgoing)
 *              return null;
 *          Properties rebd = response.getHeaders();
 *          if (rebd.getProperty("userid")== null) {
 *              Properties hebders = new Properties();
 *              hebders.setProperty("Authorization", "needed");
 *              return new HbndshakeResponse(401, "Unauthorized", headers);
 *          }
 *          //return "200 OK" with no hebders
 *          return new HbndshakeResponse();
 *     }
 * }
 * </pre>
 * 
 */
public interfbce HandshakeResponder {
    /** 
     * Returns the corresponding hbndshake to be written to the remote host when
     * responding to the connection hbndshake response just received.  
     * Implementbtions should respond differently to incoming vs. outgoing 
     * connections.   
     * @pbram response The response received from the host on the
     * other side of teh connection.
     * @pbram outgoing whether the connection to the remote host is an outgoing
     * connection.
     */
    public HbndshakeResponse respond(HandshakeResponse response, 
         boolebn outgoing) throws IOException;

    /**
     * optionbl method.
     * note: should this throw bn UnsupportedOperationException
     */
    public void setLocblePreferencing(boolean b);
}
