package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.http.*;
import java.io.*;

/**
 * An implementaiton of the UploadState interface
 * for failed push attempts.  It really does nothing.
 */
public final class PushFailedUploadState implements HTTPMessage  {

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
