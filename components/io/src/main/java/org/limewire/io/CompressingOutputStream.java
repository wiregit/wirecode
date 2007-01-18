package org.limewire.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Simulates zlib's Z_PARTIAL_FLUSH and Z_SYNC_FLUSH behaviour.
 * This is a workaround for the following bugParade bugs:<br>
 * http://developer.java.sun.com/developer/bugParade/bugs/4255743.html <br>
 * http://developer.java.sun.com/developer/bugParade/bugs/4206909.html <br>
 * The code was taken from the comments at those respective pages and
 * modified slightly.
 */
public final class CompressingOutputStream extends DeflaterOutputStream {
    
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
            // DO NOT CALL super.deflate(), it is wrong.
            // It incorrectly assumes that its buffer will be large enough
            // to hold all data from a single deflate call.  That is wrong.
            // We need to loop until deflate returns <= 0, saying it couldn't
            // deflate.
            int deflated;
            while( (deflated = def.deflate(buf, 0, buf.length)) > 0)
                out.write(buf, 0, deflated);
        } catch(NullPointerException e) {
            //This will happen if 'end' was called on the deflater
            //while we were deflating.
            throw new IOException("deflater was ended");
        }
    }
} // class
