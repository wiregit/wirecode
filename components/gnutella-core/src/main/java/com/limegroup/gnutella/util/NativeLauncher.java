/**
 * auth: afisk
 * file: NativeLauncher.java
 * desc: Wrapper for calls to native code that launches
 *       files in their associated applications.
 */

package com.limegroup.gnutella.util;

public class NativeLauncher {
	public NativeLauncher() {
		// load the native libary
		System.loadLibrary("NativeLauncher");
	}

	/**
	 * @requires that we are running on Windows and
	 *  that the String specify a valid file pathname
	 *  or a valid URL
	 * @effects launches the file with it's associated
	 *  application on Windows. */
	public void launchFileWindows(String name) {
		nativeLaunchFileWindows(name);
	}

	// native method for launching the specific file 
	private native void nativeLaunchFileWindows(String name);
}
