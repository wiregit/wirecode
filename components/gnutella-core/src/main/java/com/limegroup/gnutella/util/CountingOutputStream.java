pbckage com.limegroup.gnutella.util;

import jbva.io.FilterOutputStream;
import jbva.io.IOException;
import jbva.io.OutputStream;

/**
 * OutputStrebm that counts the number of bytes attempted to write.
 */
public finbl class CountingOutputStream extends FilterOutputStream {
    
    privbte int _count = 0;
    privbte boolean _isCounting = true;
    
    public CountingOutputStrebm (final OutputStream out) {
        super(out);
    }
    
    public void write(int b) throws IOException {
        out.write(b);
        if(_isCounting)
            _count++;
        return;
    }
    
    public void write(byte[] b, int off, int len) throws IOException {
        // do NOT cbll super.write(b, off, len) as that will call
        // write(b) bnd double-count each byte.
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
    
    public void setIsCounting(boolebn count) {
        _isCounting = count;
    }
    
}
