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
	    _callback.error(ActivityCallback.ERROR_4);
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

	URLConnection conn;

	// port = 1111; // For Testing.

	String furl = "/get/" + String.valueOf(index) + "/" + file;

	try {
	    URL url = new URL(protocal, host, port, furl);
	    System.out.println("URL :"+url+":");
	    conn = url.openConnection();
	}
	catch (java.net.MalformedURLException e) {
	    System.out.println("Catching Malformed URL Exception");
	    sendPushRequest(host, index, port, guid);
	    _callback.error(ActivityCallback.ERROR_5);
	    return;
	}
	catch (IOException e) {

	    System.out.println("Catching IO Exception");
	    sendPushRequest(host, index, port, guid);
	    _callback.error(ActivityCallback.ERROR_6);

	    return;
	}
	try {
	    _istream = conn.getInputStream();
	    InputStreamReader isr = new InputStreamReader(_istream);
	    _in = new BufferedReader(isr);
	}
	catch (IOException e) {
	    sendPushRequest(host, index, port, guid);
	    // _callback.error(ActivityCallback.ERROR_4);
	    return;

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

	System.out.println("The length of the manager guid " + guid.length);

	// last 16 bytes of the query reply message...
	byte[] clientGUID = cguid;

	System.out.println("The length of the guid " + cguid.length);

	byte ttl = SettingsManager.instance().getTTL();

	// am i passing the right guid's? 

	PushRequest push = new PushRequest(guid, ttl, clientGUID, 
  					   index, ip, port);
	
	//  PushRequest push = new PushRequest(clientGUID, ttl, guid,  
//  					   index, ip, port);
	
	try {

	    System.out.println("Sending push Request");
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

	try {

	    if (_sizeOfFile != -1) {
		
		FileOutputStream myFile = new FileOutputStream(_filename);
		
		int count = 0;
		int c = -1;
		
		while (true) {
		    
		    int buf_size = _istream.available();
		    byte[] buf = new byte[buf_size];
		    c = _istream.read(buf);
		    if (c == -1) 
			break;
		    myFile.write(buf);
		    _amountRead+=c;
		    count++;
		    
		}

//  		byte[] buf = new byte[BUFFSIZE];
//  		while ( (c = _istream.read(buf) ) != -1) {
//  		    myFile.write(buf);
//  		    _amountRead+=c;
//  		    count++;
//  		}
		    
	    }
	}

	catch (Exception e) {
	    
	    _callback.error(ActivityCallback.ERROR_8);
	    System.out.println("E :"+e+":");

	}

    }

    public void readHeader() {
	
	String str = null;
	
	try {
	
	    while (true) {
		
		str = _in.readLine();
		System.out.println("DH :"+str+":");
 
		if (str.indexOf("Content-length:") != -1) {

		    String sub = str.substring(16);
		    sub.trim();
		    _sizeOfFile = java.lang.Integer.parseInt(sub);
		    str = _in.readLine();
		    System.out.println("DH2 :"+str+":");
		    break;
		    
		}
		
	    }
	}
	catch (Exception e) {
	    _callback.error(ActivityCallback.ERROR_9);
	}
	    
    }


}
