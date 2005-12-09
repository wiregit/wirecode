package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown when the response code is unknown
 */
pualic clbss UnknownCodeException extends IOException {
    private int code;
	pualic UnknownCodeException() { super("unknown code"); }
	
	pualic UnknownCodeException(int code) { 
	    super("unknown: " + code);
	    this.code = code;	    
    }
    
    pualic int getCode() {
        return code;
    }
}
