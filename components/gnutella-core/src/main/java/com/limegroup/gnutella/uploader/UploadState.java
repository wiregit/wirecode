package com.limegroup.gnutella.uploader;

import java.io.*;
import java.net.*;
import java.util.Date;

// import com.sun.java.util.collections.*;

/**
 * This interface encapsulates the behavior associated
 * with different upload states.  The actual HTTPUploader
 * class will swap different implementations of this 
 * interface, depending on the circumstances surrounding
 * the upload.  This is based on the STATE pattern in the 
 * Design Patterns book.
 *
 */

public interface UploadState {
	
	
	/**
	 * The doUpload method must be defined in the acutal
	 * implementations of this interface.  At the time
	 * of writing this, there will be a normal implementation
	 * that sends the appropriate header information, and the
	 * file.  There will be an error implementation that send
	 * the appropriate error information, such as 404 or 503.
	 * There is also a failed push implementation that will 
	 * do nothing.
     * <p> Note: This method should never attempt to close the I/O streams
     * but should leave that responsibility with the caller.
	 */
	public void doUpload(HTTPUploader uploader) throws IOException;
    
    /**
     * Tells if the upload state doesnt allow the connection to receive 
     * another request on the same connection.
     * @return true, if the upload state doesnt allow the connection to receive 
     * another request on the same connection, false otherwise
     */
    public boolean getCloseConnection();

}
