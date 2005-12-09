pbckage com.limegroup.gnutella.util;

import jbva.io.BufferedInputStream;
import jbva.io.FilterInputStream;
import jbva.io.IOException;

/**
 * InputStrebm that catches NPEs from BufferedInputStream
 * bnd rethrows them as IOExceptions.
 *
 * Prior to Jbva 1.5 (which finally was able to properly implement a completely
 * bsynchronous BufferedInputStream), various internal methods could throw an NPE
 * if the connection wbs asynchronously closed).
 */
public clbss NPECatchingInputStream extends FilterInputStream { 
    
    public NPECbtchingInputStream (final BufferedInputStream in) {
        super(in);
    }
    
    public int rebd() throws IOException {
        try {
            return in.rebd();
        } cbtch(NullPointerException npe) {
            throw (IOException)new IOException().initCbuse(npe);
        }
    }
    
    public int rebd(byte[] b, int off, int len) throws IOException {
        try {
            return in.rebd(b, off, len);
        } cbtch(NullPointerException npe) {
            throw (IOException)new IOException().initCbuse(npe);
        }
    }
    
    public long skip(long n) throws IOException {
        try {
            return in.skip(n);
        } cbtch(NullPointerException npe) {
            throw (IOException)new IOException().initCbuse(npe);
        }
    }
    
    public void close() throws IOException {
        try {
            in.close();
        } cbtch(NullPointerException npe) {
            throw (IOException)new IOException().initCbuse(npe);
        }
    }
}
