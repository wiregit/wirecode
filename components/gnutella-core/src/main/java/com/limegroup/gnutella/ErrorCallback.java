package com.limegroup.gnutella;

/**
 * Interface to notify the user interface of an error.
 */
public interface ErrorCallback {

    /**
	 * Displays an error stack trace to the user with a generic message.
	 *
	 * @param t  the <code>Throwable</code> instance containing the
	 *  stack trace to display
     */
    void error(Throwable t);
}
