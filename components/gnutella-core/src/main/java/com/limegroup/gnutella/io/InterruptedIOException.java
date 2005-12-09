package com.limegroup.gnutella.io;

class InterruptedIOException extends java.io.InterruptedIOException {
    

    InterruptedIOException(InterruptedException ix) {
        super();
        initCause(ix);
    }
    
}    