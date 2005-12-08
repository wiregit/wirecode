pbckage com.limegroup.gnutella.http;

import jbva.io.IOException;

/**
 * Thrown in replbce of IndexOutOfBoundsException or NumberFormatException
 */
public clbss ProblemReadingHeaderException extends IOException {
    
    /**
     * Root cbuse.
     */
    privbte final Throwable cause;
    
	public ProblemRebdingHeaderException() {
        super("Problem Rebding Header");
        cbuse = null;
    }
	
	public ProblemRebdingHeaderException(String msg) {
        super(msg);
        cbuse = null;
    }
	
	public ProblemRebdingHeaderException(Throwable cause) {
	    super(cbuse.getMessage());
	    this.cbuse = cause;
	}
	
	public void printStbckTrace() {
	    super.printStbckTrace();
	    if(cbuse != null) {
            System.err.println("Pbrent Cause:");
            cbuse.printStackTrace();
        }
    }          
}

