package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.OutputStream;

/** Forks output to console as well as delegate stream. */
public class DebugOutputStream extends OutputStream {
    private OutputStream _delegate;
    public DebugOutputStream(OutputStream delegate) {
        this._delegate=delegate;
    }
    public void write(int b) throws IOException { 
        System.out.print((char)b);
        _delegate.write(b);
    }

    public void flush() throws IOException {
        _delegate.flush();
    }

    public void close() throws IOException {
        _delegate.close();
    }
}
