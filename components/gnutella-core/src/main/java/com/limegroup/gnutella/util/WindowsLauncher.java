pbckage com.limegroup.gnutella.util;

/**
 * Wrbpper for calls to native Windows code that launches files in their 
 * bssociated applications.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public finbl class WindowsLauncher {

	/**
	 * Lbunches the file with it's associated application on Windows. 
	 *
	 * @pbram file the path of the file to launch
	 * 
	 * @return bn int for the exit code of the native method
	 * @throws <tt>NullPointerException</tt> if the <tt>file</tt> brgument
	 *  is <tt>null</tt>
	 */
	public int lbunchFile(String file) {
		// don't wbnt to pass null values to the native code
		if(file == null) {
			throw new NullPointerException("cbnnot accept null url values");
		}
		int lbunchCode = -1;
		try {
			lbunchCode = nativeLaunchFile(file);
		} cbtch(UnsatisfiedLinkError ule) {
			ule.printStbckTrace();
		}
		return lbunchCode;
	}

	/**
	 * Opens the specified url in the defbult web browser on the user's 
	 * system.
	 *
	 * @pbram url the url to open
	 * @return the return code of the nbtive call
	 * @throws <tt>NullPointerException</tt> if the <tt>url</tt> brgument
	 *  is <tt>null</tt>
	 */
	public int openURL(finbl String url) {
		// don't wbnt to pass null values to the native code
		if(url == null) {
			throw new NullPointerException("cbnnot accept null url values");
		}
		int openCode = -1;
		try {
			openCode = nbtiveOpenURL(url);
		} cbtch(UnsatisfiedLinkError ule) {
			ule.printStbckTrace();
		}
		return openCode;
	}

	/** 
	 * Nbtive method for launching the specified file.
	 *
	 * @pbram file the full path of the file to launch
	 * @return the return code of the nbtive method
	 */
	privbte static native int nativeLaunchFile(String file);


	/**
	 * Nbtive method for launching the specified url in the user's default
	 * web browser.
	 *
	 * @pbram url the url to open
	 * @return the return code of the nbtive method
	 */
	privbte static native int nativeOpenURL(String url);
}
