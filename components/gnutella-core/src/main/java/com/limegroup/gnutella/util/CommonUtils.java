package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;
import java.util.Properties;
import java.io.*;
import java.net.*;

/**
 * This class handles common utility functions that many classes
 * may want to access.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class CommonUtils {

	/** 
	 * Constant for the current version of LimeWire.
	 */
	private static final String LIMEWIRE_VERSION = "2.0.5";
	
	/** 
	 * Constant for the java system properties.
	 */
	private static final Properties PROPS = System.getProperties();

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
	private static boolean _isMacOSX = false;

	/** 
	 * Variable for whether or not we're on Linux.
	 */
	private static boolean _isLinux = false;

	/** 
	 * Variable for whether or not we're on Solaris.
	 */
	private static boolean _isSolaris = false;

	/**
	 * Make sure the constructor can never be called.
	 */
	private CommonUtils() {}

	/**
	 * Initialize the settings statically. 
	 */
	static {
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
		return PROPS.getProperty("java.version");
	}

	/**
	 * Returns the operating system.
	 */
	public static String getOS() {
		return PROPS.getProperty("os.name");
	}

	/**
	 * Returns the user's current working directory.
	 */
	public static String getCurrentDirectory() {
		return PROPS.getProperty("user.dir");
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
		if(_isWindows98 || _isWindows95 || _isMacClassic) {
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
	 * @return <tt>true</tt> if the application is running on Mac OSX, 
	 *         <tt>false</tt> otherwise
	 */
	public static boolean isMacOSX() {
		return _isMacOSX;
	}

	/** 
	 * Returns whether or not the os is any Mac os.
	 *
	 * @return <tt>true</tt> if the application is running on Mac OSX
	 *  or any previous mac version, <tt>false</tt> otherwise
	 */
	public static boolean isAnyMac() {
		return _isMacClassic || _isMacOSX;
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
	 * Attempts to copy the first 'amount' bytes of file 'src' to 'dst',
	 * returning the number of bytes actually copied.  If 'dst' already exists,
	 * the copy may or may not succeed.
     * 
     * @param src the source file to copy
     * @param amount the amount of src to copy, in bytes
     * @param dst the place to copy the file
     * @return the number of bytes actually copied.  Returns 'amount' if the
     *  entire requested range was copied.
     */
    public static int copy(File src, int amount, File dst) {
        final int BUFFER_SIZE=1024;
        int amountToRead=amount;
        boolean ok=true;
        InputStream in=null;
        OutputStream out=null;
        try {
            //I'm not sure whether buffering is needed here.  It can't hurt.
            in=new BufferedInputStream(new FileInputStream(src));
            out=new BufferedOutputStream(new FileOutputStream(dst));
            byte[] buf=new byte[BUFFER_SIZE];
            while (amountToRead>0) {
                int read=in.read(buf, 0, Math.min(BUFFER_SIZE, amountToRead));
                if (read==-1)
                    break;
                amountToRead-=read;
                out.write(buf, 0, read);
            }
        } catch (IOException e) {
        } finally {
            if (in!=null)
                try { in.close(); } catch (IOException e) { }
            if (out!=null) {
                try { out.flush(); } catch (IOException e) { }
                try { out.close(); } catch (IOException e) { }
            }
        }
        return amount-amountToRead;
    }

    /** 
	 * Copies the file 'src' to 'dst', returning true iff the copy succeeded.
     * If 'dst' already exists, the copy may or may not succeed.  May also
     * fail for VERY large source files.
	 */
    public static boolean copy(File src, File dst) {
        //Downcasting length can result in a sign change, causing
        //copy(File,int,File) to terminate immediately.
        long length=src.length();
        return copy(src, (int)length, dst)==length;
    }

    /*
    public static void main(String args[]) {
        try {
            File src=new File("src.tmp");
            File dst=new File("dst.tmp");
            Assert.that(!src.exists() && !dst.exists(),
                        "Temp files already exists");
            
            write("abcdef", src);
            Assert.that(copy(src, dst)==true);
            Assert.that(equal(src, dst));

            write("zxcvbnmn", src);
            Assert.that(copy(src, 3, dst)==3);
            write("zxc", src);
            Assert.that(equal(src, dst));

        } catch (IOException e) {
            e.printStackTrace();
            Assert.that(false);
        } //  catch (InterruptedException e) {
//              e.printStackTrace();
//              Assert.that(false);
//          }
    }
    
    private static void write(String txt, File f) throws IOException {
        BufferedOutputStream bos=new BufferedOutputStream(
            new FileOutputStream(f));
        bos.write(txt.getBytes());   //who care about encoding?
        bos.flush();
        bos.close();
    }

    private static boolean equal(File f1, File f2) throws IOException {
        InputStream in1=new FileInputStream(f1);
        InputStream in2=new FileInputStream(f2);
        while (true) {
            int c1=in1.read();
            int c2=in2.read();
            if (c1!=c2)
                return false;
            if (c1==-1)
                break;
        }
        return true;
    }
    */
}
