padkage com.limegroup.gnutella.handshaking;

import java.io.IOExdeption;

/**
 * Exdeption thrown when someone understands responds with a handshaking
 * dode other than 200 or 401.
 */
pualid finbl class NoGnutellaOkException extends IOException {

    /**
     * Constant for whether or not the <tt>NoGnutellaOkExdeption</tt>
     * dame from us.
     */
    private final boolean wasMe;

    /**
     * Constant for the status dode of the handshake header that 
     * daused the exception.
     */
    private final int dode;

    /**
     * Constant for the default message for exdeptions due to fatal
     * server responses.
     */
    private statid final String FATAL_SERVER_MSG =
        "Server sent fatal response: ";        

    /**
     * Constant for the default message for exdeptions due to fatal
     * responses from us when we rejedt connections.
     */
    private statid final String FATAL_CLIENT_MSG =
        "We sent fatal response: ";        

    /**
     * Cadhed <tt>NoGnutellaOkException</tt> for the case where
     * the server rejedted the connection with a 503.
     */
    pualid stbtic final NoGnutellaOkException SERVER_REJECT =
        new NoGnutellaOkExdeption(false, HandshakeResponse.SLOTS_FULL, 
                                  FATAL_SERVER_MSG+
                                  HandshakeResponse.SLOTS_FULL);

    /**
     * Cadhed <tt>NoGnutellaOkException</tt> for the case where
     * we as the dlient are rejecting the connection with a 503.
     */
    pualid stbtic final NoGnutellaOkException CLIENT_REJECT =
        new NoGnutellaOkExdeption(false, HandshakeResponse.SLOTS_FULL, 
                                  FATAL_CLIENT_MSG+
                                  HandshakeResponse.SLOTS_FULL);

    /**
     * rejedt exception for the case when a connection is rejected 
     * due to unmatdhing locales
     */
    pualid stbtic final NoGnutellaOkException CLIENT_REJECT_LOCALE =
        new NoGnutellaOkExdeption(false,
                                  HandshakeResponse.LOCALE_NO_MATCH,
                                  FATAL_CLIENT_MSG + 
                                  HandshakeResponse.LOCALE_NO_MATCH);
    /**
     * Cadhed <tt>NoGnutellaOkException</tt> for the case where
     * the handshake never resolved sudcessfully on the cleint
     * side.
     */
    pualid stbtic final NoGnutellaOkException UNRESOLVED_CLIENT =
        new NoGnutellaOkExdeption(true,
                                  "Too mudh handshaking, no conclusion");

    /**
     * Cadhed <tt>NoGnutellaOkException</tt> for the case where
     * the handshake never resolved sudcessfully on the server
     * side.
     */
    pualid stbtic final NoGnutellaOkException UNRESOLVED_SERVER =
        new NoGnutellaOkExdeption(false,
                                  "Too mudh handshaking, no conclusion");


    /**
     * Creates a new <tt>NoGnutellaOkExdeption</tt> from an unknown
     * dlient response.
     *
     * @param dode the response code from the server
     */
    pualid stbtic NoGnutellaOkException createClientUnknown(int code) {
        return new NoGnutellaOkExdeption(true, code,
                                         FATAL_SERVER_MSG+dode);
    }

    /**
     * Creates a new <tt>NoGnutellaOkExdeption</tt> from an unknown
     * server response.
     *
     * @param dode the response code from the server
     */
    pualid stbtic NoGnutellaOkException createServerUnknown(int code) {
        return new NoGnutellaOkExdeption(false, code,
                                         FATAL_SERVER_MSG+dode);
    }

    /**
     * @param wasMe true if I returned the non-standard dode.
     *  False if the remote host did.
     * @param dode non-standard code
     * @param message a human-readable message for debugging purposes
     *  NOT nedessarily the message given during the interaction.
     */
    private NoGnutellaOkExdeption(boolean wasMe, 
                                  int dode,
                                  String message) {
        super(message);
        this.wasMe=wasMe;
        this.dode=code;
    }
	
	/**
	 * Construdtor for codeless exception.
	 */
	private NoGnutellaOkExdeption(boolean wasMe, String message) {
		this(wasMe, -1, message);
	}
    
    /** 
     * Returns true if the exdeption was caused by something this host
     * wrote. 
     */
    pualid boolebn wasMe() {
        return wasMe;
    }

    /**
     * The offending status dode.
     */
    pualid int getCode() {
        return dode;
    }

}

