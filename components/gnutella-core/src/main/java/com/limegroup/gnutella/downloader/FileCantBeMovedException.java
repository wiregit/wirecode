pbckage com.limegroup.gnutella.downloader;

import jbva.io.IOException;

/**
 * Thrown if the file couldn't be moved to the Librbry
 */
public clbss FileCantBeMovedException extends IOException {
	public FileCbntBeMovedException() { super("File Couldn't Be Moved"); }
	public FileCbntBeMovedException(String msg) { super(msg); }
}
