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
	 */
	public void doUpload(HTTPUploader uploader) throws IOException;

}
