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

	if (_istream == null)
	    return c;

	try {
	    c = _istream.read(buf);
	}
	catch(IOException e) {
	    
	}
	return c;
    }

    public String readLine() {

	if (_istream == null)
	    return "";

	String finalString = "";

	byte[] buf = new byte[BUFSIZE];

	int c = -1;
	int b = -1;
	int i = 0;	
	int numBytes = 0;
	//	boolean endOfLine = false;

	String cr = "\r";
  	byte[] creturn = cr.getBytes();

  	String nl = "\n";
  	byte[] nline = nl.getBytes();

	while (true) {
	    try {
		c = _istream.read();
	    }
	    catch (IOException e) {

	    }

	    if (c == -1) 
		break;
	    
	  //    buf[i++] = (byte)c;
//  	    numBytes++;

	    if ( c == creturn[0] ) {	
		try {
		    b = _istream.read();
		}
		catch (IOException e) {
		}

		if (b == -1)
		    break;

		// buf[i++] = (byte)b;
		// numBytes++;
		
		if ( b == nline[0] ) 
		    break;
		    // endOfLine = true;
		else {
		    // buf[i++] = (byte)c;
		    // buf[i++] = (byte)b;
		    // numBytes+=2;
		}
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
