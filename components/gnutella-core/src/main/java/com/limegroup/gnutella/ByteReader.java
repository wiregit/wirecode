/*
 * auth: rsoule
 * file: ByteReader.java
 * desc: handles reading off of the input stream
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;

/** 
 * Provides the readLine method of a BufferedReader with no 
 * no automatic buffering.
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

		sBuffer.append(new String(buf, 0, numBytes));
		return sBuffer.toString();
    }
}

//    	public static void main(String args[]) {
//    		try {
//    			InputStream in;
//    			ByteReader bin;
//    			String s;       
			
//    			in=new StringBufferInputStream("abc\r\na\rbc\n");
//    			bin=new ByteReader(in);
			
//    			s=bin.readLine(); Assert.that(s.equals("abc"), s);
//    			s=bin.readLine(); Assert.that(s.equals("abc"), s);
//    			s=bin.readLine(); Assert.that(s==null, s);
//    			s=bin.readLine(); Assert.that(s==null, s);
			
//    			in=new StringBufferInputStream("a\ne");
//    			bin=new ByteReader(in);
			
//    			s=bin.readLine(); Assert.that(s.equals("a"), s);
//    			s=bin.readLine(); Assert.that(s==null, s);  
//    		} catch (IOException e) {
//    			e.printStackTrace();
//    			Assert.that(false);
//    		}
//    	}
//    }
