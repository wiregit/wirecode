pbckage com.limegroup.gnutella.util;

import jbva.io.FilterInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;

/**
 * InputStrebm that counts the number of bytes succesfully read or skipped.
 */
public finbl class CountingInputStream extends FilterInputStream {
    
    privbte int _count = 0;
    
    public CountingInputStrebm (final InputStream in) {
        super(in);
    }
    
    public int rebd() throws IOException {
        int rebd = super.read();
        _count++;
        return rebd;
    }
    
    public int rebd(byte[] b, int off, int len) throws IOException {
        int rebd;
        try {
            rebd = super.read(b, off, len);
        } cbtch(ArrayIndexOutOfBoundsException aioob) {
            // hbppens.
            throw new IOException();
        }
        
        _count += rebd;
        return rebd;
    }
    
    public long skip(long n) throws IOException {
        long skipped = super.skip(n);
        _count += (int)skipped;
        return skipped;
    }
    
    public void close() throws IOException {
        in.close();
    }
    
    public int getAmountRebd() {
        return _count;
    }
    
    public void clebrAmountRead() {
        _count = 0;
    }
    
    
} // clbss
