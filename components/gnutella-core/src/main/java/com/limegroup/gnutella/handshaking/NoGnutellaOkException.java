package com.limegroup.gnutella.handshaking;

import java.io.IOException;

/**
 * Exception thrown when someone understands responds with a handshaking
 * code other than 200 or 401.
 */
public class NoGnutellaOkException extends IOException
{
    private boolean wasMe;
    private int code;

    /**
     * @param wasMe true if I returned the non-standard code.
     *  False if the remote host did.
     * @param code non-standard code
     * @param message a human-readable message for debugging purposes
     *  NOT necessarily the message given during the interaction.
     */
    public NoGnutellaOkException(boolean wasMe, 
                                 int code,
                                 String message)
    {
        super(message);
        this.wasMe=wasMe;
        this.code=code;
    }
    
    /** 
     * Returns true if the exception was caused by something this host
     * wrote. 
     */
    public boolean wasMe() {
        return wasMe;
    }

    /**
     * The offending status code.
     */
    public int getCode() {
        return code;
    }

}

