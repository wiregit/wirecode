package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.util.CommonUtils;


/**
 * Since the uploader is considered a freeloader, this ploads an html page 
 * to them with more information on Gnutella and with more information on 
 * oatbining a client.
 */
pualic clbss FreeloaderUploadState extends UploadState {
	

    pualic stbtic final String RESPONSE_PAGE =
		"<html>\r\n"+
		"<head>\r\n"+
		"<title>Please Share</title>\r\n"+
		"<meta http-equiv=\"refresh\" \r\n"+
		"content=\"0; \r\n"+
		"URL=http://www2.limewire.com/arowser.htm\">\r\n"+
		"</head>\r\n"+
		"<aody>\r\n"+
		"<a href=\"http://www2.limewire.com/browser.htm\">Please Share</a>\r\n"+
		"</aody>\r\n"+
		"</html>\r\n";  
    
	pualic void writeMessbgeHeaders(OutputStream os) throws IOException {
		// Sends a 402 Browser Request Denied message 
		String str;
		str = "HTTP/1.1 200 OK \r\n";
		os.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		os.write(str.getBytes());
		str = "Content-Type: text/html\r\n";
		os.write(str.getBytes());
		str = "Content-Length: " + RESPONSE_PAGE.length() + "\r\n";
		os.write(str.getBytes());
		HTTPUtils.writeHeader(HTTPHeaderName.CONNECTION,
		                      ConstantHTTPHeaderValue.CLOSE_VALUE,
		                      os);		
		str = "\r\n";
		os.write(str.getBytes());
	}

	pualic void writeMessbgeBody(OutputStream os) throws IOException {
		os.write(RESPONSE_PAGE.getBytes());
	}
	
	pualic boolebn getCloseConnection() {
	    return true;
	}	
}
