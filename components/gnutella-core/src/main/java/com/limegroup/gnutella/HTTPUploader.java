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
    //////////// Initialized in Constructors ///////////
    public static final int NOT_CONNECTED = 0;
    public static final int CONNECTED = 1;
    public static final int ERROR = 2;
    public static final int COMPLETE = 3;
    /** One of NOT_CONNECTED, CONNECTED, ERROR, COMPLETE */
    private int _state;
    /** True if the connection is server-side, 
     *  false if client-side (actively establishes connection). */
    private boolean _isServer;
    
    private ConnectionManager _manager;
    private ActivityCallback _callback;
    private FileManager _fmanager;

    /** The name of the remote host, in dotted decimal */
    private String _host;
    /** The port on the remote host. */
    private int _port;
    /** Index of the file to upload. */
    private int _index;
    /** Begin and end indices to upload.  If _uploadEnd
     *  is 0, upload the whole file. 
     *  INVARIANT: _uploadBegin<=_uploadEnd */
    private int _uploadBegin;
    private int _uploadEnd;
    private int _amountRead;
    private int _priorAmountRead;


    //////////// Initialized Multiple Places //////////
    /** The socket connecting us to the host.  
     *  Server-side: initialized in constructor.  
     *  Client-side: initialized in connect(). */
    private Socket _socket;
    /** The stream to the foreign host.
     *  Server-side: initialized in constructor.  
     *  Client-side: initialized in run(). */
    private OutputStream _ostream;
    /** The filename.
     *  Server-side: initialized in constructor.
     *  Client-side: initialized in prepareFile */
    private String _filename;
    /** The client ID of the pusher, as a hex string.
     *  Server-side: NOT USED.
     *  Client-side: constructor. */
    private String _clientGUID;


    ////////// Initialized in prepareFile ////////////
    private FileDesc _fdesc;	   
    FileInputStream _fis;
    private int BUFFSIZE = 1024;
    private int _sizeOfFile;
 

    /**
     * Prepares a server-side upload.  The file
     * will actually be transferred when run() is
     * called.
     *
     * @param s the socket to use for transfer
     * @param file the file to transfer
     * @param index the index of the file to transfer
     * @param m my manager.  (used for callbacks, etc.)
     * @param begin the begin offset (in bytes) of the file
     *  to transfer
     * @param end the end offset (in bytes) of the file to
     *  transfer, or 0 to transfer the whole thing.  <b>Must
     *  be greater than equal to end.</b>
     */ 
    public HTTPUploader(Socket s, String file, 
			int index, ConnectionManager m, 
			int begin, int end) {
	_state = CONNECTED;
	_isServer = true;
	_manager = m;
	_callback = _manager.getCallback();
	_fmanager = FileManager.getFileManager();
	_host = s.getInetAddress().getHostAddress();
	_port = s.getPort();
	_index = index;
	_uploadBegin = begin;
	_uploadEnd = end;
	_amountRead = 0;
	_priorAmountRead = 0;
	_socket = s;
	try {
	    _ostream = s.getOutputStream();
	} catch (IOException e) {
	    _state = ERROR;
	}
	_filename = file.trim(); //TODO: really?
    }
	
    
    /** 
     * Prepares a client-side upload via push.  This does not block.
     * The call to run will actually establish the connection, send
     * GIV, wait for GET, and send OK followed by data.
     * 
     * @param host the host to push the file to
     * @param port the port to contact that host on
     * @param index the index of the file to push
     * @param myClientGUID my client ID (given to remote host as
     *  a crude form of authentication) as a hex String representation
     *  as given by GUID.toHexString
     * @param m my manager.  (used for callbacks, etc.)
     */
    public HTTPUploader(String host, int port, 
			int index, 
			String guidString, ConnectionManager m) {

	_state = NOT_CONNECTED;
	_isServer = false;
	_manager = m;
	_callback = _manager.getCallback();
	_fmanager = FileManager.getFileManager();
	_host = host;
	_port = port;
	_index = index;
	_uploadBegin = 0;
	_uploadEnd = 0;
	_amountRead = 0;
	_priorAmountRead = 0;
	_clientGUID=guidString;
    }

    /** 
     * Connects a client-side upload connection. 
     *
     * @requires this is an unconnected client-side upload connection
     * @modifies this, network
     * @effects establishes the outgoing connection, sends the give
     *  message, and waits for the get.  Throws IOException if any
     *  part of this sequence failed.
     */
    public void connect() throws IOException {
	//1. Establish connection.
	System.out.println("Establishing connection...");
	try {
	    _socket=new Socket(_host, _port);
	} catch (SecurityException e) {
	    throw new IOException();
	} 
	_ostream=_socket.getOutputStream();
	BufferedWriter out=new BufferedWriter(new OutputStreamWriter(_ostream));

	//2. Send GIV
	Assert.that(_filename!=null);
	String give="GIV "+_index+":"+_clientGUID+"/"+_filename+"\n\n";
	System.out.println("Writing: '"+give+"'");			   
	out.write(give);
	out.flush();

	//3. Wait for 	"GET /get/0/sample.txt HTTP/1.0"	    
	//   This code is stolen from HTTPManager.
	//   It should really be factored into some method.
	//   TODO2: timeout, range headers.
	System.out.println("Waiting for GET...");
	try {
	    ByteReader in=new ByteReader(_socket.getInputStream());
	    String line=in.readLine();
	    if (line==null)
		throw new IOException();
	    System.out.println("Pulling apart GET...");
	    if (! line.startsWith("GET "))
		throw new IOException();
	    String command=line.substring(4,line.length());
	    
	                                       /* I need to get the filename */ 
	    String parse[] = HTTPUtil.stringSplit(command, '/'); 
	                                       /* and the index, but i'm */
	
	                                       /* upset this is way hackey */
	    if (parse.length!=4)
		throw new IOException();
	    if (!parse[0].equals("get"))
		throw new IOException();	    
	    
	    //Check that the filename matches what we sent
	    //in the GIV request.  I guess it doesn't need
	    //to match technically, but we check to be safe.
	    String filename = parse[2].substring(0, parse[2].lastIndexOf("HTTP"));
	    if (filename!=_filename)
		throw new IOException();
	    int index = java.lang.Integer.parseInt(parse[1]);
	    if (index!=_index)
		throw new IOException();
	} catch (IndexOutOfBoundsException e) {
	    throw new IOException();
	} catch (NumberFormatException e) {
	    throw new IOException();
	} catch (IllegalArgumentException e) {
	    throw new IOException();
	}
	System.out.println("Done.");
    }

    /** 
     * @requires filename filled in.
     * @modifies this._fdesc...this._priorAmountRead
     * @effects prepares the file named _filename for uploading by setting
     *  this._fdesc...this._priorAmountRead.  Throws FileNotFoundException
     *  if the file can't be uploaded.
     */
    private void prepareFile() throws FileNotFoundException {
	//TODO: this used to have a special case for
	//zero-length files.  Does it still work ok?
	try {	                         /* look for the file */
	    _fdesc = (FileDesc)_fmanager._files.get(_index);
	}                                /* if its not found... */
	catch (ArrayIndexOutOfBoundsException e) {
	    throw new FileNotFoundException();
	}
	_fdesc._name = _fdesc._name.trim(); //TODO: ?
	
	/* For client-side uploads, get name. For server-side
	 * uploads, check to see that the index matches the
	 * filename. */
	if (_filename==null) {
	    _filename=_fdesc._name;
	} else {
	   if (! (_fdesc._name).equals(_filename))  /* matches the name */
	       throw new FileNotFoundException();
	}
	
	_sizeOfFile = _fdesc._size;


	try {
	    String f = _fdesc._path;	    
	    File myFile = new File(f);  /* _path is the full name */
	    String foo = myFile.toString();
	    _fis = new FileInputStream(myFile);

	} catch (Exception e) {
	    throw new FileNotFoundException();
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
		return InetAddress.getByName(new String(_host));
	    }
	    catch (Exception e) {
	    }
	}
	return null;
    }   

    public void writeHeader() throws IOException {
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
	    
	    int end;
	    if (_uploadEnd != 0)
		end = _uploadEnd;
	    else 
		end = _sizeOfFile;

	    str = "Content-range:" + _uploadBegin + "-" + end + "\r\n";
		
	    _ostream.write(str.getBytes());
		
	    str = "\r\n";
	    _ostream.write(str.getBytes());

	}

	catch (IOException e) {
	    throw e;
	}
    }

    public void run() {
	if (_state==ERROR) {
	    shutdown();
	    return;
	}

	try {
	    prepareFile();
	} catch (FileNotFoundException e) {
	    _state = ERROR;
	    doNoSuchFile();
	    shutdown();
	    return;
	}
	
	try {
	    _callback.addUpload(this);
	    if (! _isServer) {
		connect();
		_state = CONNECTED;
	    }
	    doUpload(); //sends headers via writeHeader
	    _state = COMPLETE;
	    _callback.removeUpload(this);	    
	} catch (IOException e) {
	    _state = ERROR;
	} finally {
	    shutdown();
	}
    }

    public void doUpload() throws IOException {
	writeHeader();
	int c = -1;
	int available = 0;

	byte[] buf = new byte[1024];
	while (true) {
	    try {

		_fis.skip(_uploadBegin);

		if ((_uploadEnd != 0) && 
		    (_uploadEnd == _amountRead ))
		    break;
		     
		c = _fis.read(buf);
	    }
	    catch (IOException e) {
		throw e;
	    }
	    if (c == -1) 
		break;
	    try {
		_ostream.write(buf, 0, c);
	    }
	    catch (IOException e) {
		throw e;
	    }
	    _amountRead += c;

	}
	try {
	    _ostream.close();
	}
	catch (IOException e) {
	    throw e;
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
	    _fis.close();
	} catch (Exception e) {
	}
	try {
	    _ostream.close();
	} catch (Exception e) {
	}
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


