package com.limegroup.gnutella.chat;
/**
 * Similar to the controversial ByteReader class, this
 * class also handle reading off of an input stream, but
 * reads up until "\r"'s as well as "\n"'s.
 * Provides the readLine method of a BufferedReader with 
 * no automatic buffering. 
 * 
 *@author rsoule
 */

import java.io.*;

public class ChatLineReader {
	
	// Attributes 
	private int BUFSIZE = 80;
	private InputStream _istream;
	byte[] _creturn;
	byte[] _nline;

	public ChatLineReader(InputStream stream) {
		_istream = stream;

		String cr = "\r";
        _creturn = cr.getBytes();

        String nl = "\n";
        _nline = nl.getBytes();
	} 

	/** closes the InputStream that the reader
		is reading from */
	public void close() {
		try {
			_istream.close();
		} catch(IOException e) {
			// don't do anything
		}
	}

	/** 
     * Reads a new line.  A line is  defined as a minimal 
	 * sequence of characters ending with either "\r" or "\n",
	 *
     * Throws IOException if there is an IO error.  Returns null if
     * there are no more lines to read, i.e., EOF has been reached.
     * Note that calling readLine on "ab<EOF>" returns null.
     */
	public String readLine() throws IOException {
		
		String finalString = "";

		if (_istream == null)
			throw new IOException();;
		
		byte[] buf = new byte[BUFSIZE];
		
        int c = -1; //the character just read
        int i = 0;  
        int numBytes = 0;

        while (true) {

            c = _istream.read();

            if (c == -1) 
				throw new IOException();
			// break;
                // return null;
        
            if( ( c == _creturn[0] ) || 
				( c == _nline[0] ) ){
                break;
            }
                
            else {
                buf[i++] = (byte)c;
                numBytes++;
            }

            if (numBytes == BUFSIZE) {
                finalString += new String(buf, 0, numBytes);
                i = 0;
                numBytes = 0;
            }

        }

        finalString += new String(buf, 0, numBytes);

        return finalString;
	}
	

}
