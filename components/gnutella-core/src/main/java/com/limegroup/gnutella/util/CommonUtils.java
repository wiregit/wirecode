package com.limegroup.gnutella.util;

import java.util.Properties;
import java.io.*;
import java.net.*;

/**
 * This class handles common utility functions that many classes
 * may want to access.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public class CommonUtils {

	/** 
	 * Constant for the current version of LimeWire.
	 */
	private static final String LIMEWIRE_VERSION = "Supernode Alpha";
	
	/** 
	 * Variable for the java system properties.
	 */
	private static Properties _props;

	/** 
	 * Variable for whether or not we're on Windows.
	 */
	private static boolean _isWindows = false;

	/** 
	 * Variable for whether or not we're on Windows NT or 2000.
	 */
	private static boolean _isWindowsNTor2000 = false;

	/** 
	 * Variable for whether or not we're on Windows 95.
	 */
	private static boolean _isWindows95 = false;

	/** 
	 * Variable for whether or not we're on Windows 98.
	 */
	private static boolean _isWindows98 = false;

    /** 
	 * Variable for whether or not the operating system allows the 
	 * application to be reduced to the system tray.
	 */
    private static boolean _supportsTray = false;

	/**
	 * Variable for whether or not we're on Mac 9.1 or below.
	 */
	private static boolean _isMacClassic = false;

	/** 
	 * Variable for whether or not we're on MacOSX.
	 */
	private static boolean _isMacOSX     = false;

	/** 
	 * Variable for whether or not we're on Linux.
	 */
	private static boolean _isLinux      = false;

	/** 
	 * Variable for whether or not we're on Solaris.
	 */
	private static boolean _isSolaris    = false;

	/**
	 * Variable for whether or not the localhost is running on a private
	 * ip address.
	 */
	private static boolean _isPrivateAddress = true;
	
	/**
	 * Make sure the constructor can never be called.
	 */
	private CommonUtils() {}

	/**
	 * Initialize the settings statically. 
	 */
	static {
		// get the system properties object
		_props = System.getProperties();

		// get the operating system
		String os = System.getProperty("os.name");

		// set the operating system variables
		_isWindows = os.indexOf("Windows") != -1;
		if (os.indexOf("Windows NT") != -1 || os.indexOf("Windows 2000")!=-1)
			_isWindowsNTor2000 = true;
		if(os.indexOf("Windows 95") != -1)
		   _isWindows95 = true;
		if(os.indexOf("Windows 98") != -1)
		   _isWindows98 = true;
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
		
		// determine whether or not the local host is a private ip address
		byte[] bytes = null;
		try {
			bytes = InetAddress.getLocalHost().getAddress();
		} catch(UnknownHostException uhe) {
			_isPrivateAddress = true;
		}

		// 10.0.0.0 - 10.255.255.255
        if (bytes[0]==(byte)10)
            _isPrivateAddress = true;

		// 172.16.0.0 - 172.31.255.255
        else if (bytes[0]==(byte)172 &&
                 bytes[1]>=(byte)16 &&
                 bytes[1]<=(byte)31)
            _isPrivateAddress = true;

		// 192.168.0.0 - 192.168.255.255   
        else if (bytes[0]==(byte)192 &&
                 bytes[1]==(byte)168)
            _isPrivateAddress = true; 

		// 0.0.0.0 - Gnutella (well BearShare really) convention
        else if (bytes[0]==(byte)0 &&
                 bytes[1]==(byte)0 &&
                 bytes[2]==(byte)0 &&
                 bytes[3]==(byte)0)
            _isPrivateAddress = true;
		
		// otherwise, we're not firewalled
        else
			_isPrivateAddress = false;
	}

	/**
	 * Returns the current version number of LimeWire as
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
	 * Returns the version of java we're using.
	 */
	public static String getJavaVersion() {
		return _props.getProperty("java.version");
	}

	/**
	 * Returns the operating system.
	 */
	public static String getOS() {
		return _props.getProperty("os.name");;
	}

	/**
	 * Returns the user's current working directory.
	 */
	public static String getCurrentDirectory() {
		return _props.getProperty("user.dir");
	}

    /**
     * Returns true if this is Windows NT or Windows 2000 and
	 * hence can support a system tray feature.
     */
	public static boolean supportsTray() {
		return _supportsTray;
	}
		
	/**
	 * Returns whether or not this operating system is considered
	 * capable of meeting the requirements of a supernode.
	 *
	 * @return <tt>true</tt> if this os meets supernode requirements,
	 *         <tt>false</tt> otherwise
	 */
	public static boolean isSupernodeOS() {
		if(!_isWindows98 && !_isWindows95 && !_isMacClassic) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the os is some version of Windows.
	 *
	 * @return <tt>true</tt> if the application is running on some Windows 
	 *         version, <tt>false</tt> otherwise
	 */
	public static boolean isWindows() {
		return _isWindows;
	}

	/**
	 * Returns whether or not the os is some version of Windows.
	 *
	 * @return <tt>true</tt> if the application is running on Windows NT 
	 *         or 2000, <tt>false</tt> otherwise
	 */
	public static boolean isWindowsNTor2000() {
		return _isWindowsNTor2000;
	}

	/** 
	 * Returns whether or not the os is Mac 9.1 or earlier.
	 *
	 * @return <tt>true</tt> if the application is running on a Mac version
	 *         prior to OSX, <tt>false</tt> otherwise
	 */
	public static boolean isMacClassic() {
		return _isMacClassic;
	}

	/** 
	 * Returns whether or not the os is Mac OSX.
	 *
	 * @return <tt>true</tt> if the application is running on a Mac OSX, 
	 *         <tt>false</tt> otherwise
	 */
	public static boolean isMacOSX() {
		return _isMacOSX;
	}

	/** 
	 * Returns whether or not the os is Solaris.
	 *
	 * @return <tt>true</tt> if the application is running on Solaris, 
	 *         <tt>false</tt> otherwise
	 */
	public static boolean isSolaris() {
		return _isSolaris;
	}

	/** 
	 * Returns whether or not the os is Linux.
	 *
	 * @return <tt>true</tt> if the application is running on Linux, 
	 *         <tt>false</tt> otherwise
	 */
	public static boolean isLinux() {
		return _isLinux;
	}

	/** 
	 * Returns whether or not the os is some version of
	 * Unix, defined here as only Solaris or Linux.
	 */
	public static boolean isUnix() {
		return _isLinux || _isSolaris; 
	}   

    /** 
	 * Copies the file 'src' to 'dst', returning true iff the copy succeeded.
     * If 'dst' already exists, the copy may or may not succeed. 
	 */
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

    /**
     * Returns true if this is a private IP address as defined by
     * RFC 1918.  In the case that this has a symbolic name that
     * cannot be resolved, returns true.
	 *
	 * @return <tt>true</tt> if the localhost has a private ip address,
	 *         <tt>false</tt> otherwise
     */
    public static boolean isPrivateAddress() {
		return _isPrivateAddress;
    }

}
