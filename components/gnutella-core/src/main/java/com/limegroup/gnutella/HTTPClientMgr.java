/**
 * auth: rsoule
 * file: HTTPClientMgr.java
 * desc: This class will handle all HTTP client interactions.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;

public class HTTPClientMgr implements Runnable {

    private static int BUFFSIZE = 1024;

    private Socket _socket;
    private BufferedWriter _out;
    private String _filename;
    private int _index;
    private int _sizeOfFile;
    private int _amountRead;
    private FileManager _fmanager;
    private FileDesc _fdesc;
    
    public HTTPClientMgr(Socket s, String filename, int index) {
	
	_socket = s;                    /* initialize variables */
	_filename = filename;
	_index = index;
	_amountRead = 0;

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


    }

    public void run() {

	upload();

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

    public void upload() {
	                                 /* most of this code is taken */
	try {

	    OutputStream os = _socket.getOutputStream();
	    OutputStreamWriter osw = new OutputStreamWriter(os);
	    _out = new BufferedWriter(osw); 
	    
	}							  

  	catch (IOException e) {
	    
	    e.printStackTrace();

  	    return;
  	}

  	BufferedReader fin = null;

	try {/* used for writing to the socket */
	    
	                                   /* TODO1: this is potentially a */
  	                                   /* security flaw.  */
	                                   /* Double-check this is right!! */  	  
	    File file = new File(_fdesc._path);  /* _path is the full name */

  	    fin = new BufferedReader(new FileReader(file));

  	} 
	catch (FileNotFoundException e) {

  	    doNoSuchFile();

  	    return;
  	}	    

	try {
	    _out.write("HTTP 200 OK \r\n");
	    _out.write("Server: Gnutella \r\n");
	    String type = getMimeType();       /* write this method later  */
	    _out.write("Content-type:" + type + "\r\n"); 	
	    _out.write("Content-length: "+ _sizeOfFile + "\r\n");
	    _out.write("\r\n");
	}

	catch (Exception e) {

	    e.printStackTrace();

	}
	                               
  	char[] buf = new char[BUFFSIZE];   	

  	while (true) {

	    try {
	    
		int got=fin.read(buf);         /* Read the file from disk, */
		
		if (got==-1)
		    break;
		
		_amountRead += got;

		_out.write(buf, 0, got);        /* write to network. */
		
	    }

	    catch (Exception e) {

		e.printStackTrace();

	    }
  	}

	try {
	    _out.flush();

	}

	catch (Exception e) {

	}
    }


    private void resume() {

    }

    private void abort() {

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
   	                          /* clients handle it? */          

	    _out.flush();
	}

	catch (Exception e) {
	}
    }
}










