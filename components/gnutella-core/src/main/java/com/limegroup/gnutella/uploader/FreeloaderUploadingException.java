package com.limegroup.gnutella.uploader;

import java.io.IOException;

/**
 * Thrown id the downloaded file is incomplete
 */
public class FreeloaderUploadingException extends IOException {
	public FreeloaderUploadingException() {
		super("A web browser or free loader is attempting to upload"); 
	}
	public FreeloaderUploadingException(String msg) { super(msg); }
}
