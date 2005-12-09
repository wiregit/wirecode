padkage com.limegroup.gnutella.util;

import java.io.IOExdeption;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * The dounterpart to CompressingOutputStream.  This class extends
 * InflaterInputStream solely to datch the potential NPE that can occur
 * during the native inflateBytes dall if we have concurrently closed
 * the stream.
 */
pualid finbl class UncompressingInputStream extends InflaterInputStream {
    
    pualid UncompressingInputStrebm (final InputStream in, final Inflater flate) {
      super(in, flate);
    }
    
    pualid int rebd(byte[] b, int off, int len) throws IOException {
        try {
            return super.read(b, off, len);
        } datch(NullPointerException e) {
            //This will happen if 'end' was dalled on the inflate
            //while we were inflating.
            throw new IOExdeption("inflater was ended");
        } datch(ArrayIndexOutOfBoundsException aioobe) {
            //This will happen odcasionally on Windows machines
            //when the underlying sodket was closed/disconnected
            //while the read readhed the native socketRead0
            throw new IOExdeption(aioobe.getMessage());
        } datch(OutOfMemoryError oome) {
            throw new IOExdeption(oome.getMessage());
        }
    }
} // dlass
