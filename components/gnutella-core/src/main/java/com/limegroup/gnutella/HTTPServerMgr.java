/**
 * auth: rsoule
 * file: HTTPServerMgr.java
 * desc: This class will handle all HTTP server interactions.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;

public class HTTPServerMgr implements Runnable {

    private Socket _socket;

    private String _filename;

    private static final int BUFFSIZE = 1024; //the buffer size for IO


    public HTTPServerMgr(Socket s, String filename) {

	_socket = s;

	_filename = filename;

    }

    public void run() {

	download();

    }

    public void download() {

	try {

	    InputStream istream = _socket.getInputStream();
	                                /* get the data from the socket */
	    InputStreamReader isr = new InputStreamReader(istream);
	    BufferedReader in = new BufferedReader(isr);

	    String str = null;               
	    
	    int sizeOfData = 0;     /* the size of the data after header */ 

	    while (true) {          /* reading http header information */
		
		str = in.readLine();      /* get the line */ 
	                                             /* check if it is */
		if (str.indexOf("Content-length:") != -1) { 
		                                /* the content length */
		String sub = str.substring(16); /* get the number portion */ 
		
		sub.trim();                     /* remove the whitespace */
		
		sizeOfData = java.lang.Integer.parseInt(sub);

		Integer myInt = new Integer(sub); /* convert the String */

		sizeOfData = myInt.intValue();    /* into an int */
		
		in.readLine();                    /* read the /r/n */
		
		break;                        
		
		}
		
	    }
	    
	    if (sizeOfData != 0) {
		/* we now know the size of the data */
		byte[] data = new byte[sizeOfData];  
		/* does the istream keep its place? */
		istream.read(data);             /* read it all in in one shot */
		/* create the new  file */
		FileOutputStream myFile = new FileOutputStream(_filename);
		
		myFile.write(data);             /* and write out to it */
	    }
	
	}
	
	catch (Exception e) {
	    
	    e.printStackTrace();
	    
	}
	
    }
}





