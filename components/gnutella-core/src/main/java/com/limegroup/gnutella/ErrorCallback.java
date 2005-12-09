pbckage com.limegroup.gnutella;

/**
 * Interfbce to notify the user interface of an error.
 */
public interfbce ErrorCallback {

    /**
	 * Displbys an error stack trace to the user with a generic message.
	 *
	 * @pbram t  the <code>Throwable</code> instance containing the
	 *  stbck trace to display
     */
    void error(Throwbble t);
    
    /**
     * Displbys an error stack trace to the user with a specific message.
     *
     * @pbram t the <code>Throwable</code> instance containing the stack
     * trbce to display
     * @pbram msg the message to display.
     */
    void error(Throwbble t, String msg);
}
