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
	    _bis = new BufferedInputStream(_istream);
	    InputStreamReader isr = new InputStreamReader(_istream);
	    _in = new BufferedReader(isr, 1);
	}
	catch (IOException e) {
	    System.out.println("THere was some sort of IOException");
	    e.printStackTrace();
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

	// last 16 bytes of the query reply message...
	byte[] clientGUID = cguid;

	byte ttl = SettingsManager.instance().getTTL();

	// am i passing the right guid's? 

	PushRequest push = new PushRequest(guid, ttl, clientGUID, 
  					   index, ip, port);
	
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

	System.out.println("run being called");

	_callback.addDownload(this);
	doDownload();
	_callback.removeDownload(this);
    }

    
    public void doDownload() {
	
	readHeader();
	
	try {

	    if (_sizeOfFile != -1) {
		

		SettingsManager set = SettingsManager.instance();
		_downloadDir = set.getSaveDirectory();
		String pathname = _downloadDir  + _filename;
		System.out.println("THe Pathname is " + pathname);


		// FileOutputStream myFile = new FileOutputStream(pathname);
		
		// BufferedOutputStream bos = new BufferedOutputStream(myFile);

		int c = -1;
		
		_bis = new BufferedInputStream(_istream);

		

		//  while (true) {
		    
//  		    int buf_size = _bis.available();
//  		    System.out.println("THe amount available: " + buf_size);
//  		    byte[] buf = new byte[buf_size];
//  		    c = _bis.read(buf);
//  		    if (c == -1) 
//  			break;
//  		    // myFile.write(buf);
//  		    bos.write(buf);
//  		    _amountRead+=c;
//  		    count++;
		    
//  		}

		FileOutputStream fos = new FileOutputStream(pathname);
		//  BufferedOutputStream bos = new BufferedOutputStream(fos);
//  		OutputStreamWriter osw = new OutputStreamWriter(bos); 
//  		BufferedWriter out = new BufferedWriter(osw); 
		
		//  char[] buf = new char[1024];

		//  while ((c = _in.read(buf)) != -1) {

//  		    out.write(buf, 0, c);
//  		    _amountRead+=c;

//  		}


		//  while ((c = _istream.read()) != -1) {
		    
//  		    fos.write(c);
//  		    _amountRead+=c;

//  		}


		byte[] buf = new byte[1024];
		
		// while ((c = _istream.read(buf) != -1)) {
		    
		while (true) {
		    c = _istream.read(buf);
		    if (c == -1) 
			break;
		    fos.write(buf, 0, c);
		    _amountRead+=c;
		}


		fos.close();


		System.out.println("THe size of the file: " + _sizeOfFile);


	    }
	}

	catch (Exception e) {
	    
	    _callback.error(ActivityCallback.ERROR_8);
	    System.out.println("E :"+e+":");

	}

    }


    public void doSchmownload() {
	
	System.out.println("DOing the Download....");
	readHeader();
	try {
	    if (_sizeOfFile != -1) {
		SettingsManager set = SettingsManager.instance();
		_downloadDir = set.getSaveDirectory();
		String pathname = _downloadDir  + _filename;
		System.out.println("THe Pathname is " + pathname);
		File myFile = new File(pathname);
		FileOutputStream fos = new FileOutputStream(myFile);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		int count = 0;
		int c = -1;
		byte[] buf = new byte[MAX_BUFF];
		while (true) {
		    int available = _bis.available();
		    System.out.println("THe Amoutn available: " + available);
		    int amount = Math.min(MAX_BUFF, available);
		    System.out.println("THe amount going to try to read " + amount);
		    if (amount != 0) 
			c = _bis.read(buf, 0, amount);
		    else {
			c = _bis.read();
			System.out.println("THe c in the else " + c);
			if (c != -1) {
			    buf[0] = (byte)c;
			    c = 1;
			}
		    }
		    System.out.println("c " + c);
		    if (c == -1) 
			break;

		    bos.write(buf, 0, c);

		    _amountRead+=c;
		    System.out.println("THe Amoutn read: " + _amountRead);
		    count++;
		}
		// bos.flush();

		System.out.println("THe FINAL amount read: " + _amountRead);
		System.out.println("THe size of the file: " + _sizeOfFile);

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

		    String sub = str.substring(15);
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

    public void shutdown()
    {
	try {
	    _socket.close();
	} catch (Exception e) {
	}
    }



}
