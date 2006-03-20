package com.limegroup.gnutella.io;

public class InterruptedIOException extends java.io.InterruptedIOException {
    

    public InterruptedIOException(InterruptedException ix) {
        super();
        initCause(ix);
    }
    
}    