package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;
import java.util.Properties;
import java.io.*;
import java.net.*;
import com.apple.mrj.*;

/**
 * This class handles common utility functions that many classes
 * may want to access.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class CommonUtils {

	/** 
	 * Constant for the current version of LimeWire.
	 */
	private static final String LIMEWIRE_VERSION = "@version@";
	
	/** 
	 * Constant for the java system properties.
	 */
	private static final Properties PROPS = System.getProperties();

	/** 
	 * Variable for whether or not we're on Windows.
	 */
	private static boolean _isWindows = false;

	/** 
	 * Variable for whether or not we're on Windows NT, 2000, or XP.
	 */
	private static boolean _isWindowsNTor2000orXP = false;

	/** 
	 * Variable for whether or not we're on Windows 95.
	 */
	private static boolean _isWindows95 = false;

	/** 
	 * Variable for whether or not we're on Windows 98.
	 */
	private static boolean _isWindows98 = false;

	/** 
	 * Variable for whether or not we're on Windows Me.
	 */
	private static boolean _isWindowsMe = false;

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
	 * Variable for whether or not the JVM is 1.1.8.
	 */
	private static boolean _isJava118 = false;

	/**
	 * Cached constant for the HTTP Server: header value.
	 */
	private static final String HTTP_SERVER;

	/**
	 * Make sure the constructor can never be called.
	 */
	private CommonUtils() {}

	/**
	 * Initialize the settings statically. 
	 */
	static {
		// get the operating system
		String os = System.getProperty("os.name").toLowerCase();

		// set the operating system variables
		_isWindows = os.indexOf("windows") != -1;
		if (os.indexOf("windows nt") != -1 || 
			os.indexOf("windows 2000")!= -1 ||
			os.indexOf("windows xp")!= -1)
			_isWindowsNTor2000orXP = true;
		if(os.indexOf("windows 95") != -1)
		   _isWindows95 = true;
		if(os.indexOf("windows 98") != -1)
		   _isWindows98 = true;
		if(os.indexOf("windows me") != -1)
		   _isWindowsMe = true;
		if(_isWindows) _supportsTray=true;
		_isSolaris = os.indexOf("solaris") != -1;
		_isLinux   = os.indexOf("linux")   != -1;
		if(os.startsWith("mac os")) {
			if(os.endsWith("x")) {
				_isMacOSX = true;
			} else {
				_isMacClassic = true;
			}			
		}
		
		if(CommonUtils.getJavaVersion().startsWith("1.1.8")) {
			_isJava118 = true;
		} 
		
		if(!LIMEWIRE_VERSION.endsWith("Pro")) {
			HTTP_SERVER = "LimeWire/" + LIMEWIRE_VERSION;
		}
		else {
			HTTP_SERVER = ("LimeWire/" + 
						   LIMEWIRE_VERSION.substring(0, LIMEWIRE_VERSION.length()-4) +
						   " (Pro)");
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
	 * Returns the string for the server that should be reported in the HTTP
	 * "Server: " tag.
	 * 
	 * @return the HTTP "Server: " header value
	 */
	public static String getHttpServer() {
		return HTTP_SERVER;
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
	 * Returns the user's current working directory as a <tt>File</tt>
	 * instance, or <tt>null</tt> if the property is not set.
	 *
	 * @return the user's current working directory as a <tt>File</tt>
	 *  instance, or <tt>null</tt> if the property is not set
	 */
	public static File getCurrentDirectory() {
		return new File(PROPS.getProperty("user.dir"));
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
		if(_isWindows98 || _isWindows95 || _isWindowsMe || _isMacClassic) {
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
	public static boolean isWindowsNTor2000orXP() {
		return _isWindowsNTor2000orXP;
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
	 * Returns whether or not the current JVM is a 1.1.8 implementation.
	 *
	 * @return <tt>true</tt> if we are running on 1.1.8, <tt>false</tt>
	 *  otherwise
	 */
	public static boolean isJava118() {
		return _isJava118;
	}

	/**
	 * Returns whether or not the current JVM is 1.3.x or later
	 *
	 * @return <tt>true</tt> if we are running on 1.3.x or later, 
     *  <tt>false</tt> otherwise
	 */
	public static boolean isJava13OrLater() {       
        String version=CommonUtils.getJavaVersion();
		return !version.startsWith("1.2") 
            && !version.startsWith("1.1") 
		    && !version.startsWith("1.0"); 
	}	

	/**
	 * Returns whether or not the current JVM is 1.4.x or later
	 *
	 * @return <tt>true</tt> if we are running on 1.4.x or later, 
     *  <tt>false</tt> otherwise
	 */
	public static boolean isJava14OrLater() {
        String version=CommonUtils.getJavaVersion();
		return !version.startsWith("1.3") 
            && !version.startsWith("1.2") 
		    && !version.startsWith("1.1")  
		    && !version.startsWith("1.0"); 
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
    
    /**
     * Returns the user home directory.
     *
     * @return the <tt>File</tt> instance denoting the abstract pathname of
     *  the user's home directory, or <tt>null</tt> if the home directory
	 *  does not exist
     */
    public static File getUserHomeDir() {
        return new File(PROPS.getProperty("user.home"));
    }
    
    /**
     * Returns the directory where all user settings should be stored.  This
     * is where all application data should be stored.  If the directory does
     * does not already exist, this attempts to create the directory, although
     * this is not guaranteed to succeed.
     *
     * @return the <tt>File</tt> instance denoting the user's home 
     *  directory for the application, or <tt>null</tt> if that directory 
	 *  does not exist
     */
    public static File getUserSettingsDir() {
        File settingsDir = null;
		if(CommonUtils.isWindows()) {
			settingsDir = CommonUtils.getCurrentDirectory();
		}

		// return the special user preferences directory on OS X.
		// this may have problems on 10.0.
		else if(CommonUtils.isMacOSX()) {
		    File userPrefsDir;
		    try {
		        short userDomainCode = -32763;
		        userPrefsDir = 
		            MRJFileUtils.findFolder(userDomainCode,
		    							    new MRJOSType("pref"));
		        settingsDir = new File(userPrefsDir, ".limewire");
		    } catch(FileNotFoundException e) {
		        // this will just continue to return the default
		        // directory for all oses
		    } catch(NoSuchMethodError e) {
				// this means it's probably an older java implementation,
				// so just return the current directory
				settingsDir = CommonUtils.getCurrentDirectory();
			}
		} else {
            settingsDir = new File(CommonUtils.getUserHomeDir(), 
							       ".limewire");
		}
		if(settingsDir == null) {
		    settingsDir = new File(CommonUtils.getUserHomeDir(), 
							       ".limewire");
		}
        if(!settingsDir.isDirectory()) {
            settingsDir.mkdirs();
        }
        return settingsDir;
    }
	
	/**
	 * Returns whether or not the QuickTime libraries are available
	 * on the user's system.
	 *
	 * @return <tt>true</tt> if the QuickTime libraries are available,
	 *  <tt>false</tt> otherwise
	 */
	public static boolean isQuickTimeAvailable() {
		return CommonUtils.isMacOSX();
	}
	
	/**
	 * Returns whether or not the specified file extension is supported in 
	 * our implementation of QuickTime.  So, this will only return 
	 * <tt>true</tt> if both QuickTime supports the extension in general, 
	 * and if our QuickTime implementation supports the extension.
	 *
	 * @param ext the extension to check for QuickTime support
	 * @return <tt>true</tt> if QuickTime supports the file type and our 
	 *  implementation of QuickTime supports that part of QuickTime's 
	 *  functionality, <tt>false</tt> otherwise
	 */
	public static boolean isQuickTimeSupportedFormat(File file) {
		String fileName = file.getName();
		if(fileName.equals("") || fileName.length()<4) {
			return false;
		}
		
		int i = fileName.lastIndexOf(".");
		if(i == -1 || i==fileName.length()) return false;
		
		String ext = fileName.substring(i+1).toLowerCase();
		String[] supportedFormats = {
		    "mp3", "wav", "au", "aif", "aiff"};
		
		for(int r=0; r<supportedFormats.length; r++) {
			if(ext.equals(supportedFormats[r])) return true;
		}
		return false;
	}

	/**
	 * Convenience method that checks both that the QuickTime for Java
	 * libraries are available and that we can launch the specified 
	 * file using QuickTime.
	 *
	 * @return <tt>true</tt> if the QuickTime for Java libraries are
	 *  available and the file is of a type that our QuickTime players
	 *  support, <tt>false</tt> otherwise
	 */
	public static boolean canLaunchFileWithQuickTime(File file) {
		if(!isQuickTimeAvailable()) return false;
		return isQuickTimeSupportedFormat(file);
	}


    /*
    public static void main(String args[]) {
        System.out.println("Is 1.3 or later? "+isJava13OrLater());
        System.out.println("Is 1.4 or later? "+isJava14OrLater());
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



