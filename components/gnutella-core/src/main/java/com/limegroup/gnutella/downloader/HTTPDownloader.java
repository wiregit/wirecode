/**
 * auth: rsoule
 * file: HTTPDownloader.java
 * desc: Read data from the net and write to disk.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;

public class HTTPDownloader implements Runnable {

    private InputStream _istream;
    private BufferedReader _in;

    private String _filename;
    private int _sizeOfFile;
    private int _amountRead;
    private ConnectionManager _manager;
    private ActivityCallback _callback;
    private Socket _socket;

    /* The server side put */
    public HTTPDownloader(Socket s, String file, ConnectionManager m) {
			  	
	_filename = file;
	_amountRead = 0;
	_sizeOfFile = -1;
	_socket = s;
	_manager = m;
	_callback = _manager.getCallback();

	try {
	    _istream = s.getInputStream();
	    InputStreamReader isr = new InputStreamReader(_istream);
	    _in = new BufferedReader(isr);
	}

	catch (Exception e) {
	    _callback.error("unable to open inputstream");
	}
	
    }
      
    /* The client side get */
    public HTTPDownloader(String protocal, String host, 
			  int port, String file, ConnectionManager m ) {
			
	_filename = file;
	_amountRead = 0;
	_sizeOfFile = -1;
	_socket = null;
	_manager = m;
	_callback = _manager.getCallback();

	URLConnection conn;

	try {
	    URL url = new URL(protocal, host, port, file);
	    conn = url.openConnection();
	}
	catch (java.net.MalformedURLException e) {
	    _callback.error("Bad URL");
	    return;
	}
	catch (IOException e) {
	    _callback.error("can't open connection");
	    return;
	}
	try {
	    _istream = conn.getInputStream();
	    InputStreamReader isr = new InputStreamReader(_istream);
	    _in = new BufferedReader(isr);
	}
	catch (IOException e) {
	    _callback.error("can't open input stream");
	}

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

    /* need to change this */
    public InetAddress getInetAddress() {
	
	if (_socket == null) 
	    return null;
	else 
	    return _socket.getInetAddress();
	
    }

    public void run() {
	_callback.addDownload(this);
	doDownload();
	_callback.removeDownload(this);
    }

    public void doDownload() {
	
	readHeader();

	try {

	    if (_sizeOfFile != -1) {
		
		FileOutputStream myFile = new FileOutputStream(_filename);
		
		int count = 0;
		int c = -1;
		
		while ( (c = _istream.read() ) != -1) {
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

    public void readHeader() {
	
	String str = null;
	
	try {
	
	    while (true) {
		
		str = _in.readLine();
		
		if (str.indexOf("Content-length:") != -1) {

		    String sub = str.substring(16);
		    sub.trim();
		    _sizeOfFile = java.lang.Integer.parseInt(sub);
		    _in.readLine();
		    break;
		    
		}
		
	    }
	}
	catch (Exception e) {
	    _callback.error("Unable to read the header information");
	}
	    
    }


}
