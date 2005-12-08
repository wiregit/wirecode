pbckage com.limegroup.gnutella.downloader;

import jbva.io.IOException;

/**
 * Thrown when the slots bre filled, and the client should
 * try bgain later, ie an HTTP 503
 */
public clbss TryAgainLaterException extends IOException {
	public TryAgbinLaterException() { super("Try Again Later"); }
	public TryAgbinLaterException(String msg) { super(msg); }
}
