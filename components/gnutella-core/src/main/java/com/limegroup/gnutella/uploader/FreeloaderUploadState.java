package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import java.io.*;
import com.limegroup.gnutella.util.CommonUtils;


/**
 * Since the uploader is considered a freeloader, this ploads an html page 
 * to them with more information on Gnutella and with more information on 
 * obtaining a client.
 */
public class FreeloaderUploadState implements UploadState {

    public static final String RESPONSE_PAGE =
		"<html>\r\n"+
		"<head>\r\n"+
		"<title>Please Share</title>\r\n"+
		"<meta http-equiv=\"refresh\" \r\n"+
		"content=\"0; \r\n"+
		"URL=http://www2.limewire.com/browser.htm\">\r\n"+
		"</head>\r\n"+
		"<body>\r\n"+
		"<a href=\"http://www2.limewire.com/browser.htm\">Please Share</a>\r\n"+
		"</body>\r\n"+
		"</html>\r\n";  


	/**
	 * This class implements a failed upload 
	 * due to a freeloader making an upload attempt.
	 */	
	public void doUpload(HTTPUploader uploader) throws IOException {
		// Sends a 402 Browser Request Denied message 
		OutputStream os = uploader.getOutputStream();

		String str;
		str = "HTTP 200 OK \r\n";
		os.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		os.write(str.getBytes());
		str = "Content-Type: text/html\r\n";
		os.write(str.getBytes());
		str = "Content-Length: " + RESPONSE_PAGE.length() + "\r\n";
		os.write(str.getBytes());
		str = "\r\n";
		os.write(str.getBytes());
		os.write(RESPONSE_PAGE.getBytes());
		os.flush();

	}
    
    /**
     * Tells if the upload state doesnt allow the connection to receive
     * another request on the same connection. This state never allows 
     * receiving next request.
     * @return true, if the upload state doesnt allow the connection to receive
     * another request on the same connection, false otherwise
     */
    public boolean getCloseConnection() {
        return true;
    }    

}
