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

    private int BUFFSIZE = 1024;
    private int MIN_BUFF = 1024;
    private int MAX_BUFF = 64 * 1024;

    private BufferedReader _in;

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

    /* The server side put */
    public HTTPDownloader(Socket s, String file, ConnectionManager m) {
			
	_filename = file;
	_amountRead = 0;
	_sizeOfFile = -1;
	_socket = s;
	_manager = m;
	_callback = _manager.getCallback();
	_downloadDir = "";


	try {
	    _istream = s.getInputStream();
	    _bis = new BufferedInputStream(_istream);
	    InputStreamReader isr = new InputStreamReader(_istream);
	    _in = new BufferedReader(isr, 1);
	}

	catch (Exception e) {
	    _callback.error(ActivityCallback.ERROR_4);
	}

	SettingsManager set = SettingsManager.instance();
	_downloadDir = set.getSaveDirectory();
	String pathname = _downloadDir  + "/ " + _filename;
	try {
	    _fos = new FileOutputStream(pathname);
	}
	catch (FileNotFoundException e) {
	    
	}
    }
      
    /* The client side get */
    public HTTPDownloader(String protocal, String host, 
			  int port, int index, String file, 
			  ConnectionManager m, byte[] guid ) {
			
	_filename = file;
	_amountRead = 0;
	_sizeOfFile = -1;
	_socket = null;
	_manager = m;
	_callback = _manager.getCallback();
	_downloadDir = "";

	URLConnection conn;

	String furl = "/get/" + String.valueOf(index) + "/" + file;

	try {
	    URL url = new URL(protocal, host, port, furl);
	    conn = url.openConnection();
	}
	catch (java.net.MalformedURLException e) {
	    sendPushRequest(host, index, port, guid);
	    _callback.error(ActivityCallback.ERROR_5);
	    return;
	}
	catch (IOException e) {
	    sendPushRequest(host, index, port, guid);
	    _callback.error(ActivityCallback.ERROR_6);

	    return;
	}
	try {
	    _istream = conn.getInputStream();
	    _bis = new BufferedInputStream(_istream);
	    InputStreamReader isr = new InputStreamReader(_istream);
	    _in = new BufferedReader(isr, 1);
	}
	catch (IOException e) {
	    e.printStackTrace();
	    sendPushRequest(host, index, port, guid);
	    return;

	}

	SettingsManager set = SettingsManager.instance();
	_downloadDir = set.getSaveDirectory();
	String pathname = _downloadDir  + "/ " + _filename;
	try {
	    _fos = new FileOutputStream(pathname);
	}
	catch (FileNotFoundException e) {
	    
	}

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
	    _callback.error(ActivityCallback.ERROR_7);
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
	doDownload();
	_callback.removeDownload(this);
    }

    
    public void doDownload() {
	
	readHeader();
	
	if (_sizeOfFile != -1) {
	    
	    int c = -1;		
	    byte[] buf = new byte[1024];
	    
	    while (true) {
		try {
		    c = _istream.read(buf);
		}
		catch (IOException e) {
		    _callback.error(ActivityCallback.ERROR_8);
		}
		if (c == -1) 
		    break;
		try {
		    _fos.write(buf, 0, c);
		}
		catch (IOException e) {
		    _callback.error(ActivityCallback.ERROR_8);		    
		}
		_amountRead+=c;
	    }
	    try {
		_fos.close();
	    }
	    catch (IOException e) {
		_callback.error(ActivityCallback.ERROR_8);		    
	    }
	    
	}
    }

    public void readHeader() {
	String str = null;
	while (true) {		
	    try {
		str = _in.readLine();
	    }
	    catch (IOException e) {
		_callback.error(ActivityCallback.ERROR_9);
		return;
	    }
	    if (str.indexOf("Content-length:") != -1) {
		String sub = str.substring(15);
		sub.trim();
		_sizeOfFile = java.lang.Integer.parseInt(sub);
		try {
		    str = _in.readLine();
		}
		catch (IOException e) {
		    _callback.error(ActivityCallback.ERROR_9);
		    return;
		}
		break;
	    }
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
