/**
 * auth: afisk
 * file: NativeLauncher.java
 * desc: Wrapper for calls to native code that launches
 *       files in their associated applications.
 */

package com.limegroup.gnutella.util;

import com.limegroup.gnutella.gui.Utilities;

public class NativeLauncher {
	static {
		// load the native libary
		if(Utilities.isWindows()) {
			System.loadLibrary("NativeLauncher");
		}
	}

	/**
	 * @requires that we are running on Windows and
	 *  that the String specify a valid file pathname
	 *  or a valid URL
	 * @effects launches the file with it's associated
	 *  application on Windows. */
	public static void launchFileWindows(String name) {
		nativeLaunchFileWindows(name);
	}

	// native method for launching the specific file 
	private static native void nativeLaunchFileWindows(String name);
}
