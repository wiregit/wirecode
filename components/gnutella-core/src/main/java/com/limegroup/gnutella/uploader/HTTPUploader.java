/**
 * auth: rsoule
 * file: HTTPUploader.java
 * desc: Read data from disk and write to the net.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;

class HTTPUploader {

    private OutputStream _ostream;
    private BufferedWriter _out;
    
    private String _filename;
    private int _index;
    private int _sizeOfFile;
    private int _amountRead;
    private ConnectionManager _manager;
    private ActivityCallback _callback;
    private Socket _socket;   
    private FileManager _fmanager;
    private FileDesc _fdesc;
    private BufferedReader _fin;

    public HTTPUploader(Socket s, String file, 
			int index, ConnectionManager m) {

	_filename = file;
	_index = index;
	_amountRead = 0;
	_socket = s;
	_manager = m;
	_callback = _manager.getCallback();
	_fmanager = FileManager.getFileManager();
	_fdesc = null;
	
	try {	                         /* look for the file */
	    _fdesc = (FileDesc)_fmanager._files.get(_index);
	}                                /* if its not found... */
	catch (ArrayIndexOutOfBoundsException e) {
	    doNoSuchFile();              /* send an HTTP error */
	    return;
	}
	/* check to see if the index */
	if (! _fdesc._name.equals(_filename.trim())) { /* matches the name */
	    doNoSuchFile();
  	    return;
  	}
	
	_sizeOfFile = _fdesc._size;
	
	try {
	    OutputStream _ostream = _socket.getOutputStream();
	    OutputStreamWriter osw = new OutputStreamWriter(_ostream);
	    _out = new BufferedWriter(osw); 
	}
	catch (Exception e) {
	    _callback.error("unable to open outputsetream");
	}

	try {
	    File myFile = new File(_fdesc._path);  /* _path is the full name */
	    _fin = new BufferedReader(new FileReader(myFile));
	}

	catch (Exception e) {
	    _callback.error("unable to open file");
	}

    }
	
    
    public HTTPUploader(String protocal, String host, 
			  int port, String file, ConnectionManager m ) {
			
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

    public void writeHeader() {
	try {
	    _out.write("HTTP 200 OK \r\n");
	    _out.write("Server: Gnutella \r\n");
	    String type = getMimeType();       /* write this method later  */
	    _out.write("Content-type:" + type + "\r\n"); 	
	    _out.write("Content-length: "+ _sizeOfFile + "\r\n");
	    _out.write("\r\n");
	}

	catch (Exception e) {
	    _callback.error("Unable to write header info");
	    return;
	}
    }

    public void run() {
	_callback.addUpload(this);
	doUpload();
	_callback.removeUpload(this);
    }

    public void doUpload() {
	
	int c = -1;
	
	try {
	    while ( (c = _fin.read())  != -1) {
		_out.write(c);
		_amountRead++;
	    }
	    _out.flush();
	}
	
	catch (Exception e) {
	    _callback.error("Unable to read from the file");
	    
	    return;
	    
	}
    }
    
    private String getMimeType() {         /* eventually this method should */
	String mimetype;                /* determine the mime type of a file */
	mimetype = "application/binary"; /* fill in the details of this later */
	return mimetype;                   /* assume binary for now */
    }
    
    private void doNoSuchFile() {
	/* Sends a 404 Not Found message */
	String str;
	
	try {
	    /* is this the right format? */ 
	    _out.write("HTTP 404 Not Found \r\n");   
	    _out.write("\r\n");     /* Even if it is, can Gnutella */ 
	    _out.flush();    /* clients handle it? */          
	}
	
	catch (Exception e) {
	    _callback.error("Unable to write out to the socket");
	}
    }


}






