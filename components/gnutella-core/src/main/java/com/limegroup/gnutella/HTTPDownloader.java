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
import java.util.*;

public class HTTPDownloader implements Runnable {


    public static final int NOT_CONNECTED = 0;
    public static final int CONNECTED = 1;
    public static final int ERROR = 2;
    public static final int COMPLETE = 3;

    private int BUFFSIZE = 1024;
    private int MIN_BUFF = 1024;
    private int MAX_BUFF = 64 * 1024;

    private InputStream _istream;
    private String _filename;
    private int _sizeOfFile;
    private int _amountRead;
    private ConnectionManager _manager;
    private ActivityCallback _callback;
    private Socket _socket;
    private String _downloadDir;
    private BufferedInputStream _bis;
    private FileOutputStream _fos;

    String _protocol;
    String _host;
    int _port;
    int _index;
    byte[] _guid;    
    private ByteReader _br;
    private boolean _okay;
    private int _mode;

    private int _state;

    private boolean _resume;
    private int _finalAmount;
    private int _initialAmount;


    /* The server side put */
    public HTTPDownloader(Socket s, String file, ConnectionManager m) {
	
	_state = NOT_CONNECTED;
	_okay = false;
	_mode = 1;
	_filename = file;
	_amountRead = 0;
	_sizeOfFile = -1;
	_socket = s;
	_manager = m;
	_callback = _manager.getCallback();
	_downloadDir = "";
    }

    public void initOne() {

	try {
	    _istream = _socket.getInputStream();
	    _br = new ByteReader(_istream);
	}
	catch (Exception e) {
	    _state = ERROR;
	    return;
	}

	_state = CONNECTED;

	_okay = true;
	
    }
      

  


    /* The client side get */
    public HTTPDownloader(String protocol, String host, 
			  int port, int index, String file, 
			  ConnectionManager m, byte[] guid ) {
			
	_state = NOT_CONNECTED;
	_okay = false;
	_mode = 2;
	_filename = file;
	_amountRead = 0;
	_sizeOfFile = -1;
	_socket = null;
	_manager = m;
	_callback = _manager.getCallback();
	_downloadDir = "";

	_protocol = protocol;
	_host = host;
	_index = index;
	_port = port;
	_guid = guid;

    }

    public void initTwo() {

	URLConnection conn;

	String furl = "/get/" + String.valueOf(_index) + "/" + _filename;

	try {

	    URL url = new URL(_protocol, _host, _port, furl);
	    conn = url.openConnection();
	}
	catch (java.net.MalformedURLException e) {
	    sendPushRequest(_host, _index, _port, _guid);
	    return;
	}
	catch (IOException e) {
	    sendPushRequest(_host, _index, _port, _guid);
	    return;
	}
	catch (Exception e) {
	    _state = ERROR;
	    return;
	}
  	try {
	    _istream = conn.getInputStream();
	    _br = new ByteReader(_istream);
  	}
	catch (NoRouteToHostException e) {
	    _state = ERROR;
	    return;
	}
  	catch (IOException e) {
  	    sendPushRequest(_host, _index, _port, _guid);
  	    return;

  	}
	catch (Exception e) {
	    _state = ERROR;
	    return;
	}

	_state = CONNECTED;
	_okay = true;

    }
    
    public int getState() {
	return _state;
    }

    public void sendPushRequest(String hostname, int index, 
				int port, byte[] cguid) {

	StringTokenizer tokenizer = new StringTokenizer(hostname,".");
	String a = tokenizer.nextToken();
	String b = tokenizer.nextToken();
	String c = tokenizer.nextToken();
	String d = tokenizer.nextToken();
	
	int a1 = Integer.parseInt(a);
	int b1 = Integer.parseInt(b);
	int c1 = Integer.parseInt(c);
	int d1 = Integer.parseInt(d);
	byte[] ip = {(byte)a1, (byte)b1,(byte)c1,(byte)d1};

	byte[] guid = GUID.fromHexString(_manager.ClientId);

	// last 16 bytes of the query reply message...
	byte[] clientGUID = cguid;

	byte ttl = SettingsManager.instance().getTTL();

	// am i passing the right guid's? 

	PushRequest push = new PushRequest(guid, ttl, clientGUID, 
  					   index, ip, port);
	
	try {
	    _manager.sendToAll(push);
	}
	catch (Exception e) {
	    _state = ERROR;
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

    /* need to change this */
    public InetAddress getInetAddress() {
	
	if (_socket == null) 
	    return null;
	else 
	    return _socket.getInetAddress();
	
    }

    public void run() {

	_callback.addDownload(this);

	if (_mode == 1)
	    initOne();
	else if (_mode ==2)
	    initTwo();
	else
	    return;

	 if (_okay) {
	    doDownload();
	    _callback.removeDownload(this);
	 }

	 
    }

    

   
    public void exit() {
	try {
	    _br.close();
	    _fos.close();
	}
	catch (IOException e) {
	}
    }
    

    public void setResume() {
	_resume = true;
    }

    public void doDownload() {

	readHeader();

	SettingsManager set = SettingsManager.instance();
	
	_downloadDir = set.getSaveDirectory();

	String pathname = _downloadDir + _filename;

	File myFile = new File(pathname);

	if ((myFile.exists()) && (!_resume)) {
	    // ask the user if the file should be overwritten
	    _state = ERROR;
	    return;
	}
		
	try {
	    _fos = new FileOutputStream(pathname, _resume);
	}
	    
	catch (FileNotFoundException e) {
	    _state = ERROR;
	    return;
	}
	
	catch (Exception e) {
	    _state = ERROR;
	    return;
	}
	
	int c = -1;
	
	byte[] buf = new byte[1024]; 
	
	int amountToRead = _finalAmount - _initialAmount;

	
	//  System.out.println("amountToRead: " + amountToRead);
//  	System.out.println("_initialAmount: " + _initialAmount);
//  	System.out.println("_finalAmount: " + _finalAmount);

	//TODO3: Note we ignore the file length.  In the rare case that the
	//remote host doesn't close the connection, this will loop forever.
	//However, this may be the sensible thing to do since many clients may
	//get Content-length wrong.
	while (true) {
	    
	    if (_amountRead == amountToRead) {
		_state = COMPLETE;		
		break;
	    }

	    try {
		c = _br.read(buf);
	    }
	    catch (Exception e) {
		_state = ERROR;
		return;
	    }
	    
	    if (c == -1)
		break;
	    
	    try {
		_fos.write(buf, 0, c);
	    }
	    catch (Exception e) {
		_state = ERROR;
		break;
	    }

	    _amountRead+=c;
	    
	}
	
	try {
	    _br.close();
	    _fos.close();
	}
	
	catch (IOException e) {
	    _state = ERROR;
	    return;
	}

	if ( _amountRead == _sizeOfFile )
	    _state = COMPLETE;
	else
	    _state = ERROR;
	
    }

    
    public void readHeader() {
	String str = " ";
	
	boolean foundLength = false;
	boolean foundRangeInitial = false;
	boolean foundRangeFinal = false;
	
	//TODO1: what if Content-length is not the last header?
	//Better to look for the blank line after headers.
	while (!str.equals("")) {
	    
	    str = _br.readLine();
	    //System.out.println(str);
	    
	    if (str.indexOf("Content-length:") != -1) {
		    String sub;
		    try {
			sub=str.substring(15);
		    } catch (ArrayIndexOutOfBoundsException e) {
		    _state = ERROR;
		    return;
		    }
		    
		sub = sub.trim();
		
		try {
		    _sizeOfFile = java.lang.Integer.parseInt(sub);
		}
		catch (NumberFormatException e) {
		    _state = ERROR;
		    return;
		}

		foundLength = true;;
	    }

	    if (str.indexOf("Range: bytes=") != -1) {
		String sub = str.substring(13);
		sub = sub.trim();   // remove the white space
		char c;
		c = sub.charAt(0);  // get the first character
		if (c == '-') {  // - n
		    String second = sub.substring(1);
		    second = second.trim();
		    _finalAmount = java.lang.Integer.parseInt(second);
		    foundRangeFinal = true;
		}
		else {                // m - n or 0 - 
		    int dash = sub.indexOf("-");

		    String first = sub.substring(0, dash);
		    first = first.trim();

		    _initialAmount = java.lang.Integer.parseInt(first);
		    foundRangeInitial = true;

		    String second = sub.substring(dash+1);
		    second = second.trim();

		    if (!second.equals("")) {
			_finalAmount = java.lang.Integer.parseInt(second);
			foundRangeFinal = true;
		    }
		}
		
	    }
	
	    
	}
	
	if (!foundLength) {
	    _state = ERROR;
	}

	if (!foundRangeInitial) {
	    _initialAmount = 0;
	}
	
	if (!foundRangeFinal) {
	    _finalAmount = _sizeOfFile;
	}
	
    }
    
    public void shutdown()
    {
	try {
	    _istream.close();
	} catch (Exception e) {
	}
	try {
	    _fos.close();
	} catch (Exception e) {
	}
	try {
	    _socket.close();
	} catch (Exception e) {
	}
    }



}
