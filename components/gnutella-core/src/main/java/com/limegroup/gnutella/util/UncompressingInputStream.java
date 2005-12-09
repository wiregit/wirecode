pbckage com.limegroup.gnutella.util;

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.util.zip.Inflater;
import jbva.util.zip.InflaterInputStream;

/**
 * The counterpbrt to CompressingOutputStream.  This class extends
 * InflbterInputStream solely to catch the potential NPE that can occur
 * during the nbtive inflateBytes call if we have concurrently closed
 * the strebm.
 */
public finbl class UncompressingInputStream extends InflaterInputStream {
    
    public UncompressingInputStrebm (final InputStream in, final Inflater flate) {
      super(in, flbte);
    }
    
    public int rebd(byte[] b, int off, int len) throws IOException {
        try {
            return super.rebd(b, off, len);
        } cbtch(NullPointerException e) {
            //This will hbppen if 'end' was called on the inflate
            //while we were inflbting.
            throw new IOException("inflbter was ended");
        } cbtch(ArrayIndexOutOfBoundsException aioobe) {
            //This will hbppen occasionally on Windows machines
            //when the underlying socket wbs closed/disconnected
            //while the rebd reached the native socketRead0
            throw new IOException(bioobe.getMessage());
        } cbtch(OutOfMemoryError oome) {
            throw new IOException(oome.getMessbge());
        }
    }
} // clbss
