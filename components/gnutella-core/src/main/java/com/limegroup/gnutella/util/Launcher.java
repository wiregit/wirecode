package com.limegroup.gnutella.util;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.String;



/**
 * This code is Copyright 1999 by Eric Albert (ejalbert@cs.stanford.edu) and 
 * may be redistributed or modified in any form without restrictions as long 
 * as the portion of this comment from this paragraph through the end of the 
 * comment is not removed.  The author requests that he be notified of any 
 * application, applet, or other binary that makes use of this code, but that's 
 * more out of curiosity than anything and is not required.  This software 
 * includes no warranty.
 * <p>
 * Credits:
 * <br>Steven Spencer, JavaWorld magazine 
 * (<a href="http://www.javaworld.com/javaworld/javatips/jw-javatip66.html">Java Tip 66</a>)
 * <br>Ron B. Yeh, Zero G Software
 * <br>Ben Engber, The New York Times
 * <br>Paul Teitlebaum and Andrea Cantatore, Datatech Software
 * <br>Larry Barowski, Auburn University
 *
 * @author Eric Albert, Adam Fisk
 */
 //2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public class Launcher {
	/** 
	 * Caches whether any classes, methods, etc
	 * are not part of the JDK and need to be dynamically 
	 * loaded at runtime loaded successfully. <p>
	 * Note that if this is <code>false</code>, 
	 * <code>openURL()</code> will always return an
	 * IOException. 
	 */
	private static boolean _macLoadedWithoutErrors;

	/** 
	 * The com.apple.mrj.MRJFileUtils class 
	 */
	private static Class _mrjFileUtilsClass;

	/** 
	 * The com.apple.mrj.MRJOSType class 
	 */
	private static Class _mrjOSTypeClass;
	
	/** 
	 * The findFolder method of com.apple.mrj.MRJFileUtils 
	 */
	private static Method _findFolder;

	/** 
	 * The getFileType method of com.apple.mrj.MRJOSType 
	 */
	private static Method _getFileType;
		
	/** 
	 * Actually an MRJOSType pointing to the System Folder 
	 * on a Macintosh 
	 */
	private static Object _kSystemFolderType;

	/** 
	 * The file type of the Finder on a Macintosh.  
	 * Hardcoding "Finder" would keep non-U.S. 
	 * English systems from working properly. 
	 */
	private static final String FINDER_TYPE = "FNDR";

	/** 
	 * The shell parameters for Netscape that opens a given URL in 
	 * an already-open copy of Netscape on many command-line systems. 
	 */
	private static final String NETSCAPE_REMOTE_PARAMETER = "-remote";
	private static final String NETSCAPE_OPEN_PARAMETER_START = "openURL(";
	private static final String NETSCAPE_OPEN_PARAMETER_END = ")";
	
	/** 
	 * The message from any exception thrown throughout 
	 * the initialization process. 
	 */
	private static String _errorMessage;


	/** 
	 * Loads the necessary Mac classes if running on Mac.
	 */
	static {
		if(CommonUtils.isMacClassic()) {
			_macLoadedWithoutErrors = loadMacClasses();		
		}
	}

	/** 
	 * This class should be never be instantiated; this just ensures so. 
	 */
	private Launcher() {}
	
	/**
	 * Opens the specified url in a browser. 
	 *
	 * <p>A browser will only be opened if the underlying operating system 
	 * recognizes the url as one that should be opened in a browser, 
	 * namely a url that ends in .htm or .html.
	 *
	 * @param url  The url to open
	 *
	 * @return  An int indicating the success of the browser launch
	 *
	 * @throws IOException if the url cannot be loaded do to an IO problem
	 */
	public static int openURL(String url) throws IOException {	   
		if(CommonUtils.isWindows()) {
			return launchFileWindows(url);
		}	   
		else if(CommonUtils.isMacClassic()) {
			launchFileMacClassic(url);
		}
		else if(CommonUtils.isUnix()) {
			launchFileUnix(url);
		}
		return -1;
	}

	/**
	 * Launches the file whose abstract path is specified in the 
	 * <code>File</code> parameter.  This method will not launch any file
	 * with .exe, .vbs, .lnk, .bat, .sys, or .com extensions, diplaying 
	 * an error if one of the file is of one of these types.
	 *
	 * @param path  The path of the file to launch
	 *
	 * @return  An int indicating the success of the browser launch
	 *
	 * @throws IOException if the file cannot be launched do to an IO problem
	 */
	public static int launchFile(File file) throws IOException,SecurityException {
		String path = file.getCanonicalPath();
		String extCheckString = path.toLowerCase();
		if(!extCheckString.endsWith(".exe") &&
		   !extCheckString.endsWith(".vbs") &&
		   !extCheckString.endsWith(".lnk") &&
		   !extCheckString.endsWith(".bat") &&
		   !extCheckString.endsWith(".sys") &&
		   !extCheckString.endsWith(".com")) {
			if(CommonUtils.isWindows()) {
				return launchFileWindows(path);
			}	   
			else if(CommonUtils.isMacClassic()) {
				launchFileMacClassic(path);
			}
			else if(CommonUtils.isUnix()) {
				launchFileUnix(path);
			}
		}
		else {
			String msg = "LimeWire will not launch the specified "+
			"file for security reasons.";
			throw new SecurityException(msg);
		}
		return -1;		
	}

	/**
	 * Launches the given file on Windows.
	 *
	 * @param path The path of the file to launch
	 *
	 * @return An int for the exit code of the native method
	 */
	private static int launchFileWindows(String path) {		
		WindowsLauncher wl = new WindowsLauncher();
		return wl.launchFile(path);
	}

	/** 
	 * Launches the given file on a Mac with and OS between 8.5 and 9.1.
	 *
	 * @param path The path of the file to launch
	 *
	 * @throws IOException  If the call to Runtime.exec throws an IOException
	 */
	private static void launchFileMacClassic(String path) throws IOException {
		if(_macLoadedWithoutErrors) {
			try {
				Runtime.getRuntime().exec(new String[] {getMacFinder(),path});
			} catch(SecurityException se) {
			}
		}
	}

	/**
	 * Attempts to launch the given file on Unix.
	 * NOTE: WE COULD DO THIS ONE BETTER!!
	 *
	 * @throws IOException  If the call to Runtime.exec throws an IOException
	 *                      or if the Process created by the Runtime.exec call
	 *                      throws an InterruptedException
	 */
	private static void launchFileUnix(String path) throws IOException {
		// First, attempt to open the file in a 
		// currently running session of Netscape
		// NOT SURE THIS WILL WORK FOR NON-HTML FILES!!
		String[] strs = {"netscape", 
						 NETSCAPE_REMOTE_PARAMETER,
						 NETSCAPE_OPEN_PARAMETER_START, 
						 path,
						 NETSCAPE_OPEN_PARAMETER_END};
		Process process = Runtime.getRuntime().exec(strs);
		try {
			int exitCode = process.waitFor();
			if (exitCode != 0) 	// if Netscape was not open
				Runtime.getRuntime().exec(new String[] {"netscape", path});
			
		} catch (InterruptedException ie) {
			throw new IOException("InterruptedException launching browser: " 
								  + ie.getMessage());
		}
	}

	/**  
	 * Returns the String specifying the "finder" on the mac.  This should
	 * only be called if the application is running on Mac OS 9.1 or below.
	 *
	 * @return A <code>String</code> instance specifying the path of the Mac
	 *         finder
	 */
	private static String getMacFinder() {
		File systemFolder;
		try {
			Object[] objects = {_kSystemFolderType};
			systemFolder = (File)_findFolder.invoke(null, objects);
		} catch (IllegalArgumentException iare) {
			_errorMessage = iare.getMessage();
			return null;
		} catch (IllegalAccessException iae) {
			_errorMessage = iae.getMessage();
			return null;
		} catch (InvocationTargetException ite) {
			_errorMessage = ite.getTargetException().getClass() + 
			": " + ite.getTargetException().getMessage();
			return null;
		}
		String[] systemFolderFiles = systemFolder.list();
		// Avoid a FilenameFilter because that can't be stopped mid-list
		for(int i=0; i < systemFolderFiles.length; i++) {
			try {
				File file = new File(systemFolder, systemFolderFiles[i]);
				if (!file.isFile()) {
					continue;
				}
				Object fileType = _getFileType.invoke(null, new Object[] {file});
				if (FINDER_TYPE.equals(fileType.toString())) {
					// Actually the Finder, but that's OK
					return file.toString();
				}
			} catch (IllegalArgumentException iare) {
				_errorMessage = iare.getMessage();
				return null;
			} catch (IllegalAccessException iae) {
				_errorMessage = iae.getMessage();
				return null;
			} catch (InvocationTargetException ite) {
				_errorMessage = ite.getTargetException().getClass() + 
				": " + ite.getTargetException().getMessage();
				return null;
			}
		}
		return null;	   
	}

	/** 
	 * Loads specialized classes for the Mac needed to launch files.
	 *
	 * @return <code>true</code>  if initialization succeeded,
	 *	   	   <code>false</code> if initialization failed
	 */
	private static boolean loadMacClasses() {
		try {
			_mrjFileUtilsClass = Class.forName("com.apple.mrj.MRJFileUtils");
			_mrjOSTypeClass = Class.forName("com.apple.mrj.MRJOSType");
			Field systemFolderField = _mrjFileUtilsClass.getDeclaredField
			    ("kSystemFolderType");
			_kSystemFolderType = systemFolderField.get(null);
			_findFolder  = _mrjFileUtilsClass.getDeclaredMethod
			    ("findFolder", new Class[] { _mrjOSTypeClass });
			_getFileType = _mrjFileUtilsClass.getDeclaredMethod
			    ("getFileType", new Class[] { File.class });
		} catch (ClassNotFoundException cnfe) {
			_errorMessage = cnfe.getMessage();
			return false;
		} catch (NoSuchFieldException nsfe) {
			_errorMessage = nsfe.getMessage();
			return false;
		} catch (NoSuchMethodException nsme) {
			_errorMessage = nsme.getMessage();
			return false;
		} catch (SecurityException se) {
			_errorMessage = se.getMessage();
			return false;
		} catch (IllegalAccessException iae) {
			_errorMessage = iae.getMessage();
			return false;
		}

		return true;
	}
}









