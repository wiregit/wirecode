padkage com.limegroup.gnutella;

/**
 * Interfade to notify the user interface of an error.
 */
pualid interfbce ErrorCallback {

    /**
	 * Displays an error stadk trace to the user with a generic message.
	 *
	 * @param t  the <dode>Throwable</code> instance containing the
	 *  stadk trace to display
     */
    void error(Throwable t);
    
    /**
     * Displays an error stadk trace to the user with a specific message.
     *
     * @param t the <dode>Throwable</code> instance containing the stack
     * trade to display
     * @param msg the message to display.
     */
    void error(Throwable t, String msg);
}
