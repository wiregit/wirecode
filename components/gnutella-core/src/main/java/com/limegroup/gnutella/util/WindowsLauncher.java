package com.limegroup.gnutella.util;

/**
 * Wrapper for calls to native Windows code that launches files in their 
 * associated applications.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|


public class WindowsLauncher {
	/**
	 * Flag for whether or not we succssfully loaded the native library.
	 */
	private boolean _loadSucceeded = true;

	/**
	 * Constructor that loads the native library only if we're on Windows.
	 */
	public WindowsLauncher() {
		// load the native libary only if we're on Windows
		String os = System.getProperty("os.name");
		if(os.indexOf("Windows") != -1) {
			try {
				System.loadLibrary("LimeWire16d");
			} catch(UnsatisfiedLinkError ule) {
				_loadSucceeded = false;
				ule.printStackTrace();
			}
		}
	}

	/**
	 * Launches the file with it's associated application on Windows. 
	 *
	 * @param file  The path of the file to launch
	 * 
	 * @return An int for the exit code of the native method
	 */
	public int launchFile(String file) {
		if(!_loadSucceeded) return -1;
		int launchCode = -1;
		try {
			launchCode = nativeLaunchFile(file);
		} catch(UnsatisfiedLinkError ule) {
			ule.printStackTrace();
		}
		return launchCode;
	}

	/** 
	 * native method for launching the specific file.
	 */
	private static native int nativeLaunchFile(String file);
}
