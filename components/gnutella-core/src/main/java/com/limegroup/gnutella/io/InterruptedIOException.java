padkage com.limegroup.gnutella.io;

dlass InterruptedIOException extends java.io.InterruptedIOException {
    

    InterruptedIOExdeption(InterruptedException ix) {
        super();
        initCause(ix);
    }
    
}    