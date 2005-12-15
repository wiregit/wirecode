
// Commented for the Learning branch

package com.limegroup.gnutella.handshaking;

import java.io.IOException;

/**
 * If a computer refuses a connection by sending a group of handshake headers that don't start "200 OK", throw a NoGnutellaOkException.
 * 
 * You can make a new NoGnutellaOkException and throw it.
 * Or, throw one one of the public static final NoGnutellaOkException objects premade here for your use.
 * They have names like NoGnutellaOkException.SERVER_REJECT and NoGnutellaOkException.CLIENT_REJECT.
 * 
 * This design is done for ease and performance.
 * Instead of having to make a new NoGnutellaOkException, you can just pick the one you want from those here.
 * Java can reference these objects multiple times, and doesn't have to create and delete exception objects every time you throw one.
 */
public final class NoGnutellaOkException extends IOException {

	// Member variables

    /**
     * True if it was us that refused the connection by saying something other than "200 OK".
     * False if the remote computer sent us the rejection.
     */
    private final boolean wasMe;

    /**
     * The status code in the first line of the group of handshake headers that isn't 200 OK.
     */
    private final int code;

    // Commonly used text

    /**
     * "Server sent fatal response: " When the remote computer sends a fatal server response, use this text in the exception.
     */
    private static final String FATAL_SERVER_MSG = "Server sent fatal response: ";

    /**
     * "We sent fatal response: " When we send a fatal response, use this text in the exception.
     */
    private static final String FATAL_CLIENT_MSG = "We sent fatal response: ";

    // Pre-made static final NoGnutellaOkException objects you can throw instead of making a new one

    /**
     * Throw NoGnutellaOkException.SERVER_REJECT when the remote computer sends us 503.
     * 
     * This exception is only thrown in one place, Connection.concludeOutgoingHandshake().
     * We connected to the remote computer and sent our stage 1 headers.
     * The remote computer replied with stage 2 headers that don't start with "200 OK".
     */
    public static final NoGnutellaOkException SERVER_REJECT = new NoGnutellaOkException(
        false,                                            // The remote computer refused the connection
        HandshakeResponse.SLOTS_FULL,                     // 503, the default rejection code
        FATAL_SERVER_MSG + HandshakeResponse.SLOTS_FULL); // Compose text for the exception like "Server sent fatal response: 503"

    /**
     * Throw NoGnutellaOkException.CLIENT_REJECT when we send the remote computer 503.
     * 
     * In the Connection class, both concludeIncomingHandshake and conclucdeOutgoingHandshake throw this exception.
     * In concluceOutgoingHandshake, we connected to the remote computer, it sent stage 2, and then we rejected it in stage 3.
     * In concludeIncomingHandshake, the remote computer connected to us, and we rejected it in stage 2.
     */
    public static final NoGnutellaOkException CLIENT_REJECT = new NoGnutellaOkException(

    	// TODO:kfaaborg this should be true, not false
        false,                                            // We refused the connection
        HandshakeResponse.SLOTS_FULL,                     // 503, the default rejection code
        FATAL_CLIENT_MSG + HandshakeResponse.SLOTS_FULL); // Compose text for the exception like "We sent fatal response: 503"

    /**
     * Throw NoGnutellaOkException.CLIENT_REJECT_LOCALE when the remote computer sends us 577 because of a language mismatch.
     * 
     * This exception is only thrown in one place, in Connection.concludeOutgoingHandshake().
     * We connected to the remote computer and sent our stage 1 headers.
     * The remote computer replied with stage 2 headers that indicate a different language, and we don't want to connect to foreign language computers.
     * 
     * This exception is only thrown in one place, Connection.concludeOutgoingHandshake().
     * We connected to the remote computer and sent our stage 1 headers.
     * The remote computer replied with stage 2 headers that start with "200 OK", but include a "X-Locale-Pref" header that doesn't match ours.
     * We rejected the connection in stage 3.
     * 
     * This feature, locale preferencing, is turned off by default.
     * So, this exception is never really used.
     */
    public static final NoGnutellaOkException CLIENT_REJECT_LOCALE = new NoGnutellaOkException(
        false,                                                 // The remote computer refused the connection
        HandshakeResponse.LOCALE_NO_MATCH,                     // Put 577 in the exception object, the code for a language mismatch
        FATAL_CLIENT_MSG + HandshakeResponse.LOCALE_NO_MATCH); // Compose text for the exception like "We sent fatal response: 577"

    /**
     * This exception was written for the unfinished challenge response handshaking feature, and is never used.
     */
    public static final NoGnutellaOkException UNRESOLVED_CLIENT = new NoGnutellaOkException(
        true,                                   // We refused the connection
        "Too much handshaking, no conclusion"); // Custom text for the exception

    /**
     * This exception was written for the unfinished challenge response handshaking feature, and is never used.
     */
    public static final NoGnutellaOkException UNRESOLVED_SERVER = new NoGnutellaOkException(
        false,                                  // The remote computer refused the connection
        "Too much handshaking, no conclusion"); // Custom text for the exception

    // Make a new custom NoGnutellaOkException to throw

    /**
     * We just refused a connection during the Gnutella handshake by sending a group of headers that doesn't begin "200 OK".
     * Make a new NoGnutellaOkException to throw to get us out of the handshaking code.
     * 
     * @param code We refused the connection by starting a group of handshake headers with this code, a number like 503
     */
    public static NoGnutellaOkException createClientUnknown(int code) {

    	// Pass the constructor true for we refused the connection, the status code we sent like 503, and compose text like "We sent fatal response: 503"
        return new NoGnutellaOkException(true, code, FATAL_SERVER_MSG + code);
        // TODO:kfaaborg This should be FATAL_CLIENT_MSG, not FATAL_SERVER_MSG
    }

    /**
     * The remote computer just refused a connection during the Gnutella handshake by sending a group of headers that doesn't begin "200 OK"
     * Make a new NoGnutellaOkException to throw to get us out of the handshaking code.
     * 
     * @param code The remote computer refused the connection by starting a group of handshake headers with this code, a number like 503
     */
    public static NoGnutellaOkException createServerUnknown(int code) {

    	// False for the remote computer refused the connection, the status code it sent us like 503, and compose text like "Server sent fatal response: 503"
        return new NoGnutellaOkException(false, code, FATAL_SERVER_MSG + code);
    }

    /**
     * Make a new NoGnutellaOkException.
     * This is the main constructor that the other constructors call.
     * 
     * @param wasMe   False if the remote computer refused the connection, true if it was us.
     * @param code    The computer refused the connection by starting a group of handshake headers with this code, which isn't "200 OK"
     * @param message Text composed in this class and not taken from the handshake, like "Server sent fatal response: 503"
     */
    private NoGnutellaOkException(boolean wasMe, int code, String message) {

    	// Save the parameters in this new exception object
        super(message);     // Call the IOException constructor to have it save the message text in this object
        this.wasMe = wasMe; // Save which computer refused the connection and with what status code
        this.code  = code;
    }

	/**
	 * Make a new NoGnutellaOkException for a situation when we don't know the status code.
	 * It's not "200 OK", but somehow we don't know what it is instead.
	 * 
	 * @param wasMe   False if the remote computer refused the connection, true if it was us
	 * @param message Text to store in the IOException a NoGnutellaOkException is
	 */
	private NoGnutellaOkException(boolean wasMe, String message) {

		// Call the main constructor with -1 as the status code instead of 503 or something else that isn't 200
		this(wasMe, -1, message);
	}
	
	// Read the member variables of this object

    /**
     * False if the remote computer refused the connection with a group of handshake headers that don't start "200 OK".
     * True if we refused the connection with a group of headers like this.
     */
    public boolean wasMe() {

    	// The constructor was told which side said 503, and saved it in this member variable
        return wasMe;
    }

    /**
     * The status code the computer said to refuse a Gnutella connection in the handshake.
     * 
     * @return The status code this NoGnutellaOkException was made with, like 503
     */
    public int getCode() {

    	// The constructor saved this in code
        return code;
    }
}
