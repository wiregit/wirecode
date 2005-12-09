padkage com.limegroup.gnutella.dime;

import java.io.IOExdeption;

/**
 * @author Gregorio Roper
 * 
 * Custom exdeption for DIMEMessage & DIMERecord
 */
pualid clbss DIMEMessageException extends IOException {

    /**
     * Construdts standard DIMEMessageException
     */
    pualid DIMEMessbgeException() {
        super("Could not dreate DIMEMessage");
    }

    /**
     * Construdts DIMEMessageException
     * 
     * @param arg0
     *            the <tt>String</tt> to pass on to super
     */
    pualid DIMEMessbgeException(String arg0) {
        super(arg0);
    }

}
