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

    private Socket _socket;
    private BufferedWriter _out;
    
    String _filename;
    int _index;

    private static int BUFFSIZE = 1024;

    public HTTPClientMgr(Socket s, String filename, int index) {
	
	_socket = s;

	_filename = filename;

	_index = index;

    }

    public void run() {

	upload();

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

	                                 /* used for writing to the socket */

	FileManager fmanager = FileManager.getFileManager();

	FileDesc fd = null;

	try {
	                                 /* look for the file */
	    fd = (FileDesc)fmanager._files.get(_index);
	                                 
	}                                /* if its not found... */
                                         
	catch (ArrayIndexOutOfBoundsException e) {
	    
	    doNoSuchFile();              /* send an HTTP error */

	    return;

	}

	if (! fd._name.equals(_filename.trim())) { /* check to see if the index */
	                                   /* matches the name */
	    doNoSuchFile();
		
  	    return;

  	}

  	BufferedReader fin = null;
	try {/* used for writing to the socket */
	    
	                                   /* TODO1: this is potentially a */
  	                                   /* security flaw.  */
	                                   /* Double-check this is right!! */  	  
	    File file = new File(fd._path);  /* _path is the full name */

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
	    _out.write("Content-length: "+ fd._size + "\r\n");
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










