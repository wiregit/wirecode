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

    private static final int BUFFSIZE = 1024; //the buffer size for IO

    private Socket _socket;

    private String _filename;
    
    private int _sizeOfFile;

    private int _amountRead;

    private ConnectionManager _manager;

    public HTTPServerMgr(Socket s, String filename, ConnectionManager m) {

	_socket = s;

	_manager = m;

	_filename = filename;

	_sizeOfFile = -1;

	_amountRead = 0;

    }


    public String getFileName() {
	return _filename;
    }

    public int getContentLength() {
	return _sizeOfFile;
    }
    
    public int getAmountRead() {
	return _amountRead;
    }
    public InetAddress getInetAddress() {
	return _socket.getInetAddress();
    }

    public void run() {

	download();

    }

    public void download() {

	System.out.println("doing the download...");

	try {

	    InputStream istream = _socket.getInputStream();
	                                /* get the data from the socket */
	    InputStreamReader isr = new InputStreamReader(istream);
	    BufferedReader in = new BufferedReader(isr);

	    String str = null;               

	    while (true) {          /* reading http header information */
		
		str = in.readLine();      /* get the line */ 
	                                             /* check if it is */
		if (str.indexOf("Content-length:") != -1) { 
		                                /* the content length */
		    String sub = str.substring(16); /* get the number portion */ 
		
		    sub.trim();                     /* remove the whitespace */
		
		    _sizeOfFile = java.lang.Integer.parseInt(sub);
						
		    in.readLine();                    /* read the /r/n */
		
		    break;                        
		
		}
		
	    }
	    
	    if (_sizeOfFile != -1) {

		//  FileOutputStream myFile = new FileOutputStream(_filename);

//  		byte[] data = new byte[BUFFSIZE];

//  		while (true) {

//  		    int got = istream.read(data);

//  		    if (got==-1)
//  			break;

//  		    _amountRead += got;

//  		    myFile.write(data, 0, got);      /* and write out to it */

//  		}

		FileOutputStream myFile = new FileOutputStream(_filename);

		int count = 0;
		int c=-1;
		while ( (c = istream.read() ) != -1) {
		    
		    myFile.write(c);
		    count++;

		}

	    }
	
	}
	
	catch (Exception e) {
	    
	    e.printStackTrace();
	    
	}
	
    }
}





