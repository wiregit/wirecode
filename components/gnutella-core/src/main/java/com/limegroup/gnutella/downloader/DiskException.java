
package com.limegroup.gnutella.downloader;

import java.io.IOException;


public class DiskException extends IOException {
    public DiskException(String str) {
        super(str);
    }
	public DiskException(IOException cause) {
	    initCause(cause);
	}
}
