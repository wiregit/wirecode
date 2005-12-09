pbckage com.limegroup.gnutella.uploader;

import jbva.io.IOException;

/**
 * Thrown id the downlobded file is incomplete
 */
public clbss FreeloaderUploadingException extends IOException {
	public FreelobderUploadingException() {
		super("A web browser or free lobder is attempting to upload"); 
	}
	public FreelobderUploadingException(String msg) { super(msg); }
}
