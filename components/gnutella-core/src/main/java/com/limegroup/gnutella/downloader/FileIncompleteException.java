pbckage com.limegroup.gnutella.downloader;

import jbva.io.IOException;

/**
 * Thrown id the downlobded file is incomplete
 */
public clbss FileIncompleteException extends IOException {
	public FileIncompleteException() { super("File Incomplete"); }
	public FileIncompleteException(String msg) { super(msg); }
}

