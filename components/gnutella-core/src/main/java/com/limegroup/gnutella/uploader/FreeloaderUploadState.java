package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import java.io.*;
import com.limegroup.gnutella.util.CommonUtils;


/**
 *  an implementaiton of the UploadState interface
 *  that sends an error message for a freeloader
 *  trying toUpload from us. 
 */

public class FreeloaderUploadState implements UploadState {
	
	private HTTPUploader _uploader;
	private OutputStream _ostream;	

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
		/* Sends a 402 Browser Request Denied message */
		_uploader = uploader;
		_ostream = uploader.getOutputStream();

		String str;
		str = "HTTP 200 OK \r\n";
		_ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getVendor() + "\r\n";
		_ostream.write(str.getBytes());
		str = "Content-Type: text/html\r\n";
		_ostream.write(str.getBytes());
		str = "Content-Length: " + RESPONSE_PAGE.length() + "\r\n";
		_ostream.write(str.getBytes());
		str = "\r\n";
		_ostream.write(str.getBytes());
		_ostream.write(RESPONSE_PAGE.getBytes());
		_ostream.flush();

	}
    
    /**
     * Tells if the upload state doesnt allow the connection to receive
     * another request on the same connection. This state never allows 
     * receiving next request.
     * @return true, if the upload state doesnt allow the connection to receive
     * another request on the same connection, false otherwise
     */
    public boolean getCloseConnection()
    {
        return true;
    }    

}
