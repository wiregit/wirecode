package com.limegroup.gnutella.util;

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
	private static final String LIMEWIRE_VERSION = "@version@";

    /**
     * The cached value of the major revision number.
     */
    private static final int _majorVersionNumber = 
        getMajorVersionNumberInternal(LIMEWIRE_VERSION);

    /**
     * The cached value of the minor revision number.
     */
    private static final int _minorVersionNumber = 
        getMinorVersionNumberInternal(LIMEWIRE_VERSION);

    /**
     * The cached value of the GUESS major revision number.
     */
    private static final int _guessMajorVersionNumber = 0;

    /**
     * The cached value of the GUESS minor revision number.
     */
    private static final int _guessMinorVersionNumber = 1;

    /**
     * The cached value of the Ultrapeer major revision number.
     */
    private static final int _upMajorVersionNumber = 0;

    /**
     * The cached value of the Ultrapeer minor revision number.
     */
    private static final int _upMinorVersionNumber = 1;

    /**
     * The vendor code for QHD and GWebCache.  WARNING: to avoid character
     * encoding problems, this is hard-coded in QueryReply as well.  So if you
     * change this, you must change QueryReply.
     */
    public static final String QHD_VENDOR_NAME = "LIME";

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

    private static final String LIMEWIRE_PREFS_DIR_NAME = ".limewire";

	/**
	 * Constant for the current running directory.
	 */
	private static final File CURRENT_DIRECTORY =
		new File(PROPS.getProperty("user.dir"));


    /**
     * Variable for whether or this this is a PRO version of LimeWire. 
     */
    private static boolean _isPro = false;


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
			HTTP_SERVER = ("LimeWire/"+LIMEWIRE_VERSION.
                           substring(0, LIMEWIRE_VERSION.length()-4)+" (Pro)");
            _isPro = true;
		}
	}

    /** Gets the major version of GUESS supported.
     */
    public static int getGUESSMajorVersionNumber() {    
        return _guessMajorVersionNumber;
    }
    
    /** Gets the minor version of GUESS supported.
     */
    public static int getGUESSMinorVersionNumber() {
        return _guessMinorVersionNumber;
    }

    /** Gets the major version of Ultrapeer Protocol supported.
     */
    public static int getUPMajorVersionNumber() {    
        return _upMajorVersionNumber;
    }
    
    /** Gets the minor version of Ultrapeer Protocol supported.
     */
    public static int getUPMinorVersionNumber() {
        return _upMinorVersionNumber;
    }

	/**
	 * Returns the current version number of LimeWire as
     * a string, e.g., "1.4".
	 */
	public static String getLimeWireVersion() {
		return LIMEWIRE_VERSION;
	}

    /** Gets the major version of LimeWire.
     */
    public static int getMajorVersionNumber() {    
        return _majorVersionNumber;
    }
    
    /** Gets the minor version of LimeWire.
     */
    public static int getMinorVersionNumber() {
        return _minorVersionNumber;
    }

    static int getMajorVersionNumberInternal(String version) {
        if (!version.equals("@version@")) {
            try {
                int firstDot = version.indexOf(".");
                String majorStr = version.substring(0, firstDot);
                return new Integer(majorStr).intValue();
            }
            catch (NumberFormatException nfe) {
            }
        }
        // in case this is a mainline version or NFE was caught (strange)
        return 2;
    }

    public static boolean isPro() {
        return _isPro;
    }


    static int getMinorVersionNumberInternal(String version) {
        if (!version.equals("@version@")) {
            try {
                int firstDot = version.indexOf(".");
                String minusMajor = version.substring(firstDot+1);
                int secondDot = minusMajor.indexOf(".");
                String minorStr = minusMajor.substring(0, secondDot);
                return new Integer(minorStr).intValue();
            }
            catch (NumberFormatException nfe) {
            }
        }
        // in case this is a mainline version or NFE was caught (strange)
        return 7;
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
	 * Returns the operating system version.
	 */
	public static String getOSVersion() {
		return PROPS.getProperty("os.version");
	}

	/**
	 * Returns the user's current working directory as a <tt>File</tt>
	 * instance, or <tt>null</tt> if the property is not set.
	 *
	 * @return the user's current working directory as a <tt>File</tt>
	 *  instance, or <tt>null</tt> if the property is not set
	 */
	public static File getCurrentDirectory() {
		return CURRENT_DIRECTORY;
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
	 * Returns whether or not the os is Mac OSX 10.2 or above.
	 *
	 * @return <tt>true</tt> if the application is running on Mac OSX, 
	 *  10.2 or above, <tt>false</tt> otherwise
	 */
	public static boolean isJaguarOrAbove() {
		if(!isMacOSX()) return false;
		return getOSVersion().startsWith("10.2");
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
    public synchronized static File getUserSettingsDir() {
        File settingsDir = new File(getUserHomeDir(), 
                                    LIMEWIRE_PREFS_DIR_NAME);
        if(CommonUtils.isMacOSX()) {            
            File tempSettingsDir = new File(getUserHomeDir(), 
                                            "Library/Preferences");
            settingsDir = new File(tempSettingsDir, "LimeWire");
		} 

        if(!settingsDir.isDirectory()) {
            if(!settingsDir.mkdirs()) {
                String msg = "could not create preferences directory: "+
                    settingsDir;
                throw new RuntimeException(msg);
            }
        }
        // make sure Windows files are moved
        moveWindowsFiles(settingsDir);
        return settingsDir;
    }

    /**
     * Boolean for whether or not the windows files have been copied.
     */
    private static boolean _windowsFilesMoved = false;

    /**
     * The array of files that should be stored in the user's home 
     * directory.
     */
    private static final String[] USER_FILES = {
        "limewire.props",
        "gnutella.net",
        "fileurns.cache"
    };

    /**
     * On Windows, this copies files from the current directory to the
     * user's LimeWire home directory.  The installer does not have
     * access to the user's home directory, so these files must be
     * copied.  Note that they are only copied, however, if existing 
     * files are not there.  This ensures that the most recent files,
     * and the files that should be used, should always be saved in 
     * the user's home LimeWire preferences directory.
     */
    private synchronized static void moveWindowsFiles(File settingsDir) {
        if(!isWindows()) return;
        if(_windowsFilesMoved) return;
        File currentDir = CommonUtils.getCurrentDirectory();
        for(int i=0; i<USER_FILES.length; i++) {
            File curUserFile = new File(settingsDir, USER_FILES[i]);
            File curDirFile  = new File(currentDir,  USER_FILES[i]);
            
            // if the file already exists in the user's home directory,
            // don't copy it
            if(curUserFile.isFile()) {
                continue;
            }
            copy(curDirFile, curUserFile);
        }
        _windowsFilesMoved = true;
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
	
	/**
	 * Convenience method to generically compare any two comparable
	 * things.
     *
	 * Handles comparison uniquely for 'native' types.
	 * This is for a few reasons:
	 * 1) We want to compare strings by lowercase comparison
	 * 2) Java 1.1.8 did not have native types implement Comparable
	 * Note that we check for both java.lang.Comparable and
	 * com.sun.java.util.collections.Comparable,
	 * and we do this _before_ checking for native types.
	 * So, this is slightly optimized for more recent JVMs
	 * Note that non-integer comparisons must specifically
	 * check if the difference is less or greater than 0
	 * so that rounding won't be wrong.
	 * Of the native types, we check 'Integer' first since
	 * that's the most common, Boolean,
	 * then Double or Float, and finally, the rest will be caught in
	 * 'Number', which just uses an int comparison.
	 */
    public static int compare(Object o1, Object o2) {
        int retval;
        
        if ( o1 == null && o2 == null ) {
            retval = 0;
        } else if ( o1 == null ) {
            retval = -1;
        } else if ( o2 == null ) {
            retval = 1;
        } else if ( o1.getClass() == String.class ) {
            retval = compareIgnoreCase( (String)o1, (String)o2 );
        } else if( o1 instanceof com.sun.java.util.collections.Comparable ) {
            retval =
                ((com.sun.java.util.collections.Comparable)o1).compareTo(o2);
        } else if( o1 instanceof Integer ) {
            retval = ((Integer)o1).intValue() - ((Integer)o2).intValue();
        } else if( o1 instanceof Boolean ) {
            retval = o1.equals(o2) ? 0 : o1.equals(Boolean.TRUE) ? 1 : -1;
        } else if( o1 instanceof Double || o1 instanceof Float ) {
            double dbl = 
                ((Number)o1).doubleValue() - ((Number)o2).doubleValue();
            if ( dbl > 0 ) retval = 1;
            else if ( dbl < 0 ) retval = -1;
            else retval = 0;
        } else if( o1 instanceof Number ) {
            retval = ((Number)o1).intValue() - ((Number)o2).intValue();
        } else {
            retval = 0;
        }
        return retval;
    } 

    /** 
     * Utility method to avoid loading of StringUtils class.
     */
    private static int compareIgnoreCase(String a, String b) {
        //Check out String.compareTo(String) for a description of the basic
        //algorithm.  The ignore case extension is trivial.
        for (int i=0; i<Math.min(a.length(), b.length()); i++) {
            char ac=Character.toLowerCase(a.charAt(i));
            char bc=Character.toLowerCase(b.charAt(i));
            int diff=ac-bc;
            if (diff!=0)
                return diff;
        }
        return a.length()-b.length();
    }

	/**
	 * Copies the specified resource file into the current directory from
	 * the jar file. If the file already exists, no copy is performed.
	 *
	 * @param fileName the name of the file to copy, relative to the jar 
	 *  file -- such as "com/limegroup/gnutella/gui/images/image.gif"
	 */
	public static void copyResourceFile(final String fileName) {
		copyResourceFile(fileName, null, false);
	}  

	/**
	 * Copies the specified resource file into the current directory from
	 * the jar file. If the file already exists, no copy is performed.
	 *
	 * @param fileName the name of the file to copy, relative to the jar 
	 *  file -- such as "com/limegroup/gnutella/gui/images/image.gif"
	 */
    public static void copyResourceFile(final String fileName, File newFile, 
										final boolean forceOverwrite) {
		if(newFile == null) newFile = new File(fileName);

		// return quickly if the file is already there, no copy necessary
		if( !forceOverwrite && newFile.exists() ) return;
		String parentString = newFile.getParent();
        if(parentString == null) return;
		File parentFile = new File(parentString);
		if(!parentFile.isDirectory()) {
			parentFile.mkdirs();
		}

		ClassLoader cl = CommonUtils.class.getClassLoader();			
		
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;            
		try {
			//load resource using my class loader or system class loader
			//Can happen if Launcher loaded by system class loader
			InputStream is =  
				cl != null
				?  cl.getResource(fileName).openStream()
				:  ClassLoader.getSystemResource(fileName).openStream();
			
			//buffer the streams to improve I/O performance
			final int bufferSize = 2048;
			bis = new BufferedInputStream(is, bufferSize);
			bos = 
				new BufferedOutputStream(new FileOutputStream(newFile), 
										 bufferSize);
			byte[] buffer = new byte[bufferSize];
			int c = 0;
			
			do { //read and write in chunks of buffer size until EOF reached
				c = bis.read(buffer, 0, bufferSize);
				bos.write(buffer, 0, c);
			}
			while (c == bufferSize); //(# of bytes read)c will = bufferSize until EOF
			
		} catch(Exception e) {	
			//if there is any error, delete any portion of file that did write
			newFile.delete();
		} finally {
			try {
				if(bis != null) bis.close();
				if(bos != null) bos.close();
			} catch(IOException ioe) {}	// all we can do is try to close the streams
		} 
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



