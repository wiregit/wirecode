package com.limegroup.gnutella;

/**
 * This class handles displaying errors from the backend to the user.
 * The method to set the callback must be called immediately to allow
 * the backend to use this service during construction time.
 */
public final class ErrorService {	

	/**
	 * The <tt>ErrorCallback</tt> instance that callbacks are sent to.
	 */
	private static ErrorCallback _errorCallback;

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
	 * Displays the error to the user.
	 */
	public static void error(Throwable problem) {
		_errorCallback.error(problem);
	}
}
