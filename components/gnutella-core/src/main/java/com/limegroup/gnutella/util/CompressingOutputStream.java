package com.limegroup.gnutella.util;

import java.io.*;
import java.util.zip.*;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Simulates zlib's Z_PARTIAL_FLUSH and Z_SYNC_FLUSH behaviour.
 * This is a workaround for the following bugParade bugs:<br>
 * http://developer.java.sun.com/developer/bugParade/bugs/4255743.html <br>
 * http://developer.java.sun.com/developer/bugParade/bugs/4206909.html <br>
 * The code was taken from the comments at those respective pages and
 * modified slightly.
 */
public final class CompressingOutputStream extends DeflaterOutputStream {
    
    private static final Log LOG = LogFactory.getLog(CompressingOutputStream.class);
    
    public CompressingOutputStream (final OutputStream out, final Deflater flate) {
      super(out, flate);
    }

    private static final byte [] EMPTYBYTEARRAY = new byte [0];
    /**
     * Insure all remaining data will be output.
     */
    public void flush() throws IOException {
        if( def.finished() ) return;
        
        /**
         * Now this is tricky: We force the Deflater to flush
         * its data by switching compression level.
         * As yet, a perplexingly simple workaround for 
         * http://developer.java.sun.com/developer/bugParade/bugs/4255743.html 
         */
        def.setInput(EMPTYBYTEARRAY, 0, 0);

        def.setLevel(Deflater.NO_COMPRESSION);
        deflate();

        def.setLevel(Deflater.DEFAULT_COMPRESSION);
        deflate();

        super.flush();
    }
    
    protected void deflate() throws IOException {
        try {
            long now1 = System.currentTimeMillis();
            super.deflate();
            long now2 = System.currentTimeMillis();
            if( now2 - now1 != 0 )
                LOG.debug("deflate took: " + (now2 - now1) + " millseconds");
        } catch(NullPointerException e) {
            //This will happen if 'end' was called on the deflater
            //while we were deflating.
            throw new IOException("deflater was ended");
        }
    }
} // class
