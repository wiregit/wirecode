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


    private int NOT_CONNECTED = 0;
    private int CONNECTED = 1;
    private int ERROR = 2;
    private int COMPLETE = 3;

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

    private boolean _okay;

    private int _state;

    public HTTPUploader(Socket s, String file, 
			int index, ConnectionManager m) {


	_state = NOT_CONNECTED;

	_okay = false;

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
	    doNoSuchFile();              /* send an HTTP error */
	    _state = ERROR;
	    return;
	}
	/* check to see if the index */

	_filename = _filename.trim();

	_fdesc._name = _fdesc._name.trim(); 

	if (! (_fdesc._name).equals(_filename)) { /* matches the name */
  	    doNoSuchFile();
	    _state = ERROR;
    	    return;
    	}
	
	_sizeOfFile = _fdesc._size;

	if (_sizeOfFile == 0) 
	    return;
	
	try {
	    _ostream = _socket.getOutputStream();
	}
	catch (Exception e) {
	    _state = ERROR;
	    return;
	}

	try {
	    String f = _fdesc._path;	    
	    File myFile = new File(f);  /* _path is the full name */
	    String foo = myFile.toString();
	    _fis = new FileInputStream(myFile);

	}

	catch (Exception e) {
	    _state = ERROR;
	    return;
	}

	_okay = true;

	_state = CONNECTED;

    }
	
    
    public HTTPUploader(String protocal, String host, 
			  int port, String file, ConnectionManager m ) {

	_state = NOT_CONNECTED;

	_okay = false;

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
	    _state = ERROR;
	    return;
	}

	/* check to see if the index */
	if (! (_fdesc._name.trim()).equals(_filename.trim())) { /* matches the name */
	    doNoSuchFile();
	    _state = ERROR;
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
	    _state = ERROR;
	    return;
	}

	URLConnection conn;

	try {
	    URL url = new URL(protocal, host, port, file);
	    conn = url.openConnection();
	}
	catch (java.net.MalformedURLException e) {
	    _state = ERROR;
	    return;
	}
	catch (IOException e) {
	    _state = ERROR;
	    return;
	}
	catch (Exception e) {
	    _state = ERROR;
	    return;
	}
	try {
	    _ostream = conn.getOutputStream();
	}
	catch (IOException e) {
	    _state = ERROR;
	    return;
	}
	catch (Exception e) {
	    _state = ERROR;
	    return;
	}

	_state = CONNECTED;

	_okay = true;
			
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
		return InetAddress.getByName(new String(_host));
	    }
	    catch (Exception e) {
	    }
	}
	return null;
    }   

    public void writeHeader() {
	try {
	    
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
	    _state = ERROR;
	    return;
	}
    }

    public void run() {

	if (_okay) {
	    _callback.addUpload(this);
	    doUpload();
	    _callback.removeUpload(this);
	}
	else {
	    shutdown();
	}
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
		_state = ERROR;
	    }
	    if (c == -1) 
		break;
	    try {
		_ostream.write(buf, 0, c);
	    }
	    catch (IOException e) {
		_state = ERROR;
	    }
	    _amountRead += c;

	}
	try {
	    _ostream.close();
	}
	catch (IOException e) {
	    _state = ERROR;
	}

	_state = COMPLETE;


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
	    _state = ERROR;
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

    }

    public int getState() {
	return _state;
    }

}


