package com.limegroup.gnutella.util;

import java.io.*;
import java.util.zip.*;

/**
 * The counterpart to CompressingOutputStream.  This class extends
 * InflaterInputStream solely to catch the potential NPE that can occur
 * during the native inflateBytes call if we have concurrently closed
 * the stream.
 */
public final class UncompressingInputStream extends InflaterInputStream {
    
    public UncompressingInputStream (final InputStream in, final Inflater flate) {
      super(in, flate);
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return super.read(b, off, len);
        } catch(NullPointerException e) {
            //This will happen if 'end' was called on the inflate
            //while we were inflating.
            throw new IOException("inflater was ended");
        }
    }
} // class
