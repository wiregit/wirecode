package com.limegroup.gnutella.uploader;

import java.io.*;
import com.limegroup.gnutella.util.CommonUtils;


/**
 * an implementaiton of the UploadState 
 * that sends an error message for the limit of
 * uploads allowed has been reached. This is
 * an HTTP 503 error.
 */

public class LimitReachedUploadState implements UploadState {

	private HTTPUploader _uploader;
	private OutputStream _ostream;	
  
	/**
	 * This class implements a failed upload 
	 * due to the Upload limit having been reached.
	 */
	public void doUpload(HTTPUploader uploader) throws IOException {
		/* Sends a 503 Service Unavailable message */
		_uploader = uploader;
		_ostream = uploader.getOutputStream();

		String str;
		String errMsg = "Server busy.  Too many active downloads.";
		str = "HTTP/1.1 503 Service Unavailable\r\n";
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
		_uploader.stop();

	}

}
