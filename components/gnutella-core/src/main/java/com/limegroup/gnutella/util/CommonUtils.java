package com.limegroup.gnutella.util;

import java.util.Properties;
import java.io.*;

/**
 * This class handles common utility functions that many classes
 * may want to access.
 *
 * @author Adam Fisk
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|


public class CommonUtils {

	// constant for the current version of LimeWire
	private static final String LIMEWIRE_VERSION = "1.6d";
	
	// variable for the system properties
	private static Properties _props;

	// variable for whether or not we're on Windows
	private static boolean _isWindows    = false;
    // true if NT or 2000.
    private static boolean _supportsTray = false;

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
	static {
		// get the system properties object
		_props = System.getProperties();

		// get the operating system
		String os = System.getProperty("os.name");

		// set the operating system variables
		_isWindows = os.indexOf("Windows") != -1;
		//if (os.indexOf("Windows NT")!=-1 || os.indexOf("Windows 2000")!=-1)
		if(_isWindows) _supportsTray=true;
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
	 * returns the current version number of LimeWire as
     * a string, e.g., "1.4".
	 */
	public static String getLimeWireVersion() {
		return LIMEWIRE_VERSION;
	}

	/**
	 * Returns a version number appropriate for upload headers.
     * Same as '"LimeWire "+getLimeWireVersion'.
	 */
	public static String getVendor() {
		return "LimeWire " + LIMEWIRE_VERSION;
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
	 * returns the user's current working directory.
	 */
	public static String getCurrentDirectory() {
		return _props.getProperty("user.dir");
	}

	/**
	 * returns whether or not the os is some version of Windows
	 */
	public static boolean isWindows() {
		return _isWindows;
	}

    /**
     * Returns true iff this is Windows NT or Windows 2000 and
	 * hence can support a system tray feature.
     */
    public static boolean supportsTray() {
        return _supportsTray;
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

    /** Copies the file 'src' to 'dst', returning true iff the copy succeeded.
     *  If 'dst' already exists, the copy may or may not succeed. */
    public static boolean copy(File src, File dst) {
        boolean ok=true;
        InputStream in=null;
        OutputStream out=null;
        try {
            //I'm not sure whether buffering is needed here.  It can't hurt.
            in=new BufferedInputStream(new FileInputStream(src));
            out=new BufferedOutputStream(new FileOutputStream(dst));
            byte[] buf=new byte[1024];
            while (true) {
                int read=in.read(buf);
                if (read==-1)
                    break;
                out.write(buf, 0, read);
            }
        } catch (IOException e) {
            ok=false;
        } finally {
            if (in!=null)
                try { in.close(); } catch (IOException e) { ok=false; }
            if (out!=null) {
                try { out.flush(); } catch (IOException e) { ok=false; }
                try { out.close(); } catch (IOException e) { ok=false; }
            }
        }
        return ok;
    }
}
