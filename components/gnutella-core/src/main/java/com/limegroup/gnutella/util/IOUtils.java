package com.limegroup.gnutella.util;

import java.io.*;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.MessageService;

/**
 * Provides utility I/O methods, used by multiple classes
 * @author Anurag Singla
 */
public class IOUtils {
    
    /**
     * Attempts to handle an IOException.  If we know expect the problem,
     * we can either ignore it or display a friendly error (both returning
     * true, for handled) or expect the outer-world to handle it (and
     * return false).
     *
     * If friendly is null, a generic error related to the bug is displayed.
     *
     * @return true if we could handle the error.
     */
    public static boolean handleException(IOException ioe, String friendly) {
        String msg = ioe.getMessage();
        if(msg == null)
            return false;
            
        if(friendly == null)
            friendly = "GENERIC";
        
        msg = msg.toLowerCase();
        // If the user's disk is full, let them know.
        if(StringUtils.contains(msg, "no space left") || 
           StringUtils.contains(msg, "not enough space")) {
            MessageService.showError("ERROR_DISK_FULL_" + friendly);
            return true;
        }
        // If the file is locked, let them know.
        if(StringUtils.contains(msg, "being used by another process")) {
            MessageService.showError("ERROR_LOCKED_BY_PROCESS_" + friendly);
            return true;
        }
        // If we don't have permissions to write, let them know.
        if(StringUtils.contains(msg, "access is denied")) {
            MessageService.showError("ERROR_ACCESS_DENIED_" + friendly);
            return true;
        }
        
        // dunno what to do, let the outer world handle it.
        return false;
    }       

   /**
     * Returns the first word of specified maximum size up to the first space
     * and returns it.  This does not read up to the first whitespace
     * character -- it only looks for a single space.  This is particularly
     * useful for reading HTTP requests, as the request method, the URI, and
     * the HTTP version must all be separated by a single space.
     * Note that only one extra character is read from the stream in the case of
     * success (the white space character after the word).
     *
     * @param in The input stream from where to read the word
     * @param maxSize The maximum size of the word.
     * @return the first word (i.e., no whitespace) of specified maximum size
     * @exception IOException if the word of specified maxSize couldnt be read,
     * either due to stream errors, or timeouts
     */
    public static String readWord(InputStream in, int maxSize)
      throws IOException {
        final char[] buf = new char[maxSize];
        int i = 0;
        //iterate till maxSize + 1 (for white space)
        while (true) {
            int got;
            try {
                got = in.read();
                if (got >= 0) { // not EOF
                    if ((char)got != ' ') { //didn't get word. Exclude space.
                        if (i < maxSize) { //We dont store the last letter
                            buf[i++] = (char)got;
                            continue;
                        }
                        //if word of size upto maxsize not found, throw an
                        //IOException. (Fixes bug 26 in 'core' project)
                        throw new IOException("could not read word");
                    }
                    return new String(buf, 0, i);
                }
                throw new IOException("unexpected end of file");
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                // thrown in strange circumstances of in.read(), consider IOX.
                throw new IOException("unexpected aioobe");
            }
        }
    }
    
    public static long ensureSkip(InputStream in, long length) throws IOException {
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

}
