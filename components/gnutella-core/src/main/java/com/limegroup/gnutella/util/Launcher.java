package com.limegroup.gnutella.util;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.String;



/**
 * This class launches files in their associated applications and opens 
 * urls in the default browser for different operating systems.  This
 * really only works meaningfully for the Mac and Windows.<p>
 *
 * Acknowledgement goes to Eric Albert for demonstrating the general 
 * technique for loading the MRJ classes in his frequently-used
 * "BrowserLauncher" code.
 */
 //2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class Launcher {

	/**
	 * <tt>boolean</tt> specifying whether or not the necessary Mac
	 * classes were loaded successfully.
	 */
	private static boolean _macClassesLoadedSuccessfully = true;

	/** 
	 * The getFileCreator method of com.apple.mrj.MRJFileUtils 
	 */
	private static Method _getFileCreator;

	/**
	 * The openURL method of com.apple.mrj.MRJFileUtils.
	 */
	private static Method _openURL;

	/**
	 * The findApplication method of com.apple.mrj.MRJFileUtils for locating
	 * the applications associated with specific creator codes on the Mac.
	 */
	private static Method _findApplication;
		
	/** 
	 * The shell parameters for Netscape that opens a given URL in 
	 * an already-open copy of Netscape on many command-line systems. 
	 */
	private static final String NETSCAPE_REMOTE_PARAMETER = "-remote";
	private static final String NETSCAPE_OPEN_PARAMETER_START = "openURL(";
	private static final String NETSCAPE_OPEN_PARAMETER_END = ")";
	   
	/**
	 * Launcher class for opening applications on windows.
	 */
	private static WindowsLauncher _windowsLauncher;
   

	/** 
	 * Loads the necessary Mac classes if running on Mac.
	 */
	static {
		if(CommonUtils.isWindows()) {
			_windowsLauncher = new WindowsLauncher();
		}
		else if(CommonUtils.isMacClassic()) {
			try {
				loadMacClasses();		
			} catch(IOException ioe) {
				_macClassesLoadedSuccessfully = false;
			}
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
			return openURLWindows(url);
		}	   
		else if(CommonUtils.isMacClassic()) {
			openURLMacClassic(url);
		}
		else if(CommonUtils.isMacOSX()) {
			launchFileMacOSX(url);
		}
		else if(CommonUtils.isUnix()) {
			launchFileUnix(url);
		}
		return -1;
	}

	/**
	 * Opens the default web browser on windows, passing it the specified
	 * url.
	 *
	 * @param url the url to open in the browser
	 * @return the error code of the native call, -1 if the call failed
	 *  for any reason
	 */
	private static int openURLWindows(String url) throws IOException {
		return new WindowsLauncher().openURL(url);
	}
	
	/**
	 * Opens the specified url in the default browser on the Mac.
	 * This makes use of the dynamically-loaded MRJ classes.
	 *
	 * @param url the url to load
	 *
	 * @throws <tt>IOException</tt> if the necessary mac classes were not
	 *         loaded successfully or if another exception was
	 *         throws -- it wraps these exceptions in an <tt>IOException</tt>
	 */
	private static void openURLMacClassic(String url) throws IOException {
		if(!_macClassesLoadedSuccessfully) throw new IOException();
		try {
			Object[] params = new Object[] {url};
			_openURL.invoke(null, params);
		} 
		catch (NoSuchMethodError err) {
			throw new IOException();
			// this can occur when earlier versions of MRJ are used which
			// do not support the openURL method.
		} catch (NoClassDefFoundError err) {
			throw new IOException();
			// this can occur under runtime environments other than MRJ.
		} catch (IllegalAccessException iae) {
			throw new IOException();
		} catch (InvocationTargetException ite) {
			throw new IOException();
		}
	}

    private static void openURLOSX(String url) throws IOException {
    }
	/**
	 * Launches the file whose abstract path is specified in the 
	 * <tt>File</tt> parameter.  This method will not launch any file
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
				launchFileMacClassic(file);
			}
			else if(CommonUtils.isMacOSX()) {
				launchFileMacOSX(path);
			}
			else if(CommonUtils.isUnix()) {
				launchFileUnix(path);
			}
		}
		else {
			throw new SecurityException();
		}
		return -1;		
	}

	/**
	 * Launches the given file on Windows.
	 *
	 * @param path the path of the file to launch
	 *
	 * @return an int for the exit code of the native method
	 */
	private static int launchFileWindows(String path) throws IOException {		
		return new WindowsLauncher().launchFile(path);
	}

	/** 
	 * Launches the given file on a Mac with and OS between 8.5 and 9.1.
	 *
	 * @param file the <tt>File</tt> instance denoting the abstract pathname 
	 *            of the file to launch
	 *
	 * @throws IOException  if the call to Runtime.exec throws an IOException
	 */
	private static void launchFileMacClassic(final File file) throws IOException {
		if(!_macClassesLoadedSuccessfully) throw new IOException();
		File appFile    = getMacApplication(file);
		String appPath  = appFile.getCanonicalPath();
		String filePath = file.getCanonicalPath();	   
		try {
			Runtime.getRuntime().exec(new String[] {appPath, filePath});
		} catch(SecurityException se) {
		}
	}

	/**
	 * Launches a file on OSX, appending the full path of the file to the
	 * "open" command that opens files in their associated applications
	 * on OSX.
	 *
	 * @param file the <tt>File</tt> instance denoting the abstract pathname
	 *  of the file to launch
	 * @throws IOException if an I/O error occurs in making the runtime.exec()
	 *  call or in getting the canonical path of the file
	 */
	private static void launchFileMacOSX(final String file) throws IOException {
	    Runtime.getRuntime().exec(new String[]{"open", file});
	}

	/**
	 * Returns the path to the associated application on the Mac.
	 *
	 * @param file the abstract pathname of the file to launch
	 * @return the path to the application associated with the specified file
	 *         on the Mac, <tt>null</tt> if the application could not be 
	 *         found for some reason
	 */
	private static File getMacApplication(final File file) throws IOException {	
		try {
			Object fileType = _getFileCreator.invoke(null, new Object[] {file});
			Object appPath  = _findApplication.invoke(null, new Object[] {fileType});
			return (File)appPath;
		} catch(IllegalAccessException iae) {
			throw new IOException();
		} catch (InvocationTargetException ite) {
			throw new IOException();
		}
	}

	/** 
	 * Loads specialized classes for the Mac needed to launch files.
	 *
	 * @return <tt>true</tt>  if initialization succeeded,
	 *	   	   <tt>false</tt> if initialization failed
	 *
	 * @throws <tt>IOException</tt> if an exception occurs loading the
	 *         necessary classes
	 */
	private static void loadMacClasses() throws IOException {
		try {
			Class mrjFileUtilsClass = Class.forName("com.apple.mrj.MRJFileUtils");
			Class mrjOSType = Class.forName("com.apple.mrj.MRJOSType");

			String fcName = "getFileCreator";
			Class[] fcParams = {File.class};
			_getFileCreator = mrjFileUtilsClass.getDeclaredMethod(fcName, 
																  fcParams);	
			String openURLName = "openURL";
			Class[] openURLParams = {String.class};
			_openURL = mrjFileUtilsClass.getDeclaredMethod(openURLName, 
														   openURLParams);
			String faName = "findApplication";
			Class[] faParams = {mrjOSType};
			_findApplication  = mrjFileUtilsClass.getDeclaredMethod(faName, 
																	faParams);

		} catch (ClassNotFoundException cnfe) {
			throw new IOException();
		} catch (NoSuchMethodException nsme) {
			throw new IOException();
		} catch (SecurityException se) {
			throw new IOException();
		} 
	}


	/**
	 * Attempts to launch the given file on Unix.
	 * NOTE: WE COULD DO THIS ONE BETTER!!
	 *
	 * @throws IOException  if the call to Runtime.exec throws an IOException
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
	 * Methods required for Mac OS X.  The presence of native methods does not cause
	 * any problems on other platforms.
	 * 
	 * NOTE: These are taken directly from Eric Albert's "BrowserLauncher" class
	 */
	private native static int ICStart(int[] instance, int signature);
	private native static int ICStop(int[] instance);
	private native static int ICLaunchURL(int instance, byte[] hint, byte[] data, int len,
											int[] selectionStart, int[] selectionEnd);
}









