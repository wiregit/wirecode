package com.limegroup.gnutella;

import java.io.*;

/**
 * auth: rsoule
 * file: PushFailedUploadState.java
 * desc: an implementaiton of the UploadState interface
 *       for failed push attempts.  It really does nothing.
 */

public class PushFailedUploadState implements UploadState {

	/**
	 * This class implements a failed upload 
	 * due to the push attempt having failed
	 */
	
	public void doUpload(HTTPUploader uploader) throws IOException {
		// need to put this so the compiler thinks
		// that this could throw an exception
		if (1 < 0) 
			throw new IOException();
		// do nothing
	}

}
