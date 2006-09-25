package com.limegroup.gnutella.util;

/**
 * Wrapper for calls to native Windows code that launches files in their 
 * associated applications.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class WindowsLauncher {

	/**
	 * Launches the file with it's associated application on Windows. 
	 *
	 * @param file the path of the file to launch
	 * 
	 * @return an int for the exit code of the native method
	 * @throws <tt>NullPointerException</tt> if the <tt>file</tt> argument
	 *  is <tt>null</tt>
	 */
	public int launchFile(String file) {
		// don't want to pass null values to the native code
		if(file == null) {
			throw new NullPointerException("cannot accept null url values");
		}
		int launchCode = -1;
		try {
			launchCode = nativeLaunchFile(file);
		} catch(UnsatisfiedLinkError ule) {
			ule.printStackTrace();
		}
		return launchCode;
	}

	/**
	 * Opens the specified url in the default web browser on the user's 
	 * system.
	 *
	 * @param url the url to open
	 * @return the return code of the native call
	 * @throws <tt>NullPointerException</tt> if the <tt>url</tt> argument
	 *  is <tt>null</tt>
	 */
	public int openURL(final String url) {
		// don't want to pass null values to the native code
		if(url == null) {
			throw new NullPointerException("cannot accept null url values");
		}
		int openCode = -1;
		try {
			openCode = nativeOpenURL(url);
		} catch(UnsatisfiedLinkError ule) {
			ule.printStackTrace();
		}
		return openCode;
	}

	/** 
	 * Native method for launching the specified file.
	 *
	 * @param file the full path of the file to launch
	 * @return the return code of the native method
	 */
	private static native int nativeLaunchFile(String file);


	/**
	 * Native method for launching the specified url in the user's default
	 * web browser.
	 *
	 * @param url the url to open
	 * @return the return code of the native method
	 */
	private static native int nativeOpenURL(String url);
}
