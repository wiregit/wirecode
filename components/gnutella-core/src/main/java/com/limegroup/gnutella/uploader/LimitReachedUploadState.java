package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import java.io.*;
import com.limegroup.gnutella.util.CommonUtils;


/**
 * an implementaiton of the UploadState 
 * that sends an error message for the limit of
 * uploads allowed has been reached. This is
 * an HTTP 503 error.
 */

public class LimitReachedUploadState implements UploadState {
  
	/**
	 * Implements a failed upload due to the Upload limit 
	 * having been reached.
	 */
	public void doUpload(HTTPUploader uploader) throws IOException {
		/* Sends a 503 Service Unavailable message */
		OutputStream ostream = uploader.getOutputStream();

		String str;
		String errMsg = "Server busy.  Too many active uploads.";
		str = "HTTP/1.1 503 Service Unavailable\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getVendor() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: text/plain\r\n";
		ostream.write(str.getBytes());
		str = "Content-Length: " + errMsg.length() + "\r\n";
		ostream.write(str.getBytes());
		FileDesc fileDesc = uploader.getFileDesc();
		if(fileDesc != null) {
			fileDesc.writeAlternateLocationsTo(ostream);
		}
		str = "\r\n";
		ostream.write(str.getBytes());
		ostream.write(errMsg.getBytes());
		ostream.flush();
	}
    
    /**
     * Tells if the upload state doesnt allow the connection to receive
     * another request on the same connection. This state never allows 
     * receiving next request.
     * @return true, if the upload state doesnt allow the connection to receive
     * another request on the same connection, false otherwise
     */
    public boolean getCloseConnection(){
        return true;
    }    

}
