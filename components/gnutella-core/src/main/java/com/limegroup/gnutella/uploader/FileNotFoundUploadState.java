package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import java.io.*;
import java.util.Date;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * an implementaiton of the UploadState interface
 * to report when a file is not found. 
 */

public class FileNotFoundUploadState implements UploadState {

	private HTTPUploader _uploader;
	private OutputStream _ostream;	

	/**
	 * This class implements a HTTP response for the file not being 
	 * found on the server.
	 */
	public void doUpload(HTTPUploader uploader) throws IOException {
		/* Sends a 404 Service Unavailable message */
		_uploader = uploader;
		_ostream = uploader.getOutputStream();

		String str;
		String errMsg = "File not found on server.";
		str = "HTTP/1.1 404 Not Found\r\n";
		_ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getVendor() + "\r\n";
		_ostream.write(str.getBytes());
		str = "Content-Type: text/plain\r\n";
		_ostream.write(str.getBytes());
	    str = "Content-Length: " + errMsg.length() + "\r\n";
		_ostream.write(str.getBytes());
		str = "\r\n";
		_ostream.write(str.getBytes());
		_ostream.write(errMsg.getBytes());
		_ostream.flush();
	}
    
    /**
     * Tells if the upload state doesnt allow the connection to receive
     * another request on the same connection. This state always allows 
     * receiving next request, as the user may request another file that 
     * may be valid
     * @return true, if the upload state doesnt allow the connection to receive
     * another request on the same connection, false otherwise
     */
    public boolean getCloseConnection()
    {
        return false;
    }    

}
