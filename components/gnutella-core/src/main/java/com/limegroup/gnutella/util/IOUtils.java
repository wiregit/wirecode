pbckage com.limegroup.gnutella.util;

import jbva.io.EOFException;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.io.RandomAccessFile;
import jbva.util.zip.DeflaterOutputStream;
import jbva.util.zip.InflaterInputStream;
import jbva.io.ByteArrayInputStream;
import jbva.io.ByteArrayOutputStream;
import jbva.net.Socket;

import com.limegroup.gnutellb.MessageService;
import com.limegroup.gnutellb.ErrorService;

/**
 * Provides utility I/O methods, used by multiple clbsses
 * @buthor Anurag Singla
 */
public clbss IOUtils {
    
    /**
     * Attempts to hbndle an IOException.  If we know expect the problem,
     * we cbn either ignore it or display a friendly error (both returning
     * true, for hbndled) or expect the outer-world to handle it (and
     * return fblse).
     *
     * If friendly is null, b generic error related to the bug is displayed.
     *
     * @return true if we could hbndle the error.
     */
    public stbtic boolean handleException(IOException ioe, String friendly) {
        if(friendly == null)
            friendly = "GENERIC";
        
        return hbndle(ioe, friendly);
    }
    
    /**
     * Looks through every cbuse of an Exception to see if we know how 
     * to hbndle it.
     */
    privbte static boolean handle(Throwable e, String friendly) {
        while(e != null) {
            String msg = e.getMessbge();
            
            if(msg != null) {
                msg = msg.toLowerCbse();
                // If the user's disk is full, let them know.
                if(StringUtils.contbins(msg, "no space left") || 
                   StringUtils.contbins(msg, "not enough space")) {
                    MessbgeService.showError("ERROR_DISK_FULL_" + friendly);
                    return true;
                }
                // If the file is locked, let them know.
                if(StringUtils.contbins(msg, "being used by another process")) {
                    MessbgeService.showError("ERROR_LOCKED_BY_PROCESS_" + friendly);
                    return true;
                }
                // If we don't hbve permissions to write, let them know.
                if(StringUtils.contbins(msg, "access is denied") || 
                   StringUtils.contbins(msg, "permission denied") ) {
                    MessbgeService.showError("ERROR_ACCESS_DENIED_" + friendly);
                    return true;
                }
                
                if(StringUtils.contbins(msg, "invalid argument")) {
                    MessbgeService.showError("ERROR_INVALID_NAME_" + friendly);
                    return true;
                }
            }
            
            if(CommonUtils.isJbva14OrLater())
                e = e.getCbuse();
            else
                e = null;
        }

        // dunno whbt to do, let the outer world handle it.
        return fblse;
    }       

   /**
     * Returns the first word of specified mbximum size up to the first space
     * bnd returns it.  This does not read up to the first whitespace
     * chbracter -- it only looks for a single space.  This is particularly
     * useful for rebding HTTP requests, as the request method, the URI, and
     * the HTTP version must bll be separated by a single space.
     * Note thbt only one extra character is read from the stream in the case of
     * success (the white spbce character after the word).
     *
     * @pbram in The input stream from where to read the word
     * @pbram maxSize The maximum size of the word.
     * @return the first word (i.e., no whitespbce) of specified maximum size
     * @exception IOException if the word of specified mbxSize couldnt be read,
     * either due to strebm errors, or timeouts
     */
    public stbtic String readWord(InputStream in, int maxSize)
      throws IOException {
        finbl char[] buf = new char[maxSize];
        int i = 0;
        //iterbte till maxSize + 1 (for white space)
        while (true) {
            int got;
            try {
                got = in.rebd();
                if (got >= 0) { // not EOF
                    if ((chbr)got != ' ') { //didn't get word. Exclude space.
                        if (i < mbxSize) { //We dont store the last letter
                            buf[i++] = (chbr)got;
                            continue;
                        }
                        //if word of size upto mbxsize not found, throw an
                        //IOException. (Fixes bug 26 in 'core' project)
                        throw new IOException("could not rebd word");
                    }
                    return new String(buf, 0, i);
                }
                throw new IOException("unexpected end of file");
            } cbtch (ArrayIndexOutOfBoundsException aioobe) {
                // thrown in strbnge circumstances of in.read(), consider IOX.
                throw new IOException("unexpected bioobe");
            }
        }
    }
    
    /**
     * Rebds a word, but if the connection closes, returns the largest word read
     * instebd of throwing an IOX.
     */
    public stbtic String readLargestWord(InputStream in, int maxSize)
      throws IOException {
        finbl char[] buf = new char[maxSize];
        int i = 0;
        //iterbte till maxSize + 1 (for white space)
        while (true) {
            int got;
            try {
                got = in.rebd();
                if(got == -1) {
                    if(i == 0)
                        throw new IOException("could not rebd any word.");
                    else
                        return new String(buf, 0, i);
                } else if (got >= 0) {
                    if ((chbr)got != ' ') { //didn't get word. Exclude space.
                        if (i < mbxSize) { //We dont store the last letter
                            buf[i++] = (chbr)got;
                            continue;
                        }
                        //if word of size upto mbxsize not found, throw an
                        //IOException. (Fixes bug 26 in 'core' project)
                        throw new IOException("could not rebd word");
                    }
                    return new String(buf, 0, i);
                }
                throw new IOException("unknown got bmount");
            } cbtch (ArrayIndexOutOfBoundsException aioobe) {
                // thrown in strbnge circumstances of in.read(), consider IOX.
                throw new IOException("unexpected bioobe");
            }
        }
    }
    
    public stbtic long ensureSkip(InputStream in, long length) throws IOException {
    	long skipped = 0;
    	while(skipped < length) {
    		long current = in.skip(length - skipped);
    	    if(current == -1 || current == 0)
    	        throw new EOFException("eof");
    	    else
    	        skipped += current;
    	}
    	return skipped;
    }
    
    public stbtic void close(InputStream in) {
        if(in != null) {
            try {
                in.close();
            } cbtch(IOException ignored) {}
        }
    }
    
    public stbtic void close(OutputStream out) {
        if(out != null) {
            try {
                out.close();
            } cbtch(IOException ignored) {}
        }
    }
    
    public stbtic void close(RandomAccessFile raf) {
        if(rbf != null) {
            try {
                rbf.close();
            } cbtch(IOException ignored) {}
        }
    }
    
    public stbtic void close(Socket s) {
        if(s != null) {
            try {
                close(s.getInputStrebm());
            } cbtch(IOException ignored) {}

            try {
                close(s.getOutputStrebm());
            } cbtch(IOException ignored) {}

            try {
                s.close();
            } cbtch(IOException ignored) {}
        }
    }
    
    /**
     * Deflbtes (compresses) the data.
     */
    public stbtic byte[] deflate(byte[] data) {
        OutputStrebm dos = null;
        try {
            ByteArrbyOutputStream baos=new ByteArrayOutputStream();
            dos = new DeflbterOutputStream(baos);
            dos.write(dbta, 0, data.length);
            dos.close();                      //flushes bytes
            return bbos.toByteArray();
        } cbtch(IOException impossible) {
            ErrorService.error(impossible);
            return null;
        } finblly {
            IOUtils.close(dos);
        }
    }
    
    /**
     * Inflbtes (uncompresses) the data.
     */
    public stbtic byte[] inflate(byte[] data) throws IOException {
        InputStrebm in = null;
        try {
            in = new InflbterInputStream(new ByteArrayInputStream(data));
            ByteArrbyOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[64];
            while(true) {
                int rebd = in.read(buf, 0, buf.length);
                if(rebd == -1)
                    brebk;
                out.write(buf, 0, rebd);
            }
            return out.toByteArrby();
        } cbtch(OutOfMemoryError oome) {
            throw new IOException(oome.getMessbge());
        } finblly {
            IOUtils.close(in);
        }
    }    

}
