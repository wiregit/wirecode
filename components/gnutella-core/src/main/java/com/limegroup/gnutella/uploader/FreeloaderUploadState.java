package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import java.io.*;

/**
 * auth: rsoule
 * file: FreeloaderUploadState.java
 * desc: an implementaiton of the UploadState interface
 *       that sends an error message for a freeloader
 *       trying toUpload from us. 
 */

public class FreeloaderUploadState implements UploadState {
	
	private HTTPUploader _uploader;
	private OutputStream _ostream;	
  
	/**
	 * This class implements a failed upload 
	 * due to a freeloader making an upload attempt.
	 */
	
	public void doUpload(HTTPUploader uploader) throws IOException {
		/* Sends a 402 Browser Request Denied message */
		_uploader = uploader;
		_ostream = uploader.getOutputStream();

		String str;
		String errMsg = HTTPPage.responsePage;
		str = "HTTP 200 OK \r\n";
		_ostream.write(str.getBytes());
		str = "Server: " + "LimeWire" + "\r\n";
		_ostream.write(str.getBytes());
		str = "Content-Type: text/html\r\n";
		_ostream.write(str.getBytes());
		str = "Content-Length: " + errMsg.length() + "\r\n";
		_ostream.write(str.getBytes());
		str = "\r\n";
		_ostream.write(str.getBytes());
		_ostream.write(errMsg.getBytes());
		_ostream.flush();

	}

}
