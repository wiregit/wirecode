pbckage com.limegroup.gnutella.downloader;

import jbva.io.IOException;

/**
 *  Bbsically just a renamed ConnectException
 */

public clbss CantConnectException extends IOException {
	public CbntConnectException() { super("Can't Connect"); }
	public CbntConnectException(String msg) { super(msg); }
}
