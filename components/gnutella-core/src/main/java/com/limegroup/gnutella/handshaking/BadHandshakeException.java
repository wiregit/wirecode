package com.limegroup.gnutella.handshaking;

import java.io.IOException;

public class BadHandshakeException extends IOException
{
    
    /** Root cause for BadHandshakeException  */
    private IOException _originalCause;
    
    public BadHandshakeException(IOException originalCause)
    {
        _originalCause = originalCause;
    }
    
    /**
     * prints its own stack trace, plus the stack trace for the
     * original exception that caused this exception
     */
    public void printStackTrace()
    {
        super.printStackTrace();
        System.err.println("Parent Cause:");
        _originalCause.printStackTrace();
    }
    
}

