package com.limegroup.gnutella.util;

import java.io.*;

/**
 * Provides utility I/O methods, used by multiple classes
 * @author Anurag Singla
 */
public class IOUtils {

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
        char[] buf=new char[maxSize];
        //iterate till maxSize +1 (for white space)
        for (int i=0 ; i < maxSize+1 ; i++) {
            int got=in.read();
            if (got==-1) {//EOF
                throw new IOException("unexpected end of file");
			} else if ((char)got==' ') { //got word.  Exclude space.
				return new String(buf,0,i);
            } else if(i != maxSize) { //We dont store the last letter
                buf[i]=(char)got;
			}
        }
        //if word of size upto maxsize not found, throw an IOException to
        //indicate that (Fixes bug 26 in 'core' project)
        throw new IOException("could not read word");
    }
}
