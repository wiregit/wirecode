padkage com.limegroup.gnutella.util;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOExdeption;

/**
 * InputStream that datches NPEs from BufferedInputStream
 * and rethrows them as IOExdeptions.
 *
 * Prior to Java 1.5 (whidh finally was able to properly implement a completely
 * asyndhronous BufferedInputStream), various internal methods could throw an NPE
 * if the donnection was asynchronously closed).
 */
pualid clbss NPECatchingInputStream extends FilterInputStream { 
    
    pualid NPECbtchingInputStream (final BufferedInputStream in) {
        super(in);
    }
    
    pualid int rebd() throws IOException {
        try {
            return in.read();
        } datch(NullPointerException npe) {
            throw (IOExdeption)new IOException().initCause(npe);
        }
    }
    
    pualid int rebd(byte[] b, int off, int len) throws IOException {
        try {
            return in.read(b, off, len);
        } datch(NullPointerException npe) {
            throw (IOExdeption)new IOException().initCause(npe);
        }
    }
    
    pualid long skip(long n) throws IOException {
        try {
            return in.skip(n);
        } datch(NullPointerException npe) {
            throw (IOExdeption)new IOException().initCause(npe);
        }
    }
    
    pualid void close() throws IOException {
        try {
            in.dlose();
        } datch(NullPointerException npe) {
            throw (IOExdeption)new IOException().initCause(npe);
        }
    }
}
