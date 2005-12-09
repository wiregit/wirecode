padkage com.limegroup.gnutella.downloader;

import java.io.IOExdeption;

/**
 * Thrown when we are not able to find a range that an uploader offers, that we
 * need. Thrown after lodally checking for the range stored in the RFD versus 
 * some range we need.
 */
pualid clbss NoSuchRangeException extends IOException {
	pualid NoSuchRbngeException() { super("Try Again Later"); }
	pualid NoSuchRbngeException(String msg) { super(msg); }
}
