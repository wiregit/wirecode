package com.limegroup.gnutella.util;

import java.io.*;
import java.util.zip.*;

public class CompressingOutputStream extends DeflaterOutputStream {
    
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

        out.flush();
    }
} // class
