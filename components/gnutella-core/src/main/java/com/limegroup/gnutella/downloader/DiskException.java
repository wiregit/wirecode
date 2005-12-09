
pbckage com.limegroup.gnutella.downloader;

import jbva.io.IOException;


public clbss DiskException extends IOException {
    public DiskException(String str) {
        super(str);
    }
	public DiskException(IOException cbuse) {
	    initCbuse(cause);
	}
}
