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

public class HTTPUploader implements Runnable {

    private int BUFFSIZE = 1024;

    private OutputStream _ostream;
    
    private String _filename;
    private int _index;
    private int _sizeOfFile;
    private int _amountRead;
    private int _priorAmountRead;
    private ConnectionManager _manager;
    private ActivityCallback _callback;
    private Socket _socket;   
    private FileManager _fmanager;
    private FileDesc _fdesc;	
    private String _host;
    
    FileInputStream _fis;


    public HTTPUploader(Socket s, String file, 
			int index, ConnectionManager m) {

	// System.out.println("In the first upload constructor");

	file = file.trim();

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
	    //	    System.out.println("ERROR A");
	    doNoSuchFile();              /* send an HTTP error */
	    return;
	}
	/* check to see if the index */
	if (! (_fdesc._name.trim()).equals(_filename.trim())) { /* matches the name */
	    //	    System.out.println("ERROR B");
  	    doNoSuchFile();
    	    return;
    	}
	
	_sizeOfFile = _fdesc._size;

	if (_sizeOfFile == 0) 
	    return;
	
	try {
	    _ostream = _socket.getOutputStream();
	}
	catch (Exception e) {
	    //	    System.out.println("ERROR C");
	    uploadError("unable to open outputsetream");
	}

	try {
	    String f = _fdesc._path;
	    
	    File myFile = new File(f);  /* _path is the full name */

	    String foo = myFile.toString();
	    
	    _fis = new FileInputStream(myFile);

	}

	catch (Exception e) {
	    uploadError("unable to open file");
	}

    }
	
    
    public HTTPUploader(String protocal, String host, 
			  int port, String file, ConnectionManager m ) {

	_host = host;
	_filename = file;
	_amountRead = 0;
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
	if (! (_fdesc._name.trim()).equals(_filename.trim())) { /* matches the name */
	    doNoSuchFile();
	    return;
  	}
	
	_sizeOfFile = _fdesc._size;

	if (_sizeOfFile == 0)
	    return;

	try {
	    File myFile = new File(_fdesc._path);  /* _path is the full name */
	    _fis = new FileInputStream(myFile);
	}
	
	catch (Exception e) {
	    uploadError("unable to open file");
	}

	URLConnection conn;

	try {
	    URL url = new URL(protocal, host, port, file);
	    conn = url.openConnection();
	}
	catch (java.net.MalformedURLException e) {
	    uploadError("Bad URL");
	    return;
	}
	catch (IOException e) {
	    uploadError("can't opeInputStreamReader n connection");
	    return;
	}
	catch (Exception e) {
	    uploadError("Unknown error occured:");
	    // e.printStackTrace();
	    return;
	}
	try {
	    _ostream = conn.getOutputStream();
	}
	catch (IOException e) {
	    uploadError("can't open output stream");
	    // e.printStackTrace();
	    return;
	}
	catch (Exception e) {
	    uploadError("Unknown error occured:");
	    // e.printStackTrace();
	    return;
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

    public int getPriorAmountRead() {
	return _priorAmountRead;
    }

    public void setPriorAmountRead( int val ) {
	_priorAmountRead = val;
    }

    public InetAddress getInetAddress() {
	if (_socket != null) 
	    return _socket.getInetAddress();
	else {
	    try {
		//		System.out.println("Host " + new String(_host));
		return InetAddress.getByName(new String(_host));
	    }
	    catch (Exception e) {
		//   System.out.println("The get by name didn't work");
	    }
	}
	return null;
    }   

    public void writeHeader() {
	try {
	    
	    // _out.write("HTTP 200 OK \r\n");
	    // _out.write("Server: Gnutella \r\n");
	    // String type = getMimeType();       /* write this method later  */
	    // _out.write("Content-type:" + type + "\r\n"); 	
	    // _out.write("Content-length:"+ _sizeOfFile + "\r\n");
	    // _out.write("\r\n");
	    // _out.flush();

	    String str = "HTTP 200 OK \r\n";

	    _ostream.write(str.getBytes());
	    str = "Server: Gnutella \r\n";
	    _ostream.write(str.getBytes());
	    String type = getMimeType();       /* write this method later  */
	    str = "Content-type:" + type + "\r\n"; 	
	    _ostream.write(str.getBytes());
	    str = "Content-length:"+ _sizeOfFile + "\r\n";
	    _ostream.write(str.getBytes());
	    str = "\r\n";
	    _ostream.write(str.getBytes());

	}

	catch (Exception e) {
	    uploadError("Unable to write header info");
	    return;
	}
    }

    public void run() {

	//	System.out.println("In the upload run");
	
	_callback.addUpload(this);
	doUpload();
	_callback.removeUpload(this);
    }

    public void doUpload() {
	writeHeader();
	int c = -1;
	int available = 0;

	byte[] buf = new byte[1024];
	while (true) {
	    try {
		c = _fis.read(buf);
	    }
	    catch (IOException e) {

	    }
	    if (c == -1) 
		break;
	    try {
		_ostream.write(buf, 0, c);
	    }
	    catch (IOException e) {
		uploadError("Unable to write to the socket");		
	    }
	    _amountRead += c;

	}
	try {
	    _ostream.close();
	}
	catch (IOException e) {
	    uploadError("Unable to close the socket");		
	}


    }
    
    private String getMimeType() {         /* eventually this method should */
	String mimetype;                /* determine the mime type of a file */
	mimetype = "application/binary"; /* fill in the details of this later */
	return mimetype;                   /* assume binary for now */
    }
    
    private void doNoSuchFile() {
	/* Sends a 404 Not Found message */
	try {
	    /* is this the right format? */ 
	    String str;
	    str = "HTTP 404 Not Found \r\n";   
	    _ostream.write(str.getBytes());
	    str = "\r\n";     /* Even if it is, can Gnutella */ 
	    _ostream.write(str.getBytes());
	    _ostream.flush();    /* clients handle it? */          
	}
	
	catch (Exception e) {
	    uploadError("Unable to write out to the socket");
	}
    }
    

    public void shutdown()
    {
	try {
	    _socket.close();
	} catch (Exception e) {
	}
    }

    private void uploadError(String str)
    {
	//	System.out.println(str);
	// These should not go anywhere for uploads
    }

}


