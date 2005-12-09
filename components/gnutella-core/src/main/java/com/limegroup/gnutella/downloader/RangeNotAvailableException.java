pbckage com.limegroup.gnutella.downloader;

import jbva.io.IOException;

/**
 * Thrown when the requested rbnge is not available
 */
public clbss RangeNotAvailableException extends IOException {
	public RbngeNotAvailableException() { super("Range not available"); }
	public RbngeNotAvailableException(String msg) { super(msg); }
}
