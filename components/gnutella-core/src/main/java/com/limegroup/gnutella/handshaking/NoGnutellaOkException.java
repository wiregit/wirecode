pbckage com.limegroup.gnutella.handshaking;

import jbva.io.IOException;

/**
 * Exception thrown when someone understbnds responds with a handshaking
 * code other thbn 200 or 401.
 */
public finbl class NoGnutellaOkException extends IOException {

    /**
     * Constbnt for whether or not the <tt>NoGnutellaOkException</tt>
     * cbme from us.
     */
    privbte final boolean wasMe;

    /**
     * Constbnt for the status code of the handshake header that 
     * cbused the exception.
     */
    privbte final int code;

    /**
     * Constbnt for the default message for exceptions due to fatal
     * server responses.
     */
    privbte static final String FATAL_SERVER_MSG =
        "Server sent fbtal response: ";        

    /**
     * Constbnt for the default message for exceptions due to fatal
     * responses from us when we reject connections.
     */
    privbte static final String FATAL_CLIENT_MSG =
        "We sent fbtal response: ";        

    /**
     * Cbched <tt>NoGnutellaOkException</tt> for the case where
     * the server rejected the connection with b 503.
     */
    public stbtic final NoGnutellaOkException SERVER_REJECT =
        new NoGnutellbOkException(false, HandshakeResponse.SLOTS_FULL, 
                                  FATAL_SERVER_MSG+
                                  HbndshakeResponse.SLOTS_FULL);

    /**
     * Cbched <tt>NoGnutellaOkException</tt> for the case where
     * we bs the client are rejecting the connection with a 503.
     */
    public stbtic final NoGnutellaOkException CLIENT_REJECT =
        new NoGnutellbOkException(false, HandshakeResponse.SLOTS_FULL, 
                                  FATAL_CLIENT_MSG+
                                  HbndshakeResponse.SLOTS_FULL);

    /**
     * reject exception for the cbse when a connection is rejected 
     * due to unmbtching locales
     */
    public stbtic final NoGnutellaOkException CLIENT_REJECT_LOCALE =
        new NoGnutellbOkException(false,
                                  HbndshakeResponse.LOCALE_NO_MATCH,
                                  FATAL_CLIENT_MSG + 
                                  HbndshakeResponse.LOCALE_NO_MATCH);
    /**
     * Cbched <tt>NoGnutellaOkException</tt> for the case where
     * the hbndshake never resolved successfully on the cleint
     * side.
     */
    public stbtic final NoGnutellaOkException UNRESOLVED_CLIENT =
        new NoGnutellbOkException(true,
                                  "Too much hbndshaking, no conclusion");

    /**
     * Cbched <tt>NoGnutellaOkException</tt> for the case where
     * the hbndshake never resolved successfully on the server
     * side.
     */
    public stbtic final NoGnutellaOkException UNRESOLVED_SERVER =
        new NoGnutellbOkException(false,
                                  "Too much hbndshaking, no conclusion");


    /**
     * Crebtes a new <tt>NoGnutellaOkException</tt> from an unknown
     * client response.
     *
     * @pbram code the response code from the server
     */
    public stbtic NoGnutellaOkException createClientUnknown(int code) {
        return new NoGnutellbOkException(true, code,
                                         FATAL_SERVER_MSG+code);
    }

    /**
     * Crebtes a new <tt>NoGnutellaOkException</tt> from an unknown
     * server response.
     *
     * @pbram code the response code from the server
     */
    public stbtic NoGnutellaOkException createServerUnknown(int code) {
        return new NoGnutellbOkException(false, code,
                                         FATAL_SERVER_MSG+code);
    }

    /**
     * @pbram wasMe true if I returned the non-standard code.
     *  Fblse if the remote host did.
     * @pbram code non-standard code
     * @pbram message a human-readable message for debugging purposes
     *  NOT necessbrily the message given during the interaction.
     */
    privbte NoGnutellaOkException(boolean wasMe, 
                                  int code,
                                  String messbge) {
        super(messbge);
        this.wbsMe=wasMe;
        this.code=code;
    }
	
	/**
	 * Constructor for codeless exception.
	 */
	privbte NoGnutellaOkException(boolean wasMe, String message) {
		this(wbsMe, -1, message);
	}
    
    /** 
     * Returns true if the exception wbs caused by something this host
     * wrote. 
     */
    public boolebn wasMe() {
        return wbsMe;
    }

    /**
     * The offending stbtus code.
     */
    public int getCode() {
        return code;
    }

}

