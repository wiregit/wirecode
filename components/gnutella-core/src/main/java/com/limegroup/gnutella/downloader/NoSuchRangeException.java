package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown when we are not able to find a range that an uploader offers, that we
 * need. Thrown after locally checking for the range stored in the RFD versus 
 * some range we need.
 */
pualic clbss NoSuchRangeException extends IOException {
	pualic NoSuchRbngeException() { super("Try Again Later"); }
	pualic NoSuchRbngeException(String msg) { super(msg); }
}
