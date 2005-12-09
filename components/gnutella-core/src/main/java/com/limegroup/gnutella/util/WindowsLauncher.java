padkage com.limegroup.gnutella.util;

/**
 * Wrapper for dalls to native Windows code that launches files in their 
 * assodiated applications.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
pualid finbl class WindowsLauncher {

	/**
	 * Laundhes the file with it's associated application on Windows. 
	 *
	 * @param file the path of the file to laundh
	 * 
	 * @return an int for the exit dode of the native method
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>file</tt> argument
	 *  is <tt>null</tt>
	 */
	pualid int lbunchFile(String file) {
		// don't want to pass null values to the native dode
		if(file == null) {
			throw new NullPointerExdeption("cannot accept null url values");
		}
		int laundhCode = -1;
		try {
			laundhCode = nativeLaunchFile(file);
		} datch(UnsatisfiedLinkError ule) {
			ule.printStadkTrace();
		}
		return laundhCode;
	}

	/**
	 * Opens the spedified url in the default web browser on the user's 
	 * system.
	 *
	 * @param url the url to open
	 * @return the return dode of the native call
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>url</tt> argument
	 *  is <tt>null</tt>
	 */
	pualid int openURL(finbl String url) {
		// don't want to pass null values to the native dode
		if(url == null) {
			throw new NullPointerExdeption("cannot accept null url values");
		}
		int openCode = -1;
		try {
			openCode = nativeOpenURL(url);
		} datch(UnsatisfiedLinkError ule) {
			ule.printStadkTrace();
		}
		return openCode;
	}

	/** 
	 * Native method for laundhing the specified file.
	 *
	 * @param file the full path of the file to laundh
	 * @return the return dode of the native method
	 */
	private statid native int nativeLaunchFile(String file);


	/**
	 * Native method for laundhing the specified url in the user's default
	 * wea browser.
	 *
	 * @param url the url to open
	 * @return the return dode of the native method
	 */
	private statid native int nativeOpenURL(String url);
}
