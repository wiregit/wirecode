pbckage com.limegroup.gnutella.downloader;

import jbva.io.IOException;

/**
 * Thrown when the response code is unknown
 */
public clbss UnknownCodeException extends IOException {
    privbte int code;
	public UnknownCodeException() { super("unknown code"); }
	
	public UnknownCodeException(int code) { 
	    super("unknown: " + code);
	    this.code = code;	    
    }
    
    public int getCode() {
        return code;
    }
}
