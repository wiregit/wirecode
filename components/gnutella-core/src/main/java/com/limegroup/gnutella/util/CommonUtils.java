/**
 * auth: afisk
 * file: CommonUtils.java
 * desc: This class handles common utility functions that many classes
 *       may want to access.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella.util;

import java.util.Properties;


public class CommonUtils {
	
	// variable for the system properties
	private static Properties _props;

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

		// get the system properties object
		_props = System.getProperties();

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
	 * returns the version of java we're using.
	 */
	public static String getJavaVersion() {
		return _props.getProperty("java.version");
	}

	/**
	 * returns the operating system
	 */
	public static String getOS() {
		return _props.getProperty("os.name");;
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

	/**
	 * This static method converts the passed in
	 * number of bytes into a kilobyte string 
	 * separated by commas and with "KB" at the
	 * end. 
	 */
	public static String toKilobytes(int bytes) {
		double d = (double)bytes/(double)1024;
		if(d < 1 && d > 0)
			d = 1;
		StringBuffer sb = new StringBuffer(Integer.toString((int)d));
		if(d > 999) {
			sb.insert(sb.length() - 3, ",");
  			if(d > 999999) {
  				sb.insert(sb.length() - 7, ",");
  			}
		}
		sb.append("KB");
		return sb.toString();
	}
	
}
