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
    private InputStream _istream;
    private ByteReader _br;
    private int _uploadBegin;
    private int _uploadEnd;

    /** 
     * @requires If isPush, "GIV " was just read from s.  
     *           Otherwise, "GET " was just read from s.
     * @effects  Transfers the file over s in the background.
     *           Throws IOException if the handshake failed.
     */
    public HTTPManager(Socket s, ConnectionManager m, boolean isPush)
	throws IOException {
	
	_socket = s;
	_manager = m;

	String command=null;
	FileManager fm = FileManager.getFileManager();

	try {
	    _istream = _socket.getInputStream();
	    _br = new ByteReader(_istream);
	    command = _br.readLine();   /* read in the first line */
	    if (command==null)
		throw new IOException();
	}
	catch (IOException e) {          /* if there is a problem reading */
	    throw e;                     /* must alert the appropriate */
	}                                /* person */ 


	//All IndexOutOfBoundsException and NumberFormatExceptions
	//are handled below. (They are converted to IOException.)
	try {
	if (!isPush) { 
	    //Expect "GET /get/0/sample.txt HTTP/1.0"	    
	                                       /* I need to get the filename */ 
	    String parse[] = HTTPUtil.stringSplit(command, '/'); 
	                                       /* and the index, but i'm */
	
	                                       /* upset this is way hackey */

	    if (parse.length!=4)
		throw new IOException();
	    if (!parse[0].equals("get"))
		throw new IOException();	    

	    _filename = parse[2].substring(0, parse[2].lastIndexOf("HTTP")-1);
	    _index = java.lang.Integer.parseInt(parse[1]);
	                                       /* is there a better way? */

	    readRange();

	    HTTPUploader uploader;
	    uploader = new HTTPUploader(s, _filename, _index, _manager, _uploadBegin, _uploadEnd);
	    Thread t = new Thread(uploader);
	    t.setDaemon(true);
	    t.start();
	}

	else /* isPush */ { 
	    //Expect  "GIV 0:BC1F6870696111D4A74D0001031AE043/sample.txt\n\n"
	    String next=_br.readLine();
	    if (next==null || (! next.equals(""))) {
		throw new IOException();
	    }
	    
	    //1. Extract file index.  IndexOutOfBoundsException
	    //   or NumberFormatExceptions will be thrown here if there's
	    //   a problem.  They're caught below.
	    int i=command.indexOf(":");
	    _index=Integer.parseInt(command.substring(0,i));
	    //2. Extract clientID.  This can throw 
	    //   IndexOutOfBoundsException or
	    //   IllegalArgumentException, which is caught below.
	    int j=command.indexOf("/", i);
	    byte[] guid=GUID.fromHexString(command.substring(i+1,j));
	    //3. Extract file name.  This can throw 
	    //   IndexOutOfBoundsException.
	    _filename=command.substring(j+1);


	    //Constructor to HTTPUploader checks that we can accept the file.
	    HTTPDownloader downloader;
	    downloader = new HTTPDownloader(s, _filename, _index, guid,
					    _manager);
	    Thread t = new Thread(downloader);
	    t.setDaemon(true);
	    t.start();
	}
	} catch (IndexOutOfBoundsException e) {
	    throw new IOException();
	} catch (NumberFormatException e) {
	    throw new IOException();
	} catch (IllegalArgumentException e) {
	    throw new IOException();
	} catch (IllegalAccessException e) {
	    //We never requested the specified file!
	    throw new IOException();
	}
    }    
    
    public void readRange() throws IOException {
	
	String str = " ";;
	
	_uploadBegin = 0;
	_uploadEnd = 0;


	while (true) {	    

	    str = _br.readLine();

	    if ( (str==null) || (str.equals("")) ){
		break;
	    }

  	    if (str.indexOf("Range: bytes=") != -1) {
  		String sub = str.substring(13);
  		sub = sub.trim();   // remove the white space
  		char c;
  		c = sub.charAt(0);  // get the first character
  		if (c == '-') {  // - n
  		    String second = sub.substring(1);
  		    second = second.trim();
  		    _uploadEnd = java.lang.Integer.parseInt(second);
  		}
  		else {                // m - n or 0 - 
  		    int dash = sub.indexOf("-");
		    
  		    String first = sub.substring(0, dash);
  		    first = first.trim();
		    
  		    _uploadBegin = java.lang.Integer.parseInt(first);
		    
  		    String second = sub.substring(dash+1);
  		    second = second.trim();
		    
  		    if (!second.equals("")) {
  			_uploadEnd = java.lang.Integer.parseInt(second);
  		    }
  		}
		
  	    }
	}
    }

    public void shutdown() {

	try {
	    _socket.close();
	}
	catch (IOException e) {
	}
	
    }

}




