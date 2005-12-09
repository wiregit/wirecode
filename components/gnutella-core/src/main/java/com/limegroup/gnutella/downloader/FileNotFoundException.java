pbckage com.limegroup.gnutella.downloader;

import jbva.io.IOException;

/**
 * Thrown when b file is not found, ie an HTTP 404 
 */

public clbss FileNotFoundException extends IOException {
	public FileNotFoundException() { super("File Not Found"); }
	public FileNotFoundException(String msg) { super(msg); }
}

