package com.limegroup.gnutella.util;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * OutputStream that counts the number of bytes attempted to write.
 */
public final class CountingOutputStream extends FilterOutputStream {
    
    private int _count = 0;
    private boolean _isCounting = true;
    
    public CountingOutputStream (final OutputStream in) {
        super(in);
    }
    
    public void write(int b) throws IOException {
        super.write(b);
        if(_isCounting)
            _count++;
        return;
    }
    
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        if(_isCounting)
            _count += len - off;
    }
    
    public int getAmountWritten() {
        return _count;
    }
    
    public void setIsCounting(boolean count) {
        _isCounting = count;
    }
    
}
