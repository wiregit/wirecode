package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.http.*;
import java.io.*;

/**
 * an implementaiton of the UploadState interface
 * for failed push attempts.  It really does nothing.
 */
public class PushFailedUploadState implements HTTPMessage  {

	/**
	 * This class implements a failed upload 
	 * due to the push attempt having failed
	 */	
	//public void doUpload(HTTPUploader uploader) throws IOException {
		// need to put this so the compiler thinks
		// that this could throw an exception
	//if (1 < 0) 
	//throw new IOException();
		// do nothing
	//}

	public void writeMessageHeaders(OutputStream os) throws IOException {
		// does nothing
	}

	public void writeMessageBody(OutputStream os) throws IOException {
		// does nothing
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
