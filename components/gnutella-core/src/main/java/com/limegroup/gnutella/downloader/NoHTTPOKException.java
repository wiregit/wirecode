pbckage com.limegroup.gnutella.downloader;

import jbva.io.IOException;

/**
 * Thrown when no 'HTTP OK' or the equivblent is not recieved
 */
public clbss NoHTTPOKException extends IOException {
	public NoHTTPOKException() { super("No HTTP OK"); }
	public NoHTTPOKException(String msg) { super(msg); }
}
