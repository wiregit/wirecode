

package com.limegroup.gnutella.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.String;

import com.limegroup.gnutella.gui.Utilities;

/**
 * This code is Copyright 1999 by Eric Albert (ejalbert@cs.stanford.edu) and may be redistributed
 * or modified in any form without restrictions as long as the portion of this comment from this
 * paragraph through the end of the comment is not removed.  The author requests that he be
 * notified of any application, applet, or other binary that makes use of this code, but that's
 * more out of curiosity than anything and is not required.  This software includes no warranty.
 * <p>
 * Credits:
 * <br>Steven Spencer, JavaWorld magazine (<a href="http://www.javaworld.com/javaworld/javatips/jw-javatip66.html">Java Tip 66</a>)
 * <br>Ron B. Yeh, Zero G Software
 * <br>Ben Engber, The New York Times
 * <br>Paul Teitlebaum and Andrea Cantatore, Datatech Software
 * <br>Larry Barowski, Auburn University
 *
 * @author Eric Albert (<a href="mailto:ejalbert@cs.stanford.edu">ejalbert@cs.stanford.edu</a>)
 * @version 1.3 (Released October 15, 1999)
 */
public class Launcher {

	/** The Java virtual machine that we are running on.  
	 *  Actually, in most cases we only care about the 
	 *  operating system, but some operating systems require 
	 *  us to switch on the VM. */
	private static int jvm;

	/** The browser for the system */
	private static Object browser;

	/** Caches whether any classes, methods, and fields that 
	 *  are not part of the JDK and need to be dynamically 
	 *  loaded at runtime loaded successfully. <p>
	 *  Note that if this is <code>false</code>, 
	 *  <code>openURL()</code> will always return an
	 *  IOException. */
	private static boolean loadedWithoutErrors;

	/** The com.apple.mrj.MRJFileUtils class */
	private static Class mrjFileUtilsClass;

	/** The com.apple.mrj.MRJOSType class */
	private static Class mrjOSTypeClass;

	/** The com.apple.MacOS.AEDesc class */
	private static Class aeDescClass;
	
	/** The <init>(int) method of com.apple.MacOS.AETarget */
	private static Constructor aeTargetConstructor;
	
	/** The <init>(int, int, int) method of com.apple.MacOS.AppleEvent */
	private static Constructor appleEventConstructor;
	
	/** The <init>(String) method of com.apple.MacOS.AEDesc */
	private static Constructor aeDescConstructor;
	
	/** The findFolder method of com.apple.mrj.MRJFileUtils */
	private static Method findFolder;

	/** The getFileType method of com.apple.mrj.MRJOSType */
	private static Method getFileType;
	
	/** The makeOSType method of com.apple.MacOS.OSUtils */
	private static Method makeOSType;
	
	/** The putParameter method of com.apple.MacOS.AppleEvent */
	private static Method putParameter;
	
	/** The sendNoReply method of com.apple.MacOS.AppleEvent */
	private static Method sendNoReply;
	
	/** Actually an MRJOSType pointing to the System Folder 
	 *  on a Macintosh */
	private static Object kSystemFolderType;

	/** The keyDirectObject AppleEvent parameter type */
	private static Integer keyDirectObject;

	/** The kAutoGenerateReturnID AppleEvent code */
	private static Integer kAutoGenerateReturnID;
	
	/** The kAnyTransactionID AppleEvent code */
	private static Integer kAnyTransactionID;
	
	/** JVM constant for MRJ 2.1 or later */
	private static final int MRJ_2_1 = 1;

	/** JVM constant for any Windows NT JVM */
	private static final int WINDOWS_NT = 2;
	
	/** JVM constant for any Windows 9x JVM */
	private static final int WINDOWS_9x = 3;

	/** JVM constant for any other platform */
	private static final int OTHER = -1;

	/** The file type of the Finder on a Macintosh.  
	 *  Hardcoding "Finder" would keep non-U.S. 
	 *  English systems from working properly. */
	private static final String FINDER_TYPE = "FNDR";

	/** The creator code of the Finder on a Macintosh, which is needed 
	 *  to send AppleEvents to the application. */
	private static final String FINDER_CREATOR = "MACS";

	/** The first parameter that needs to be passed into Runtime.exec() 
	 *  to open the default web browser on Windows. */
    private static final String FIRST_WINDOWS_PARAMETER = "/c";
    
    /** The second parameter for Runtime.exec() on Windows. */
    private static final String SECOND_WINDOWS_PARAMETER = "start";
	
    /** The third parameter for Runtime.exec() on Windows. */
	private static final String THIRD_WINDOWS_PARAMETER = "\"dummy\"";	

	/** The shell parameters for Netscape that opens a given URL in 
	 *  an already-open copy of Netscape on many command-line systems. */
	private static final String NETSCAPE_REMOTE_PARAMETER = "-remote";
	private static final String NETSCAPE_OPEN_PARAMETER_START = "openURL(";
	private static final String NETSCAPE_OPEN_PARAMETER_END = ")";
	
	/** The message from any exception thrown throughout 
	 *  the initialization process. */
	private static String errorMessage;

	/** An initialization block that determines the operating 
	 *  system and loads the necessary runtime data. */
	static {
		loadedWithoutErrors = true;
		String osName = System.getProperty("os.name");
		if ("Mac OS".equals(osName)) {
			String mrjVersion = System.getProperty("mrj.version");
			String majorMRJVersion = mrjVersion.substring(0, 3);
			try {
				double version = Double.valueOf(majorMRJVersion).doubleValue();
				if (version >= 2.1) {
					// Assume that all post-2.0 versions of MRJ work the same.  
					// MRJ 2.1 actually works via Runtime.exec() and 2.2 
					// supports that but has an openURL() method
					// as well that we currently ignore.
					jvm = MRJ_2_1;
				} else {
					loadedWithoutErrors = false;
					errorMessage = "Unsupported MRJ version: " + version;
				}
			} catch (NumberFormatException nfe) {
				loadedWithoutErrors = false;
				errorMessage = "Invalid MRJ version: " + mrjVersion;
			}
		} else if (osName.startsWith("Windows")) {
			if (osName.indexOf("9") != -1) {
				jvm = WINDOWS_9x;
			} else {
				jvm = WINDOWS_NT;
			}
		} else {
			jvm = OTHER;
		}
		
		if (loadedWithoutErrors) {	// if we haven't hit any errors yet
			loadedWithoutErrors = loadClasses();
		}
	}

	/** This class should be never be instantiated; this just ensures so. */
	private Launcher() { }
	
	/** Called by a static initializer to load any classes, fields, and methods required at runtime
	 *  to locate the user's web browser.
	 *  @return <code>true</code> if all intialization succeeded
	 *			<code>false</code> if any portion of the initialization failed
	 */
	private static boolean loadClasses() {
		switch (jvm) {
		case MRJ_2_1:
			try {
				mrjFileUtilsClass = Class.forName("com.apple.mrj.MRJFileUtils");
				mrjOSTypeClass = Class.forName("com.apple.mrj.MRJOSType");
				Field systemFolderField = mrjFileUtilsClass.getDeclaredField("kSystemFolderType");
				kSystemFolderType = systemFolderField.get(null);
				findFolder = mrjFileUtilsClass.getDeclaredMethod("findFolder", new Class[] { mrjOSTypeClass });
				getFileType = mrjFileUtilsClass.getDeclaredMethod("getFileType", new Class[] { File.class });
			} catch (ClassNotFoundException cnfe) {
				errorMessage = cnfe.getMessage();
				return false;
			} catch (NoSuchFieldException nsfe) {
				errorMessage = nsfe.getMessage();
				return false;
			} catch (NoSuchMethodException nsme) {
				errorMessage = nsme.getMessage();
				return false;
			} catch (SecurityException se) {
				errorMessage = se.getMessage();
				return false;
			} catch (IllegalAccessException iae) {
				errorMessage = iae.getMessage();
				return false;
			}
			break;
		}
		return true;
	}

	/** Attempts to locate the default web browser on the local system.  Caches results so it
	 *  only locates the browser once for each use of this class per JVM instance.
	 *  @return The browser for the system.  Note that this may not be what you would consider
	 *			to be a standard web browser; instead, it's the application that gets called to
	 *			open the default web browser.  In some cases, this will be a non-String object
	 *			that provides the means of calling the default browser.
	 */
	private static Object locateBrowser() {
		if (browser != null) {
			return browser;
		}
		switch (jvm) {
		case MRJ_2_1:
			File systemFolder;
			try {
				Object[] objects = {kSystemFolderType};
				systemFolder = (File)findFolder.invoke(null, objects);
			} catch (IllegalArgumentException iare) {
				browser = null;
				errorMessage = iare.getMessage();
				return browser;
			} catch (IllegalAccessException iae) {
				browser = null;
				errorMessage = iae.getMessage();
				return browser;
			} catch (InvocationTargetException ite) {
				browser = null;
				errorMessage = ite.getTargetException().getClass() + 
				": " + ite.getTargetException().getMessage();
				return browser;
			}
			String[] systemFolderFiles = systemFolder.list();
			// Avoid a FilenameFilter because that can't be stopped mid-list
			for(int i = 0; i < systemFolderFiles.length; i++) {
				try {
					File file = new File(systemFolder, systemFolderFiles[i]);
					if (!file.isFile()) {
						continue;
					}
					Object fileType = getFileType.invoke(null, new Object[] {file});
					if (FINDER_TYPE.equals(fileType.toString())) {
						// Actually the Finder, but that's OK
						browser = file.toString();	
						return browser;
					}
				} catch (IllegalArgumentException iare) {
					browser = browser;
					errorMessage = iare.getMessage();
					return null;
				} catch (IllegalAccessException iae) {
					browser = null;
					errorMessage = iae.getMessage();
					return browser;
				} catch (InvocationTargetException ite) {
					browser = null;
					errorMessage = ite.getTargetException().getClass() + 
					": " + ite.getTargetException().getMessage();
					return browser;
				}
			}
			browser = null;
			break;
		case WINDOWS_NT:
			browser = "cmd.exe";
			break;
		case WINDOWS_9x:
			browser = "command.com";
			break;
		case OTHER:
		default:
			browser = "netscape";
			break;
		}
		return browser;
	}

	public static void launch(String path) throws IOException {
		if (!loadedWithoutErrors) {
			throw new IOException("Exception in finding browser: " 
								  + errorMessage);
		}
		Object browser = locateBrowser();
		if (browser == null) {
			throw new IOException("Unable to locate browser: " 
								  + errorMessage);
		}
		String command;
		switch (jvm) {
		case MRJ_2_1:
			Runtime.getRuntime().exec(new String[] {(String)browser,path});
			break;
		case WINDOWS_NT: // this also gets called for Windows 2000
			File f = new File(path);
			if(f.isFile()) {
				FileInputStream fis = new FileInputStream(f);
				String name = f.getName();			
				String hex = Integer.toHexString(fis.read());
				hex += Integer.toHexString(fis.read());
				fis.close();							
				if(hex.equals("4d5a")) {
					Utilities.showError("LimeWire will not launch executable files "+
										"for security reasons.");
					return;
				}
				else {
					command = "cmd /c "+"\""+ checkChars(path)+"\"";				
					Runtime.getRuntime().exec(command);				
				}
			}
			break;
		case WINDOWS_9x:
			File fi = new File(path);
			if(fi.isFile()) {			
				FileInputStream fis = new FileInputStream(fi);
				String hex = Integer.toHexString(fis.read());
				hex += Integer.toHexString(fis.read());
				fis.close();
				if(hex.equals("4d5a")) {
					Utilities.showError("LimeWire will not launch executable files "+
										"for security reasons.");
					return;
				}
				command = "start "+"\""+ path+"\"";
				Runtime.getRuntime().exec(command);
			}
			break;

		case OTHER:
			// Assume that we're on Unix and that Netscape is installed
				
			// First, attempt to open the file in a 
			// currently running session of Netscape
			// NOT SURE THIS WILL WORK FOR NON-HTML FILES!!
			String[] strs = {(String)browser, 
							 NETSCAPE_REMOTE_PARAMETER,
							 NETSCAPE_OPEN_PARAMETER_START, 
							 path,
							 NETSCAPE_OPEN_PARAMETER_END};
			Process process = Runtime.getRuntime().exec(strs);
			try {
				int exitCode = process.waitFor();
				if (exitCode != 0) 	// if Netscape was not open
					Runtime.getRuntime().exec(new String[] {(String)browser, 
															path});
				
			} catch (InterruptedException ie) {
				throw new IOException("InterruptedException launching browser: " 
									  + ie.getMessage());
			}
			break;

		default:
			// This should never occur, but if it does, we'll try 
			// the simplest thing possible
			Runtime.getRuntime().exec(new String[] {(String) browser, 
													path});
			break;
		}
	}
	/** Attempts to open the default web browser to the given URL.
	 *  @param url The URL to open
	 *  @throws IOException If the web browser could not be 
	 *  located or does not run
	 */
	public static void openURL(String url) throws IOException {
		if (!loadedWithoutErrors) {
			throw new IOException("Exception in finding browser: " 
								  + errorMessage);
		}
		Object browser = locateBrowser();
		if (browser == null) {
			throw new IOException("Unable to locate browser: " + errorMessage);
		}
		switch (jvm) {
		case MRJ_2_1:
			Runtime.getRuntime().exec(new String[] {(String)browser, url});
			break;
		case WINDOWS_NT:			
			if (!url.startsWith("\""))
				url = "\"" + url;			
			if (!url.endsWith("\""))
				url += "\"";
			Runtime.getRuntime().exec(new String[] {(String) browser, 
													FIRST_WINDOWS_PARAMETER,
													SECOND_WINDOWS_PARAMETER,
													THIRD_WINDOWS_PARAMETER,
													url});
			break;

		case WINDOWS_9x:				
			if (!url.startsWith("\""))
				url = "\"" + url;				
			if (!url.endsWith("\""))
				url += "\"";
			Runtime.getRuntime().exec(new String[] {(String) browser, 
													FIRST_WINDOWS_PARAMETER,
													SECOND_WINDOWS_PARAMETER,
													url});
			break;

		case OTHER:
			// Assume that we're on Unix and that Netscape is installed
				
			// First, attempt to open the URL in a currently running session of Netscape
			Process process = Runtime.getRuntime().exec(new String[] {(String) browser,
																	  NETSCAPE_REMOTE_PARAMETER,
																	  NETSCAPE_OPEN_PARAMETER_START,
																	  url,
																	  NETSCAPE_OPEN_PARAMETER_END});
			try {
				int exitCode = process.waitFor();
				if (exitCode != 0) 	// if Netscape was not open
					Runtime.getRuntime().exec(new String[] {(String) browser, url});
				
			} catch (InterruptedException ie) {
				throw new IOException("InterruptedException while launching browser: " 
									  + ie.getMessage());
			}
			break;

		default:
			// This should never occur, but if it does, we'll try the simplest thing possible
			Runtime.getRuntime().exec(new String[] {(String) browser, url});
			break;
		}
	}

	/** This sets up the file name for nt to escape
	 *  special characters. */
    private static String checkChars(String str) {
        String escapeChars = "&()|<>^ ,;=";
        char[] chars = str.toCharArray();
        int length = chars.length;
        char[] new_chars = new char[length*3];
        int index = 0;
        for (int i=0; i < length; i++) {
            if (escapeChars.indexOf(chars[i]) != -1 ) {
                new_chars[index++] = '^';
                new_chars[index++] = chars[i];
                //new_chars[index++] = '"';
            }
            else 
                new_chars[index++] = chars[i];
        }
        String s = new String(new_chars);
        return s.trim();
    }
}
