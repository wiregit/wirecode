package com.limegroup.gnutella.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.String;

import com.limegroup.gnutella.gui.Utilities;

/**
 * This code is Copyright 1999 by Eric Albert (ejalbert@cs.stanford.edu) and may 
 * be redistributed or modified in any form without restrictions as long as the 
 * portion of this comment from this paragraph through the end of the comment is 
 * not removed.  The author requests that he be notified of any application, 
 * applet, or other binary that makes use of this code, but that's more out of 
 * curiosity than anything and is not required.  This software includes no warranty.
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
public class Launcher {
	/** Caches whether any classes, methods, etc
	 *  are not part of the JDK and need to be dynamically 
	 *  loaded at runtime loaded successfully. <p>
	 *  Note that if this is <code>false</code>, 
	 *  <code>openURL()</code> will always return an
	 *  IOException. */
	private static boolean _macLoadedWithoutErrors;

	/** The com.apple.mrj.MRJFileUtils class */
	private static Class _mrjFileUtilsClass;

	/** The com.apple.mrj.MRJOSType class */
	private static Class _mrjOSTypeClass;
	
	/** The findFolder method of com.apple.mrj.MRJFileUtils */
	private static Method _findFolder;

	/** The getFileType method of com.apple.mrj.MRJOSType */
	private static Method _getFileType;
		
	/** Actually an MRJOSType pointing to the System Folder 
	 *  on a Macintosh */
	private static Object _kSystemFolderType;

	/** The file type of the Finder on a Macintosh.  
	 *  Hardcoding "Finder" would keep non-U.S. 
	 *  English systems from working properly. */
	private static final String FINDER_TYPE = "FNDR";

	/** The shell parameters for Netscape that opens a given URL in 
	 *  an already-open copy of Netscape on many command-line systems. */
	private static final String NETSCAPE_REMOTE_PARAMETER = "-remote";
	private static final String NETSCAPE_OPEN_PARAMETER_START = "openURL(";
	private static final String NETSCAPE_OPEN_PARAMETER_END = ")";
	
	/** The message from any exception thrown throughout 
	 *  the initialization process. */
	private static String _errorMessage;

	private static NativeLauncher _nativeLauncher;

	/** initialization block that determines the operating 
	 *  system and loads the necessary runtime data. */
	static {		
		// really only necessary for Windows right now, but...
		_nativeLauncher = new NativeLauncher();
		if(Utilities.isMac()) {
			_macLoadedWithoutErrors = loadMacClasses();		
		}
	}

	/** This class should be never be instantiated; this just ensures so. */
	private Launcher() {}
	
	/** 
	 * @requires the path String must either be a valid path name
	 *  for the operating system, or it must be a URL of the form
	 *  http://www.whatevername.com
	 * @effects launches the passed-in file on the current platform. */
	public static void launch(String path) throws IOException {
		if(Utilities.isWindows()) {
			launchFileWindows(path);
		}	   
		else if(Utilities.isMac()) {
			launchFileMac(path);
		}
		else if(Utilities.isUnix()) {
			launchFileUnix(path);
		}
	}

	/**
	 *  @requires that we are running on Windows
	 *  @effects launches the given file on Windows
	 */
	private static void launchFileWindows(String path) {
		_nativeLauncher.launchFileWindows(path);
	}

	/** 
	 *  @requires that we are running on a Mac
	 *  @effects launches the given file on the Mac
	 */
	private static void launchFileMac(String path) throws IOException {
		if(_macLoadedWithoutErrors) {
			try {
				Runtime.getRuntime().exec(new String[] {getMacFinder(),path});
			} catch(SecurityException se) {
			}
		}
	}
	/**
	 *  @requires that we are running on a Unix system
	 *  @effects attempts to launch the given file on Unix
	 *  NOTE: WE COULD DO THIS ONE BETTER!!
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
				Runtime.getRuntime().exec(new String[] {"netscape", 
														path});
			
		} catch (InterruptedException ie) {
			throw new IOException("InterruptedException launching browser: " 
								  + ie.getMessage());
		}
	}

	/**  
	 * @requires must be running on a mac 
	 * @effects returns the String specifying the "finder" on the mac.
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
	 *  @effects loads specialized classes for the Mac needed to launch files
	 *  @requires that we are running on a Mac
	 *  @return <code>true</code>  if initialization succeeded
	 *			<code>false</code> if initialization failed
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









