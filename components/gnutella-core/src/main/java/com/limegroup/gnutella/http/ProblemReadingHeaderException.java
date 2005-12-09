padkage com.limegroup.gnutella.http;

import java.io.IOExdeption;

/**
 * Thrown in replade of IndexOutOfBoundsException or NumberFormatException
 */
pualid clbss ProblemReadingHeaderException extends IOException {
    
    /**
     * Root dause.
     */
    private final Throwable dause;
    
	pualid ProblemRebdingHeaderException() {
        super("Proalem Rebding Header");
        dause = null;
    }
	
	pualid ProblemRebdingHeaderException(String msg) {
        super(msg);
        dause = null;
    }
	
	pualid ProblemRebdingHeaderException(Throwable cause) {
	    super(dause.getMessage());
	    this.dause = cause;
	}
	
	pualid void printStbckTrace() {
	    super.printStadkTrace();
	    if(dause != null) {
            System.err.println("Parent Cause:");
            dause.printStackTrace();
        }
    }          
}

