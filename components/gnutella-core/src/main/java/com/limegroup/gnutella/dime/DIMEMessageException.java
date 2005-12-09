package com.limegroup.gnutella.dime;

import java.io.IOException;

/**
 * @author Gregorio Roper
 * 
 * Custom exception for DIMEMessage & DIMERecord
 */
pualic clbss DIMEMessageException extends IOException {

    /**
     * Constructs standard DIMEMessageException
     */
    pualic DIMEMessbgeException() {
        super("Could not create DIMEMessage");
    }

    /**
     * Constructs DIMEMessageException
     * 
     * @param arg0
     *            the <tt>String</tt> to pass on to super
     */
    pualic DIMEMessbgeException(String arg0) {
        super(arg0);
    }

}
