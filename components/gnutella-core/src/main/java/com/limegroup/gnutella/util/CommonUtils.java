package com.limegroup.gnutella.util;

/**
 * auth: afisk
 * file: CommonUtils.java
 * desc: This class handles common utility functions that many classes
 *       may want to access.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

public class CommonUtils {
	
	// variable for the operating system
	private static String _os;

	// variable for whether or not we're on Windows
	private static boolean _isWindows    = false;

	// variable for whether or not we're on Mac 9.1 or below
	private static boolean _isMacClassic = false;

	// variable for whether or not we're on MacOSX
	private static boolean _isMacOSX     = false;

	// variable for whether or not we're on Linux
	private static boolean _isLinux      = false;

	// variable for whether or not we're on Solaris
	private static boolean _isSolaris    = false;
	
	/**
	 * make sure the constructor can never be called.
	 */
	private CommonUtils() {}

	/**
	 * initialize the settings statically.
	 */
	public static void initialize() {
		// get the operating system
		_os = System.getProperty("os.name");

		// set the operating system variables
		_isWindows = _os.indexOf("Windows") != -1;
		_isSolaris = _os.indexOf("Solaris") != -1;
		_isLinux   = _os.indexOf("Linux")   != -1;
		if(_os.startsWith("Mac OS")) {
			if(_os.endsWith("X")) {
				_isMacOSX = true;
			} else {
				_isMacClassic = true;
			}			
		}
	}

	/**
	 * returns the operating system
	 */
	public static String getOS() {
		return _os;
	}

	/**
	 * returns whether or not the os is some version of Windows
	 */
	public static boolean isWindows() {
		return _isWindows;
	}

	/** 
	 * returns whether or not the os is Mac 9.1 or earlier.
	 */
	public static boolean isMacClassic() {
		return _isMacClassic;
	}

	/** 
	 * returns whether or not the os is Mac OSX
	 */
	public static boolean isMacOSX() {
		return _isMacOSX;
	}

	/** 
	 * returns whether or not the os is Solaris
	 */
	public static boolean isSolaris() {
		return _isSolaris;
	}

	/** 
	 * returns whether or not the os is Linux
	 */
	public static boolean isLinux() {
		return _isLinux;
	}

	/** 
	 * returns whether or not the os is some version of
	 * Unix, defined here as only Solaris or Linux
	 */
	public static boolean isUnix() {
		return _isLinux || _isSolaris; 
	}
	
}
