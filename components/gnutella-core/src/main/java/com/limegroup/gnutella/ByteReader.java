/*
 * handles reading off of the input stream
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;

/** 
 * Provides the readLine method of a BufferedReader with no no automatic
 * buffering.  All methods are like those in InputStream except they return
 * -1 instead of throwing IOException.
 */
public class ByteReader {

    private int BUFSIZE = 80;

    private InputStream _istream;
    
    public ByteReader(InputStream stream) {
        _istream = stream;
    }

    public void close() {
        try {
            _istream.close();
        }
        catch (IOException e) {
        
        }
    }

    public int read() {

        int c = -1;
    
        if (_istream == null)
            return c;
    
        try {
            c =  _istream.read();
        }
        catch(IOException e) {

        }
        return c;
    }

    public int read(byte[] buf) {
        int c = -1;

        if (_istream == null) {
            return c;
        }

        try {
            c = _istream.read(buf);
        }
        catch(IOException e) {

        }
        return c;
    }

    public int read(byte[] buf, int offset, int length) {
        int c = -1;

        if (_istream == null) {
            return c;
        }

        try {
            c = _istream.read(buf, offset, length);
        }
        catch(IOException e) {

        }
        return c;
    }

    /** 
     * Reads a new line WITHOUT end of line characters.  A line is 
     * defined as a minimal sequence of character ending with "\n", with
     * all "\r"'s thrown away.  Hence calling readLine on a stream
     * containing "abc\r\n" or "a\rbc\n" will return "abc".
     *
     * Throws IOException if there is an IO error.  Returns null if
     * there are no more lines to read, i.e., EOF has been reached.
     * Note that calling readLine on "ab<EOF>" returns null.
     */
    public String readLine() throws IOException {
        //TODO: this method desperately need cleanup:
        //1. Internationalization problems.  It assumes that getBytes
        //   does the right thing.  (We should hardcode the ASCII values.)
        //   Also, we probably shouldn't use the String(byte[],int,int)
        //   constructor.
        //2. There's no need to use buf and sBuffer.  It just makes the
        //   code confusing.  Just append characters to sBuffer.
        //3. The i and numBytes variables are redundant.  As far as I can
        //   tell, they're exactly the same.  Actually if you use the
        //   above suggestion, you can just call sBuffer.append(..); no
        //   need for any variables.
        if (_istream == null)
            return "";

		StringBuffer sBuffer = new StringBuffer();

        byte[] buf = new byte[BUFSIZE];

        int c = -1; //the character just read
        int i = 0;  
        int numBytes = 0;

        String cr = "\r";
        byte[] creturn = cr.getBytes();

        String nl = "\n";
        byte[] nline = nl.getBytes();

		try {
			while (true) {
				
				c = _istream.read();
				
				if (c == -1) 
					return null;
				
				if ( c == creturn[0] ) {
					continue;
				}
				
				else if ( c == nline[0] ) { 
					break;
				} 
                
				else {
					buf[i++] = (byte)c;
					numBytes++;
					
				}

				if (numBytes == BUFSIZE) {
					sBuffer.append(new String(buf, 0, numBytes));
					i = 0;
					numBytes = 0;
				}
				
			}
		} catch(ArrayIndexOutOfBoundsException e) {
			sBuffer.append(new String(buf, 0, numBytes));
			throw new ArrayIndexOutOfBoundsException(e.getMessage() + 
													 " in ByteReader:\r\n"+
													 sBuffer.toString());													 
		}

		sBuffer.append(new String(buf, 0, numBytes));
		return sBuffer.toString();
    }
}
