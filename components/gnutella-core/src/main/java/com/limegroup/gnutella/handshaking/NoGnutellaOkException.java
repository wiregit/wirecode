package com.limegroup.gnutella.handshaking;

import java.io.IOException;

/**
 * Exception thrown when the other side understands the Gnutella
 * Handshaking Protocol, but doesnt want to keep the connection up
 */
public class NoGnutellaOkException extends IOException
{
    
    public NoGnutellaOkException()
    {}
    
    public NoGnutellaOkException(String message)
    {
        super(message);
    }
    
}

