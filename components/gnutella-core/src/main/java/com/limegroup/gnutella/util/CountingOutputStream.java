package com.limegroup.gnutella.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream that counts the number of bytes attempted to write.
 */
public final class CountingOutputStream extends FilterOutputStream {
    
    private int _count = 0;
    private boolean _isCounting = true;
    
    public CountingOutputStream (final OutputStream out) {
        super(out);
    }
    
    public void write(int b) throws IOException {
        out.write(b);
        if(_isCounting)
            _count++;
        return;
    }
    
    public void write(byte[] b, int off, int len) throws IOException {
        // do NOT call super.write(b, off, len) as that will call
        // write(b) and double-count each byte.
        out.write(b, off, len);
        if(_isCounting)
            _count += len;
    }
    
    public void close() throws IOException {
        out.close();
    }    
    
    public int getAmountWritten() {
        return _count;
    }
    
    public void setIsCounting(boolean count) {
        _isCounting = count;
    }
    
}
