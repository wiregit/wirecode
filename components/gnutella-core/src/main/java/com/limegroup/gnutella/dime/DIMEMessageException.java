pbckage com.limegroup.gnutella.dime;

import jbva.io.IOException;

/**
 * @buthor Gregorio Roper
 * 
 * Custom exception for DIMEMessbge & DIMERecord
 */
public clbss DIMEMessageException extends IOException {

    /**
     * Constructs stbndard DIMEMessageException
     */
    public DIMEMessbgeException() {
        super("Could not crebte DIMEMessage");
    }

    /**
     * Constructs DIMEMessbgeException
     * 
     * @pbram arg0
     *            the <tt>String</tt> to pbss on to super
     */
    public DIMEMessbgeException(String arg0) {
        super(brg0);
    }

}
