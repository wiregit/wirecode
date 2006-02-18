
// Commented for the Learning branch

package com.limegroup.gnutella.handshaking;

import java.io.IOException;

/**
 * There was a problem during the Gnutella handshake, and we should close our TCP socket connection to this remote computer.
 * 
 * During the Gnutella handshake, there are several things that can go wrong.
 * Here they are, listed in order of severity:
 * (1) The remote computer rejects us by starting a group of handshake headers that begins like "GNUTELLA/0.6 503 Service Unavailable".
 * (2) The remote computer responds to our greeting with something non-Gnutella, like HTTP headers.
 * (3) We loose our socket connection to the remote computer while we are doing the handshake.
 * If any of these things happen, we have to close the TCP socket connection to the remote computer.
 * 
 * LimeWire uses 3 exceptions when events like this happen:
 * IOException           - Java throws this when the socket closed or we reached the end.
 * NoGnutellaOkException - The remote computer began a group of handshake headers with something other than "GNUTELLA/0.6 200 OK".
 * BadHandshakeException - One of the 3 problems listed above occurred.
 * 
 * BadHandshakeException can hold an IOException inside it.
 * If an IOException happens and you want to hide it, put it in a BadHandshakeException.
 */
public class BadHandshakeException extends IOException {

    /**
     * Make a new BandHandshakeException to throw when we loose our connection during the Gnutella handshake, or there is a problem with the remote computer's response.
     * Only Connection.initialize() throws this.
     * 
     * @param originalCause The IOException that happened
     */
    public BadHandshakeException(IOException originalCause) {

        // Call the IOException constructor to set up this new object
        super();

        // Save the exception that caused this one to be thrown in it
        initCause(originalCause);
    }
}
