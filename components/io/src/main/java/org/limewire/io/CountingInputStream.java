package org.limewire.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream that counts the number of bytes succesfully read or skipped.
 */
public final class CountingInputStream extends FilterInputStream {
    
    private int _count = 0;
    
    public CountingInputStream (final InputStream in) {
        super(in);
    }
    
    public int read() throws IOException {
        int read = super.read();
        _count++;
        return read;
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
        int read;
        try {
            read = super.read(b, off, len);
        } catch(ArrayIndexOutOfBoundsException aioob) {
            // happens.
            throw new IOException();
        }
        
        _count += read;
        return read;
    }
    
    public long skip(long n) throws IOException {
        long skipped = super.skip(n);
        _count += (int)skipped;
        return skipped;
    }
    
    public void close() throws IOException {
        in.close();
    }
    
    public int getAmountRead() {
        return _count;
    }
    
    public void clearAmountRead() {
        _count = 0;
    }
    
    
} // class
