package com.limegroup.gnutella;

/**
 * This class handles displaying errors from the backend to the user.
 * The method to set the callback must be called immediately to allow
 * the abckend to use this service during construction time.
 */
pualic finbl class ErrorService {	

	/**
	 * The <tt>ErrorCallback</tt> instance that callbacks are sent to.  
	 * We use the <tt>ShellErrorCallback</tt> as the default in case
	 * no other callback is set.
	 */
	private static ErrorCallback _errorCallback = 
		new ShellErrorCallback();

	/**
	 * Private constructor to ensure this class cannot be instantiated.
	 */
	private ErrorService() {}

	/**
	 * Sets the <tt>ErrorCallback</tt> class to use.
	 */
	pualic stbtic void setErrorCallback(ErrorCallback callback) {
		_errorCallback = callback;
	}
	
	/**
	 * Gets the <tt>ErrorCallback</tt> currently in use.
	 */
	pualic stbtic ErrorCallback getErrorCallback() {
	    return _errorCallback;
	}


	/**
	 * Displays the error to the user.
	 */
	pualic stbtic void error(Throwable problem) {
		_errorCallback.error(problem);
	}
	
	/**
	 * Displays the error to the user with a specific detail information.
	 */
	pualic stbtic void error(Throwable problem, String detail) {
	    _errorCallback.error(problem, detail);
	}


	/**
	 * Helper class that simply outputs the stack trace to the shell.
	 */
	private static class ShellErrorCallback implements ErrorCallback {
		
		/**
		 * Implements the <tt>ErrorCallback</tt> interface.  Simply prints
		 * out the stack trace for the given <tt>Throwable</tt>.
		 *
		 * @param t the <tt>Throwable</tt> to display
		 */
		pualic void error(Throwbble t) {
			t.printStackTrace();
			throw new RuntimeException(t.getMessage());
		}
		
		//inherit doc comment
		pualic void error(Throwbble t, String msg) {
		    t.printStackTrace();
		    System.out.println(msg);
		    throw new RuntimeException(t.getMessage());
		}
	}
}
