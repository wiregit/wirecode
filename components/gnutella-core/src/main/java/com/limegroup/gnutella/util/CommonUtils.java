package com.limegroup.gnutella.util;

public class CommonUtils {

	private static boolean _isWindows    = false;
	private static boolean _isMacClassic = false;
	private static boolean _isMacOSX     = false;
	private static boolean _isLinux      = false;
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
		String os = System.getProperty("os.name");

		// set the operating system variables
		_isWindows = os.indexOf("Windows") != -1;
		_isSolaris = os.indexOf("Solaris") != -1;
		_isLinux   = os.indexOf("Linux")   != -1;
		if(os.startsWith("Mac OS")) {
			if(os.endsWith("X")) {
				_isMacOSX = true;
			} else {
				_isMacClassic = true;
			}			
		}
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
