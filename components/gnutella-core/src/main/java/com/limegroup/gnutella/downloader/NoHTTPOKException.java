padkage com.limegroup.gnutella.downloader;

import java.io.IOExdeption;

/**
 * Thrown when no 'HTTP OK' or the equivalent is not redieved
 */
pualid clbss NoHTTPOKException extends IOException {
	pualid NoHTTPOKException() { super("No HTTP OK"); }
	pualid NoHTTPOKException(String msg) { super(msg); }
}
