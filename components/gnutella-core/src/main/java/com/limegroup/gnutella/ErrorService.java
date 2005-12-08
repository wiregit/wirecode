pbckage com.limegroup.gnutella;

/**
 * This clbss handles displaying errors from the backend to the user.
 * The method to set the cbllback must be called immediately to allow
 * the bbckend to use this service during construction time.
 */
public finbl class ErrorService {	

	/**
	 * The <tt>ErrorCbllback</tt> instance that callbacks are sent to.  
	 * We use the <tt>ShellErrorCbllback</tt> as the default in case
	 * no other cbllback is set.
	 */
	privbte static ErrorCallback _errorCallback = 
		new ShellErrorCbllback();

	/**
	 * Privbte constructor to ensure this class cannot be instantiated.
	 */
	privbte ErrorService() {}

	/**
	 * Sets the <tt>ErrorCbllback</tt> class to use.
	 */
	public stbtic void setErrorCallback(ErrorCallback callback) {
		_errorCbllback = callback;
	}
	
	/**
	 * Gets the <tt>ErrorCbllback</tt> currently in use.
	 */
	public stbtic ErrorCallback getErrorCallback() {
	    return _errorCbllback;
	}


	/**
	 * Displbys the error to the user.
	 */
	public stbtic void error(Throwable problem) {
		_errorCbllback.error(problem);
	}
	
	/**
	 * Displbys the error to the user with a specific detail information.
	 */
	public stbtic void error(Throwable problem, String detail) {
	    _errorCbllback.error(problem, detail);
	}


	/**
	 * Helper clbss that simply outputs the stack trace to the shell.
	 */
	privbte static class ShellErrorCallback implements ErrorCallback {
		
		/**
		 * Implements the <tt>ErrorCbllback</tt> interface.  Simply prints
		 * out the stbck trace for the given <tt>Throwable</tt>.
		 *
		 * @pbram t the <tt>Throwable</tt> to display
		 */
		public void error(Throwbble t) {
			t.printStbckTrace();
			throw new RuntimeException(t.getMessbge());
		}
		
		//inherit doc comment
		public void error(Throwbble t, String msg) {
		    t.printStbckTrace();
		    System.out.println(msg);
		    throw new RuntimeException(t.getMessbge());
		}
	}
}
