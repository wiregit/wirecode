/**
 * auth: rsoule
 * file: HTTPMgr.java
 * desc: This class will be the general HTTPManager, through
 *       which all HTTP interactions will pass. 
 *
 *  Downloading from a server
 *    
 *  /get/1234/strawberry-rhubarb-pies.rcp HTTP/1.0\r\n
 *  Connection: Keep-Alive\r\n
 *  Range: bytes=0-\r\n
 *  \r\n
 
 *  The server responds
 *
 *  HTTP 200 OK\r\n
 *  Server: Gnutella\r\n
 *  Content-type:application/binary\r\n
 *  Content-length: 948\r\n
 *  \r\n
 *
 * See ftp://ftp.isi.edu/in-notes/rfc2616.txt for the HTTP spec.
 * 
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;

public class HTTPMgr {

    private Socket _socket;
    private String _filename;
    private int _index;

    public HTTPMgr(Socket s) throws IOException {

	_socket = s;                   /* creation of a socket connection */
	
	BufferedReader in;   
 
	String command;

	FileManager fm = FileManager.getFileManager();
	
	try {                          /* try to read the HTTP header */ 

	    InputStream is = _socket.getInputStream();
	    InputStreamReader isr = new InputStreamReader(is); 
	    in = new BufferedReader(isr); 
	
	    command = in.readLine();   /* read in the first line */

	}

	catch (Exception e) {          /* if there is a problem reading */
	
	    e.printStackTrace();
    
	    return;                    /* must alert the appropriate */
	                               /* person */ 
	}

	if ((command.indexOf("get")) != -1) {  /* check if the stream is GET */
	                                       /* I need to get the filename */ 

	    String parse[] = HTTPUtil.stringSplit(command, '/'); 
	                                       /* and the index, but i'm */
	    String parse_two[] = HTTPUtil.stringSplit(parse[2], 'H'); 
	                                       /* concerned this is way hackey */

	    _index = java.lang.Integer.parseInt(parse[1]);
	                                       /* is there a better way? */
	    _filename = parse_two[0];

	    HTTPClientMgr client;
	    client = new HTTPClientMgr(s, _filename, _index);    

	    Thread t = new Thread(client);
	    t.setDaemon(true);
	    t.start();

	}

	else if ((command.indexOf("put")) != -1) { /* or if it is PUT */

	    if (_filename != null) {
		
		HTTPServerMgr server;
		server = new HTTPServerMgr(s, _filename);
		Thread t = new Thread(server);
		t.setDaemon(true);
		t.start();
		
	    }

	}

	else {
	    
	    throw new IOException();

	}
    }    

    public void shutdown() {
	try {
	    _socket.close();
	}
	catch (IOException e) {
	    System.out.println("Could not close the socket");
	    e.printStackTrace();
	}
	 
    }

}




