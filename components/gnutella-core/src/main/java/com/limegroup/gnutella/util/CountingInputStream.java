package com.limegroup.gnutella.util;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;

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
        int read = super.read(b, off, len);
        _count += read;
        return read;
    }
    
    public void close() throws IOException {
        in.close();
    }
    
    public int getAmountRead() {
        return _count;
    }
    
    public long skip(long n) throws IOException {
    	long skipped = in.skip(n);
    	_count+=skipped;
    	return skipped;
    }
    
} // class
