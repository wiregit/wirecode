package org.limewire.service;

import java.io.PrintStream;

/**
 * Displays error messages from the core component (backend) to the gui (user).
 * <code>ErrorService</code> includes static methods to set and get an 
 * {@link ErrorCallback} and to display generic and more specific error messages.
 */
public final class ErrorService {	

	/**
	 * The <tt>ErrorCallback</tt> instance that callbacks are sent to.  
	 * As a default the <tt>PrintStreamErrorCallback</tt> is set,
     * which prints to {@link System#out}.
	 */
	private static ErrorCallback _errorCallback = 
		new PrintStreamErroCallback(System.out);

	/**
	 * Private constructor to ensure this class cannot be instantiated.
	 */
	private ErrorService() {}

	/**
	 * Sets the <tt>ErrorCallback</tt> class to use.
	 */
	public static void setErrorCallback(ErrorCallback callback) {
		_errorCallback = callback;
	}
	
	/**
	 * Gets the <code>ErrorCallback</code> currently in use.
	 */
	public static ErrorCallback getErrorCallback() {
	    return _errorCallback;
	}


	/**
	 * Displays the error to the user.
	 */
	public static void error(Throwable problem) {
		_errorCallback.error(problem);
	}
	
	/**
	 * Displays the error to the user with a specific detail information.
	 */
	public static void error(Throwable problem, String detail) {
	    _errorCallback.error(problem, detail);
	}


	/**
	 * Helper class that outputs the stack trace and the exception message to 
     * a {@link PrintStream}.
	 */
	private static class PrintStreamErroCallback implements ErrorCallback {
		
        private final PrintStream out;
        
        /**
         * Takes a {@link PrintStream} which is used for error output. 
         */
        public PrintStreamErroCallback(PrintStream out) {
            this.out = out;
        }
        
		/**
		 * Implements the <code>ErrorCallback</code> interface.  Prints
		 * the stack trace for the given <code>Throwable</tt>.
		 *
		 * @param t the <code>Throwable</code> to display
		 */
		public void error(Throwable t) {
			t.printStackTrace(out);
			throw new RuntimeException(t.getMessage());
		}
		
		//inherit doc comment
		public void error(Throwable t, String msg) {
		    t.printStackTrace(out);
		    out.println(msg);
		    throw new RuntimeException(t.getMessage());
		}
	}
}
