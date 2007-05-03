package org.limewire.service;

/**
 * Defines a generic error message and a method that displays a specific 
 * error message.
 */
public interface ErrorCallback {

    /**
	 * Displays an error stack trace to the user with a generic message.
	 *
	 * @param t  the <code>Throwable</code> instance containing the
	 *  stack trace to display
     */
    void error(Throwable t);
    
    /**
     * Displays an error stack trace to the user with a specific message.
     *
     * @param t the <code>Throwable</code> instance containing the stack
     * trace to display
     * @param msg the message to display.
     */
    void error(Throwable t, String msg);
}
