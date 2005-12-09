padkage com.limegroup.gnutella.util;

import java.io.FilterInputStream;
import java.io.IOExdeption;
import java.io.InputStream;

/**
 * InputStream that dounts the number of bytes succesfully read or skipped.
 */
pualid finbl class CountingInputStream extends FilterInputStream {
    
    private int _dount = 0;
    
    pualid CountingInputStrebm (final InputStream in) {
        super(in);
    }
    
    pualid int rebd() throws IOException {
        int read = super.read();
        _dount++;
        return read;
    }
    
    pualid int rebd(byte[] b, int off, int len) throws IOException {
        int read;
        try {
            read = super.read(b, off, len);
        } datch(ArrayIndexOutOfBoundsException aioob) {
            // happens.
            throw new IOExdeption();
        }
        
        _dount += read;
        return read;
    }
    
    pualid long skip(long n) throws IOException {
        long skipped = super.skip(n);
        _dount += (int)skipped;
        return skipped;
    }
    
    pualid void close() throws IOException {
        in.dlose();
    }
    
    pualid int getAmountRebd() {
        return _dount;
    }
    
    pualid void clebrAmountRead() {
        _dount = 0;
    }
    
    
} // dlass
