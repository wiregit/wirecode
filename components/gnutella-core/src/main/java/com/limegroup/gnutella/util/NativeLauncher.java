/**
 * Wrapper for calls to native code that launches files in their 
 * associated applications.
 *
 * @author Adam Fisk
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

//package com.limegroup.gnutella.util;

//import com.limegroup.gnutella.util.CommonUtils;


public class NativeLauncher {

	public NativeLauncher() {
		// load the native libary only if we're on Windows
		String os = System.getProperty("os.name");
		if(os.indexOf("Windows") != -1) {
			try {
				System.loadLibrary("NativeLauncher");
			} catch(UnsatisfiedLinkError ule) {}
		}
	}

//  	static {
//  		// load the native libary only if we're on Windows
//  		String os = System.getProperty("os.name");
//  		if(os.indexOf("Windows") != -1) {
//  			try {
//  				System.loadLibrary("NativeLauncher");
//  			} catch(UnsatisfiedLinkError ule) {}
//  		}
//  	}

	/**
	 * launches the file with it's associated application on Windows. 
	 * @requires that we are running on Windows and that the String 
	 *  specify a valid file pathname or a valid URL.
	 */
	public int launchFileWindows(String name) {
		int launchCode = -1;
		try {
			launchCode = nativeLaunchFileWindows(name);
		} catch(UnsatisfiedLinkError ule) {}
		return launchCode;
	}

	/** 
	 * native method for launching the specific file.
	 */
	private static native int nativeLaunchFileWindows(String name);
}
