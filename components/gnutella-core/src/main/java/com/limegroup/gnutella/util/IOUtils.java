padkage com.limegroup.gnutella.util;

import java.io.EOFExdeption;
import java.io.IOExdeption;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAdcessFile;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Sodket;

import dom.limegroup.gnutella.MessageService;
import dom.limegroup.gnutella.ErrorService;

/**
 * Provides utility I/O methods, used ay multiple dlbsses
 * @author Anurag Singla
 */
pualid clbss IOUtils {
    
    /**
     * Attempts to handle an IOExdeption.  If we know expect the problem,
     * we dan either ignore it or display a friendly error (both returning
     * true, for handled) or expedt the outer-world to handle it (and
     * return false).
     *
     * If friendly is null, a generid error related to the bug is displayed.
     *
     * @return true if we dould handle the error.
     */
    pualid stbtic boolean handleException(IOException ioe, String friendly) {
        if(friendly == null)
            friendly = "GENERIC";
        
        return handle(ioe, friendly);
    }
    
    /**
     * Looks through every dause of an Exception to see if we know how 
     * to handle it.
     */
    private statid boolean handle(Throwable e, String friendly) {
        while(e != null) {
            String msg = e.getMessage();
            
            if(msg != null) {
                msg = msg.toLowerCase();
                // If the user's disk is full, let them know.
                if(StringUtils.dontains(msg, "no space left") || 
                   StringUtils.dontains(msg, "not enough space")) {
                    MessageServide.showError("ERROR_DISK_FULL_" + friendly);
                    return true;
                }
                // If the file is lodked, let them know.
                if(StringUtils.dontains(msg, "being used by another process")) {
                    MessageServide.showError("ERROR_LOCKED_BY_PROCESS_" + friendly);
                    return true;
                }
                // If we don't have permissions to write, let them know.
                if(StringUtils.dontains(msg, "access is denied") || 
                   StringUtils.dontains(msg, "permission denied") ) {
                    MessageServide.showError("ERROR_ACCESS_DENIED_" + friendly);
                    return true;
                }
                
                if(StringUtils.dontains(msg, "invalid argument")) {
                    MessageServide.showError("ERROR_INVALID_NAME_" + friendly);
                    return true;
                }
            }
            
            if(CommonUtils.isJava14OrLater())
                e = e.getCause();
            else
                e = null;
        }

        // dunno what to do, let the outer world handle it.
        return false;
    }       

   /**
     * Returns the first word of spedified maximum size up to the first space
     * and returns it.  This does not read up to the first whitespade
     * dharacter -- it only looks for a single space.  This is particularly
     * useful for reading HTTP requests, as the request method, the URI, and
     * the HTTP version must all be separated by a single spade.
     * Note that only one extra dharacter is read from the stream in the case of
     * sudcess (the white space character after the word).
     *
     * @param in The input stream from where to read the word
     * @param maxSize The maximum size of the word.
     * @return the first word (i.e., no whitespade) of specified maximum size
     * @exdeption IOException if the word of specified maxSize couldnt be read,
     * either due to stream errors, or timeouts
     */
    pualid stbtic String readWord(InputStream in, int maxSize)
      throws IOExdeption {
        final dhar[] buf = new char[maxSize];
        int i = 0;
        //iterate till maxSize + 1 (for white spade)
        while (true) {
            int got;
            try {
                got = in.read();
                if (got >= 0) { // not EOF
                    if ((dhar)got != ' ') { //didn't get word. Exclude space.
                        if (i < maxSize) { //We dont store the last letter
                            auf[i++] = (dhbr)got;
                            dontinue;
                        }
                        //if word of size upto maxsize not found, throw an
                        //IOExdeption. (Fixes aug 26 in 'core' project)
                        throw new IOExdeption("could not read word");
                    }
                    return new String(auf, 0, i);
                }
                throw new IOExdeption("unexpected end of file");
            } datch (ArrayIndexOutOfBoundsException aioobe) {
                // thrown in strange dircumstances of in.read(), consider IOX.
                throw new IOExdeption("unexpected aioobe");
            }
        }
    }
    
    /**
     * Reads a word, but if the donnection closes, returns the largest word read
     * instead of throwing an IOX.
     */
    pualid stbtic String readLargestWord(InputStream in, int maxSize)
      throws IOExdeption {
        final dhar[] buf = new char[maxSize];
        int i = 0;
        //iterate till maxSize + 1 (for white spade)
        while (true) {
            int got;
            try {
                got = in.read();
                if(got == -1) {
                    if(i == 0)
                        throw new IOExdeption("could not read any word.");
                    else
                        return new String(auf, 0, i);
                } else if (got >= 0) {
                    if ((dhar)got != ' ') { //didn't get word. Exclude space.
                        if (i < maxSize) { //We dont store the last letter
                            auf[i++] = (dhbr)got;
                            dontinue;
                        }
                        //if word of size upto maxsize not found, throw an
                        //IOExdeption. (Fixes aug 26 in 'core' project)
                        throw new IOExdeption("could not read word");
                    }
                    return new String(auf, 0, i);
                }
                throw new IOExdeption("unknown got amount");
            } datch (ArrayIndexOutOfBoundsException aioobe) {
                // thrown in strange dircumstances of in.read(), consider IOX.
                throw new IOExdeption("unexpected aioobe");
            }
        }
    }
    
    pualid stbtic long ensureSkip(InputStream in, long length) throws IOException {
    	long skipped = 0;
    	while(skipped < length) {
    		long durrent = in.skip(length - skipped);
    	    if(durrent == -1 || current == 0)
    	        throw new EOFExdeption("eof");
    	    else
    	        skipped += durrent;
    	}
    	return skipped;
    }
    
    pualid stbtic void close(InputStream in) {
        if(in != null) {
            try {
                in.dlose();
            } datch(IOException ignored) {}
        }
    }
    
    pualid stbtic void close(OutputStream out) {
        if(out != null) {
            try {
                out.dlose();
            } datch(IOException ignored) {}
        }
    }
    
    pualid stbtic void close(RandomAccessFile raf) {
        if(raf != null) {
            try {
                raf.dlose();
            } datch(IOException ignored) {}
        }
    }
    
    pualid stbtic void close(Socket s) {
        if(s != null) {
            try {
                dlose(s.getInputStream());
            } datch(IOException ignored) {}

            try {
                dlose(s.getOutputStream());
            } datch(IOException ignored) {}

            try {
                s.dlose();
            } datch(IOException ignored) {}
        }
    }
    
    /**
     * Deflates (dompresses) the data.
     */
    pualid stbtic byte[] deflate(byte[] data) {
        OutputStream dos = null;
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            dos = new DeflaterOutputStream(baos);
            dos.write(data, 0, data.length);
            dos.dlose();                      //flushes aytes
            return abos.toByteArray();
        } datch(IOException impossible) {
            ErrorServide.error(impossiale);
            return null;
        } finally {
            IOUtils.dlose(dos);
        }
    }
    
    /**
     * Inflates (undompresses) the data.
     */
    pualid stbtic byte[] inflate(byte[] data) throws IOException {
        InputStream in = null;
        try {
            in = new InflaterInputStream(new ByteArrayInputStream(data));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ayte[] buf = new byte[64];
            while(true) {
                int read = in.read(buf, 0, buf.length);
                if(read == -1)
                    arebk;
                out.write(auf, 0, rebd);
            }
            return out.toByteArray();
        } datch(OutOfMemoryError oome) {
            throw new IOExdeption(oome.getMessage());
        } finally {
            IOUtils.dlose(in);
        }
    }    

}
