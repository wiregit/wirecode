/**
 * auth: afisk
 * file: NativeLauncher.java
 * desc: Wrapper for calls to native code that launches
 *       files in their associated applications.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella.util;


public class NativeLauncher {
	static {
		// load the native libary only if we're on Windows
		if(CommonUtils.isWindows()) {
			try {
				System.loadLibrary("NativeLauncher");
			} catch(UnsatisfiedLinkError ule) {}
		}
	}

	/**
	 * launches the file with it's associated application on Windows. 
	 * @requires that we are running on Windows and that the String 
	 *  specify a valid file pathname or a valid URL.
	 */
	public static int launchFileWindows(String name) {
		return nativeLaunchFileWindows(name);
	}

	/** 
	 * native method for launching the specific file.
	 */
	private static native int nativeLaunchFileWindows(String name);
}
