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

    /** 
     * The list of all files we've requested via push messages.  Only
     * files matching this description may be accepted from incoming
     * connections.  Duplicates ARE allowed in the (rare) case that
     * the user wants to download the same file multiple times.  This
     * list is static because it must be shared by all
     * HTTPDownloader's.  Note that it is synchronized.  
     */
    private static List /* of PushRequestedFile */ requested=
	Collections.synchronizedList(new LinkedList());
    /** The maximum time, in SECONDS, allowed between a push request and an
     *  incoming push connection. */
    private static final int PUSH_INVALIDATE_TIME=3*60;  //3 minutes
    

    /* 
     * Server side download in response to incoming PUT request.
     * 
     * @requires a GIV command was just read from s, e.g., 
     *     "GIV 0:BC1F6870696111D4A74D0001031AE043/sample.txt"
     * @effects Creates a downloader that will download the specified file
     *  in the background when the run method is called.   Throws
     *  IllegalAccessException if we never requested the file.
     */
    public HTTPDownloader(Socket s, String file, int index, byte[] clientGUID,
			  ConnectionManager m) throws IllegalAccessException {
	//Authentication: check if we are requested via push.  First we clear
	//out very old entries in requested.
	Date time=new Date();
	time.setTime(time.getTime()-(PUSH_INVALIDATE_TIME*1000)); 
	synchronized (requested) {
	    Iterator iter=requested.iterator();
	    while (iter.hasNext()) {
		PushRequestedFile prf=(PushRequestedFile)iter.next();
		if (prf.before(time))
		    iter.remove();
	    }
	}
	
	byte[] ip=s.getInetAddress().getAddress();
	PushRequestedFile prf=new PushRequestedFile(clientGUID, file,
						    ip, index);
	boolean found=requested.remove(prf);
	if (! found)
	    throw new IllegalAccessException();

	_guid=clientGUID;
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
	_index=index;
    }

    /** Sends an HTTP GET request, i.e., "GET /get/0/sample.txt HTTP/1.0"  
     *  (Response will be read later in readHeader()). */
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
	
	//Note that the following code is similar to initTwo except that it does
	//not use the built in Java URL/URLConnection classes (since we've already
	//established the connection).  Ideally we should have special methods
	//to handle the HTTP formatting, but this is kind of a hack.
	try {
	    BufferedWriter out=new BufferedWriter(
				 new OutputStreamWriter(_socket.getOutputStream()));
	    out.write("GET /get/"+_index+"/"+_filename+" HTTP/1.0\r\n");
	    out.write("User-Agent: Gnutella\r\n");
	    out.write("\r\n");
	    out.flush();
	} catch (IOException e) {
	    _state = ERROR;
	    _okay = false;
	    return;
	}
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
	    conn.connect();
	    _istream = conn.getInputStream();
	    _br = new ByteReader(_istream);
	}
	catch (java.net.MalformedURLException e) {
	    sendPushRequest();
	    return;
	}
	catch (IOException e) {
	    sendPushRequest();
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

    /** 
     * @requires this is an outgoing (normal) HTTPDownloader
     * @effects sends a push request for the file specified at
     *  at construction time from host specified in the constructor.
     */
    public void sendPushRequest() {
	final byte[] clientGUID=_guid;

	//Record this push so incoming push connections can be verified.
	Assert.that(clientGUID!=null);
	Assert.that(_filename!=null);
	byte[] remoteIP=null;
	try {
	    remoteIP=InetAddress.getByName(_host).getAddress();
	} catch (UnknownHostException e) {
	    //This shouldn't ever fail because _host should be
	    //in dotted decimal fashion.  If it were to someone be
	    //thrown legitimately, there would be no point in continuing
	    //since we would never accept the corresponding incoming
	    //push connection.
	    _state = ERROR;
	    return;
	}
	PushRequestedFile prf=new PushRequestedFile(clientGUID, _filename,
						    remoteIP, _index);
	requested.add(prf);
	
	//TODO1: Should this be a new mGUID or the mGUID of the corresponding
	//query and reply?  I claim it should be a totally new mGUID, since
	//push requests are ONLY routed based on their cGUID.  This way, clients
	//could record mGUID's of push requests to avoid routing duplicate
	//push requests.  While there should never be duplicate pushes if people
	//route push request, this is unfortunately NOT the case.
	byte[] messageGUID = Message.makeGuid();
	byte ttl = SettingsManager.instance().getTTL();
	byte[] myIP = _manager.getAddress();
	int myPort=_manager.getListeningPort();
	    
	PushRequest push = new PushRequest(messageGUID, ttl, clientGUID, 
  					   _index, myIP, myPort);
	
	try {
	    //ROUTE the push to the appropriate connection, if possible.
	    Connection c=_manager.pushRouteTable.get(clientGUID);
	    if (c!=null)
		c.send(push);
	    else {
		_state = ERROR;
		return;
	    }
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
	    
	    if (str.indexOf("Content-length:") != -1) {
		String sub;
		try {
		    sub=str.substring(15);
		} catch (IndexOutOfBoundsException e) {
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

/** A file that we requested via a push message. */
class PushRequestedFile {
    byte[] clientGUID;
    String filename;
    byte[] ip;
    int index;
    Date time;

    public PushRequestedFile(byte[] clientGUID, String filename, 
			     byte[] ip, int index) {
	this.clientGUID=clientGUID;
	this.filename=filename;
	this.ip=ip;
	this.index=index;
	this.time=new Date();
    }

    /** Returns true if this request was made before the given time. */
    public boolean before(Date time) {
	return this.time.before(time);
    }

    public boolean equals(Object o) {
	if (! (o instanceof PushRequestedFile))
	    return false;

	PushRequestedFile prf=(PushRequestedFile)o;
	return Arrays.equals(clientGUID, prf.clientGUID)	    
	    && filename.equals(prf.filename)
	    //Because of the following line, hosts that used faked IP addresses
	    //will not be able to connect to you.
  	    && Arrays.equals(ip, prf.ip)
	    && index==prf.index;
    }
    
    public int hashCode() {
	//This is good enough since we'll rarely request the 
	//same file over and over.
	return filename.hashCode();
    }

    public String toString() {
	String ips=ByteOrder.ubyte2int(ip[0])+"."
	         + ByteOrder.ubyte2int(ip[1])+"."
	         + ByteOrder.ubyte2int(ip[2])+"."
	         + ByteOrder.ubyte2int(ip[3]);
	return "<"+filename+", "+index+", "
	    +(new GUID(clientGUID).toString())+", "+ips+">";
    }
}
