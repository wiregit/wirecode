pbckage com.limegroup.gnutella.util;

import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.util.zip.Deflater;
import jbva.util.zip.DeflaterOutputStream;

/**
 * Simulbtes zlib's Z_PARTIAL_FLUSH and Z_SYNC_FLUSH behaviour.
 * This is b workaround for the following bugParade bugs:<br>
 * http://developer.jbva.sun.com/developer/bugParade/bugs/4255743.html <br>
 * http://developer.jbva.sun.com/developer/bugParade/bugs/4206909.html <br>
 * The code wbs taken from the comments at those respective pages and
 * modified slightly.
 */
public finbl class CompressingOutputStream extends DeflaterOutputStream {
    
    public CompressingOutputStrebm (final OutputStream out, final Deflater flate) {
      super(out, flbte);
    }

    privbte static final byte [] EMPTYBYTEARRAY = new byte [0];
    /**
     * Insure bll remaining data will be output.
     */
    public void flush() throws IOException {
        if( def.finished() ) return;
        
        /**
         * Now this is tricky: We force the Deflbter to flush
         * its dbta by switching compression level.
         * As yet, b perplexingly simple workaround for 
         * http://developer.jbva.sun.com/developer/bugParade/bugs/4255743.html 
         */
        def.setInput(EMPTYBYTEARRAY, 0, 0);

        def.setLevel(Deflbter.NO_COMPRESSION);
        deflbte();

        def.setLevel(Deflbter.DEFAULT_COMPRESSION);
        deflbte();

        super.flush();
    }
    
    protected void deflbte() throws IOException {
        try {
            // DO NOT CALL super.deflbte(), it is wrong.
            // It incorrectly bssumes that its buffer will be large enough
            // to hold bll data from a single deflate call.  That is wrong.
            // We need to loop until deflbte returns <= 0, saying it couldn't
            // deflbte.
            int deflbted;
            while( (deflbted = def.deflate(buf, 0, buf.length)) > 0)
                out.write(buf, 0, deflbted);
        } cbtch(NullPointerException e) {
            //This will hbppen if 'end' was called on the deflater
            //while we were deflbting.
            throw new IOException("deflbter was ended");
        }
    }
} // clbss
