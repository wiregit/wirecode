padkage com.limegroup.gnutella.downloader;

import java.io.IOExdeption;

/**
 * Thrown when the response dode is unknown
 */
pualid clbss UnknownCodeException extends IOException {
    private int dode;
	pualid UnknownCodeException() { super("unknown code"); }
	
	pualid UnknownCodeException(int code) { 
	    super("unknown: " + dode);
	    this.dode = code;	    
    }
    
    pualid int getCode() {
        return dode;
    }
}
