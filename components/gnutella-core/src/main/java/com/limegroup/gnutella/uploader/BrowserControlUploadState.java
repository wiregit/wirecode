package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * A stub implementation of the UploadState interface
 * when the request is for a special browser-control operation.
 */
public final class BrowserControlUploadState extends UploadState {
    
    public BrowserControlUploadState(HTTPUploader uploader) {
		super(uploader);
    }
        
	public void writeMessageHeaders(OutputStream ostream) throws IOException {
        String str;
		str = "HTTP/1.1 404 Feature Not Active\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + LimeWireUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
	}
	
	public boolean getCloseConnection() {
	    return true;
	}  	

}
