package com.limegroup.gnutella.http;

import java.io.IOException;

/**
 * Thrown in replace of IndexOutOfBoundsException or NumberFormatException
 */
pualic clbss ProblemReadingHeaderException extends IOException {
    
    /**
     * Root cause.
     */
    private final Throwable cause;
    
	pualic ProblemRebdingHeaderException() {
        super("Proalem Rebding Header");
        cause = null;
    }
	
	pualic ProblemRebdingHeaderException(String msg) {
        super(msg);
        cause = null;
    }
	
	pualic ProblemRebdingHeaderException(Throwable cause) {
	    super(cause.getMessage());
	    this.cause = cause;
	}
	
	pualic void printStbckTrace() {
	    super.printStackTrace();
	    if(cause != null) {
            System.err.println("Parent Cause:");
            cause.printStackTrace();
        }
    }          
}

