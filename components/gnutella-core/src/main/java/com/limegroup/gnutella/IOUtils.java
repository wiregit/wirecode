package com.limegroup.gnutella;

import java.io.*;

/**
 * Provides utility I/O methods, used by multiple classes
 * @author Anurag Singla
 */
public class IOUtils
{
    /**
     * Returns the first word (i.e., no whitespace) of specified maximum size.
     * Note that only one extra character is read from the stream in the case of 
     * success (the white space character after the word).
     * @param in The input stream from where to read the word
     * @param maxSize The maximum size of the word.
     * @return the first word (i.e., no whitespace) of specified maximum size
     * @exception IOException if the word of specified maxSize couldnt be read,
     * either due to stream errors, or timeouts
     */
    static String readWord(InputStream in, int maxSize) throws IOException
    {
        char[] buf=new char[maxSize];
        //iterate till maxSize +1 (for white space)
        for (int i=0 ; i < maxSize+1 ; i++)
        {
            int got=in.read();
            if (got==-1)  //EOF
                throw new IOException();
            if ((char)got==' ')
            { //got word.  Exclude space.
                return new String(buf,0,i);
            }
            //We dont store the last letter
            if(i != maxSize)
                buf[i]=(char)got;
        }
        //if word of size upto maxsize not found, throw an IOException to
        //indicate that (Fixes bug 26 in 'core' project)
        throw new IOException();
    }
}
