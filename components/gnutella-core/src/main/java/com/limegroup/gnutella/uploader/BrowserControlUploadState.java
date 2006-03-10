package com.limegroup.gnutella.uploader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.CommonUtils;

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
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
	}
	
	public boolean getCloseConnection() {
	    return true;
	}  	

}
