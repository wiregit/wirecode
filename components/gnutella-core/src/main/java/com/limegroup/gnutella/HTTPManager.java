/**
 * auth: rsoule
 * file: HTTPManager.java
 * desc: This class handles the server side upload
 *       and download.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;

public class HTTPManager {

    private Socket _socket;
    private String _filename;
    private int _index;
    private ConnectionManager _manager;

    public HTTPManager(Socket s, ConnectionManager m) 
	throws IOException {
	
	_socket = s;
	_manager = m;

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
	}                              /* person */ 


	if ((command.indexOf("get")) != -1) {  /* check if the stream is GET */
	                                       /* I need to get the filename */ 

	    String parse[] = HTTPUtil.stringSplit(command, '/'); 
	                                       /* and the index, but i'm */


	    _filename = parse[2].substring(0, parse[2].lastIndexOf("HTTP"));
	    //String parse_two[] = HTTPUtil.stringSplit(parse[2], 'H'); 
	                             /* concerned this is way hackey */
	    //  NO KIDDING YOU DUFUS!!!!  Your lucky I found this ever.
	    //  So is the whole concept here
	    //_filename = parse_two[0];
	
	    _index = java.lang.Integer.parseInt(parse[1]);
	                                       /* is there a better way? */

	    HTTPUploader uploader;
	    uploader = new HTTPUploader(s, _filename, _index, _manager);
	    Thread t = new Thread(uploader);
	    t.setDaemon(true);
	    t.start();
	}

	else if ((command.indexOf("put")) != -1) { /* or if it is PUT */

	    if (_filename != null) {
		
		HTTPDownloader downloader;
		downloader = new HTTPDownloader(s, _filename, _manager);
		Thread t = new Thread(downloader);
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




