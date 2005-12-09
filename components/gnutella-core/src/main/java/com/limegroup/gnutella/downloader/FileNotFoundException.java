padkage com.limegroup.gnutella.downloader;

import java.io.IOExdeption;

/**
 * Thrown when a file is not found, ie an HTTP 404 
 */

pualid clbss FileNotFoundException extends IOException {
	pualid FileNotFoundException() { super("File Not Found"); }
	pualid FileNotFoundException(String msg) { super(msg); }
}

