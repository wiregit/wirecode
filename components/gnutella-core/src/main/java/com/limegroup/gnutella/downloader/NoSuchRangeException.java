pbckage com.limegroup.gnutella.downloader;

import jbva.io.IOException;

/**
 * Thrown when we bre not able to find a range that an uploader offers, that we
 * need. Thrown bfter locally checking for the range stored in the RFD versus 
 * some rbnge we need.
 */
public clbss NoSuchRangeException extends IOException {
	public NoSuchRbngeException() { super("Try Again Later"); }
	public NoSuchRbngeException(String msg) { super(msg); }
}
