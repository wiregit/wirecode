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


    private int NOT_CONNECTED = 0;
    private int CONNECTED = 1;
    private int ERROR = 2;
    private int COMPLETE = 3;

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

	if (_mode == 1)
	    initOne();
	else if (_mode ==2)
	    initTwo();
	else
	    return;

	 if (_okay) {
	    _callback.addDownload(this);
	    doDownload();
	    _callback.removeDownload(this);
	 }

	 
    }

    

    public void doDownload() {

	SettingsManager set = SettingsManager.instance();
	_downloadDir = set.getSaveDirectory();
	
	String pathname = _downloadDir + _filename;
	
	try {
	    _fos = new FileOutputStream(pathname);
	}
	catch (FileNotFoundException e) {
	    _state = ERROR;
	    return;
  	}
	catch (Exception e) {
	    _state = ERROR;
	    return;
	}
	
	readHeader();
	
	int c = -1;
	
	byte[] buf = new byte[1024]; 

	while (true) {
	    
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

	_state = COMPLETE;
    }


    public void exit() {
	try {
	    _br.close();
	    _fos.close();
	}
	catch (IOException e) {
	}
    }
    

     public void readHeader() {
	String str = null;

	int flag = 0;

	while (true) {		
		str = _br.readLine();
		System.out.println(str);

	    if (str.indexOf("Content-length:") != -1) {
		String sub = str.substring(15);

		sub.trim();
		
		try {
		    _sizeOfFile = java.lang.Integer.parseInt(sub);
		}
		catch (NumberFormatException e) {
		    _state = ERROR;
		    return;
		}
		str = _br.readLine();
	
		break;
	    }
	}

	if (flag == 0) {
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



}
