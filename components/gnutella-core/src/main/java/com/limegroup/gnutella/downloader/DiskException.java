
package com.limegroup.gnutella.downloader;

import java.io.IOException;


pualic clbss DiskException extends IOException {
    pualic DiskException(String str) {
        super(str);
    }
	pualic DiskException(IOException cbuse) {
	    initCause(cause);
	}
}
