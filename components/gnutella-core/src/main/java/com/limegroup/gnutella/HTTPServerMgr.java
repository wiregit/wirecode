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
    private ActivityCallback _callback;
    

    public HTTPServerMgr(Socket s, String filename_path, ConnectionManager m) {

	_socket = s;

	_manager = m;

	_filename = filename_path;

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
	
	_callback = _manager.getCallback();
	
	_callback.addDownload(this);
	
	download();

	_callback.removeDownload(this);

    }

    public void download() {

	InputStream istream;
	BufferedReader in;

	try {

	    istream = _socket.getInputStream();
	                                /* get the data from the socket */
	    InputStreamReader isr = new InputStreamReader(istream);
	    in = new BufferedReader(isr);

	}

	catch (Exception e) {
	    
	    _callback.error("Unable to get the inputstream from the socket");
	    return;
	    
	}
	
	
	String str = null;               
	
	try { 
	    
	    while (true) {          /* reading http header information */
		
		str = in.readLine();      /* get the line */ 
		
		System.out.println("The str " + str);

		/* check if it is */
		if (str.indexOf("Content-length:") != -1) { 
		                                /* the content length */
		    String sub = str.substring(16); /* get the number portion */ 
		
		    sub.trim();                     /* remove the whitespace */
		
		    _sizeOfFile = java.lang.Integer.parseInt(sub);
						
		    System.out.println("The sub " + sub);

		    in.readLine();                    /* read the /r/n */
		
		    break;                        
		
		}
		
	    }
	}
	catch (Exception e) {
	    
	    _callback.error("Unable to read HTTP Header info");
	    return;
	    
	}

	try {
	    	    
	    if (_sizeOfFile != -1) {

		FileOutputStream myFile = new FileOutputStream(_filename);

		int count = 0;
		int c=-1;
		while ( (c = istream.read() ) != -1) {
		    
		    myFile.write(c);
		    _amountRead += c;
		    count++;
		    

		}
		
	    }
	
	}
	
	catch (Exception e) {
	    
	    _callback.error("Unable to write to file");
	    
	}
	
    }
}





