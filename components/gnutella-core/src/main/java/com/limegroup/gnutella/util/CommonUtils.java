package com.limegroup.gnutella.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ApplicationSettings;

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
     * Variable used for testing only, it's value is set to whatever the test
     * needs, and getVersion method retuns this value if it's not null
     */
    private static String testVersion = null;

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
     * The cached value of the really minor version number.
     */
    private static final int _serviceVersionNumber =
        getServiceVersionNumberInternal(LIMEWIRE_VERSION);

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
	 * Variable for whether or not we're on Windows NT.
	 */
	private static boolean _isWindowsNT = false;

	/** 
	 * Variable for whether or not we're on Windows XP.
	 */
	private static boolean _isWindowsXP = false;

	/** 
	 * Variable for whether or not we're on Windows NT, 2000, or XP.
	 */
	private static boolean _isWindowsNTor2000orXP = false;

	/** 
	 * Variable for whether or not we're on Windows Vista.
	 */
	private static boolean _isWindowsVista = false;

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
     * Variable for whether or not we're on OS/2.
     */
    private static boolean _isOS2 = false;
     


    /**
     * Several arrays of illegal characters on various operating systems.
     * Used by convertFileName
     */
    private static final char[] ILLEGAL_CHARS_ANY_OS = {
		'/', '\n', '\r', '\t', '\0', '\f' 
	};
    private static final char[] ILLEGAL_CHARS_UNIX = {'`'};
    private static final char[] ILLEGAL_CHARS_WINDOWS = { 
		'?', '*', '\\', '<', '>', '|', '\"', ':'
	};
	private static final char[] ILLEGAL_CHARS_MACOS = {':'};

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
     * Variable for whether or not this is a PRO version of LimeWire. 
     */
    private static boolean _isPro = false;
    
    /**
     * Variable for the settings directory.
     */
    static File SETTINGS_DIRECTORY = null;


	/**
	 * Make sure the constructor can never be called.
	 */
	private CommonUtils() {}
    
	/**
	 * Initialize the settings statically. 
	 */
	static {
	    setOperatingSystems();
		
		if(!LIMEWIRE_VERSION.endsWith("Pro")) {
			HTTP_SERVER = "LimeWire/" + LIMEWIRE_VERSION;
		}
		else {
			HTTP_SERVER = ("LimeWire/"+LIMEWIRE_VERSION.
                           substring(0, LIMEWIRE_VERSION.length()-4)+" (Pro)");
            _isPro = true;
		}
	}
	
	/**
	 * Sets the operating system variables.
	 */
	private static void setOperatingSystems() {
		_isWindows = false;
		_isWindowsNTor2000orXP = false;
		_isWindowsNT = false;
		_isWindowsXP = false;
		_isWindows95 = false;
		_isWindows98 = false;
		_isWindowsMe = false;
		_isSolaris = false;
		_isLinux = false;
		_isOS2 = false;
		_isMacOSX = false;


		String os = System.getProperty("os.name").toLowerCase(Locale.US);

		// set the operating system variables
		_isWindows = os.indexOf("windows") != -1;
		if (os.indexOf("windows nt") != -1 || 
			os.indexOf("windows 2000")!= -1 ||
			os.indexOf("windows xp")!= -1)
			_isWindowsNTor2000orXP = true;
		if (os.indexOf("windows nt") != -1) 
			_isWindowsNT = true;
		if (os.indexOf("windows xp") != -1) 
			_isWindowsXP = true;
                if (os.indexOf("windows vista") != -1)
                    _isWindowsVista = true;
		if(os.indexOf("windows 95") != -1)
		   _isWindows95 = true;
		if(os.indexOf("windows 98") != -1)
		   _isWindows98 = true;
		if(os.indexOf("windows me") != -1)
		   _isWindowsMe = true;
		_isSolaris = os.indexOf("solaris") != -1;
		_isLinux   = os.indexOf("linux")   != -1;
        _isOS2     = os.indexOf("os/2")    != -1;
        if(_isWindows || _isLinux) _supportsTray=true;
		if(os.startsWith("mac os")) {
			if(os.endsWith("x")) {
				_isMacOSX = true;
			}
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
        if(testVersion==null)//Always the case, except when update tests are run
            return LIMEWIRE_VERSION;
        return testVersion;
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
    
    /** Gets the minor minor version of LimeWire.
     */
   public static int getServiceVersionNumber() {
        return _serviceVersionNumber;
   }
    

    static int getMajorVersionNumberInternal(String version) {
        if (!version.equals("@" + "version" + "@")) {
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

    /**
     * Accessor for whether or not this is LimeWire pro.
     *
     * @return <tt>true</tt> if it is pro, otherwise <tt>false</tt>
     */
    public static boolean isPro() {
        return _isPro;
    }
    
    /**
     * Accessor for whether or not this is a testing version
     * (@version@) of LimeWire.
     *
     * @return <tt>true</tt> if the version is @version@,
     *  otherwise <tt>false</tt>
     */
    public static boolean isTestingVersion() {
        return LIMEWIRE_VERSION.equals("@" + "version" + "@");
    }

    static int getMinorVersionNumberInternal(String version) {
        if (!version.equals("@" + "version" + "@")) {
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
    
    static int getServiceVersionNumberInternal(String version) {
        if (!version.equals("@" + "version" + "@")) {
            try {
                int firstDot = version.indexOf(".");
                int secondDot = version.indexOf(".", firstDot+1);
                
                int p = secondDot+1;
                int q = p;
                
                while(q < version.length() && 
                            Character.isDigit(version.charAt(q))) {
                    q++;
                }
                
                if (p != q) {
                    String service = version.substring(p, q);
                    return new Integer(service).intValue();
                }
            }
            catch (NumberFormatException nfe) {
            }
        }
        // in case this is a mainline version or NFE was caught (strange)
        return 0;
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
	 * capable of meeting the requirements of a ultrapeer.
	 *
	 * @return <tt>true</tt> if this OS meets ultrapeer requirements,
	 *         <tt>false</tt> otherwise
	 */
	public static boolean isUltrapeerOS() {
	    return !(_isWindows98 || _isWindows95 || _isWindowsMe || _isWindowsNT);
	}

        /**
         * @return true if this is a well-supported version of windows.
         * (not 95, 98, nt or me)
         */
        public static boolean isGoodWindows() {
                return isWindows() && isUltrapeerOS();
        }
        
	/**
	 * Returns whether or not the OS is some version of Windows.
	 *
	 * @return <tt>true</tt> if the application is running on some Windows 
	 *         version, <tt>false</tt> otherwise
	 */
	public static boolean isWindows() {
		return _isWindows;
	}

	/**
	 * Returns whether or not the OS is Windows NT, 2000, or XP.
	 *
	 * @return <tt>true</tt> if the application is running on Windows NT,
	 *  2000, or XP <tt>false</tt> otherwise
	 */
	public static boolean isWindowsNTor2000orXP() {
		return _isWindowsNTor2000orXP;
	}


	/**
	 * Returns whether or not the OS is WinXP.
	 *
	 * @return <tt>true</tt> if the application is running on WinXP,
	 *  <tt>false</tt> otherwise
	 */
	public static boolean isWindowsXP() {
		return _isWindowsXP;
	}
        
        /**
         * @return true if the application is running on Windows Vista
         */
        public static boolean isWindowsVista() {
            return _isWindowsVista;
        }
        
        /**
         * @return true if the application is running on a windows with
         * the 10 socket limit.
         */
        public static boolean isSocketChallengedWindows() {
            return isWindowsVista() || isWindowsXP();
        }
        
        /**
         * @return true if the application is running on a windows 
         * that supports native theme.
         */
        public static boolean isNativeThemeWindows() {
                return isWindowsVista() || isWindowsXP();
        }

    /**
     * Returns whether or not the OS is OS/2.
     *
     * @return <tt>true</tt> if the application is running on OS/2,
     *         <tt>false</tt> otherwise
     */
    public static boolean isOS2() {
        return _isOS2;
    }
     
	/** 
	 * Returns whether or not the OS is Mac OSX.
	 *
	 * @return <tt>true</tt> if the application is running on Mac OSX, 
	 *         <tt>false</tt> otherwise
	 */
	public static boolean isMacOSX() {
		return _isMacOSX;
	}
	
	/** 
	 * Returns whether or not the OS is Mac OSX 10.2 or above.
	 *
	 * @return <tt>true</tt> if the application is running on Mac OSX, 
	 *  10.2 or above, <tt>false</tt> otherwise
	 */
	public static boolean isJaguarOrAbove() {
		if(!isMacOSX()) return false;
		return getOSVersion().startsWith("10.2");
	}
	
	/**
	 * Returns whether or not the OS is Mac OSX 10.3 or above.
	 *
	 * @return <tt>true</tt> if the application is running on Mac OSX, 
	 *  10.3 or above, <tt>false</tt> otherwise
	 */
	public static boolean isPantherOrAbove() {
	    if(!isMacOSX()) return false;
	    return getOSVersion().startsWith("10.3");
	}
    
    /**
     * Returns whether or not the Cocoa Foundation classes are available.
     */
    public static boolean isCocoaFoundationAvailable() {
        if(!isMacOSX())
            return false;
            
        try {
            Class.forName("com.apple.cocoa.foundation.NSUserDefaults");
            Class.forName("com.apple.cocoa.foundation.NSMutableDictionary");
            Class.forName("com.apple.cocoa.foundation.NSMutableArray");
            Class.forName("com.apple.cocoa.foundation.NSObject");
            Class.forName("com.apple.cocoa.foundation.NSSystem");
            return true;
        } catch(ClassNotFoundException error) {
            return false;
        } catch(NoClassDefFoundError error) {
            return false;
        }
    }

    /** 
	 * Returns whether or not the OS is any Mac OS.
	 *
	 * @return <tt>true</tt> if the application is running on Mac OSX
	 *  or any previous mac version, <tt>false</tt> otherwise
	 */
	public static boolean isAnyMac() {
		return _isMacOSX;
	}

	/** 
	 * Returns whether or not the OS is Solaris.
	 *
	 * @return <tt>true</tt> if the application is running on Solaris, 
	 *         <tt>false</tt> otherwise
	 */
	public static boolean isSolaris() {
		return _isSolaris;
	}

	/** 
	 * Returns whether or not the OS is Linux.
	 *
	 * @return <tt>true</tt> if the application is running on Linux, 
	 *         <tt>false</tt> otherwise
	 */
	public static boolean isLinux() {
		return _isLinux;
	}

	/** 
	 * Returns whether or not the OS is some version of
	 * Unix, defined here as only Solaris or Linux.
	 */
	public static boolean isUnix() {
		return _isLinux || _isSolaris; 
	}
	
	/**
	 * Returns whether the OS is POSIX-like. 
	 */
	public static boolean isPOSIX() {
	    return _isLinux || _isSolaris || _isMacOSX;
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
	 * Returns whether or not the current JVM is 1.4.x or later
	 *
	 * @return <tt>true</tt> if we are running on 1.4.x or later, 
     *  <tt>false</tt> otherwise
	 */
	public static boolean isJava142OrLater() {
        String version = CommonUtils.getJavaVersion();
        return !version.startsWith("1.4.1")
            && !version.startsWith("1.4.0")
            && isJava14OrLater();
	}	
	
	/**
	 * Returns whether or not the current JVM is 1.5.x or later.
	 */
	public static boolean isJava15OrLater() {
        String version=CommonUtils.getJavaVersion();
        return !version.startsWith("1.4")
		    && !version.startsWith("1.3") 
            && !version.startsWith("1.2") 
		    && !version.startsWith("1.1")  
		    && !version.startsWith("1.0"); 
    }
    
    /**
     * Returns whether or not the current JVM is 1.6.x or later.
     */
    public static boolean isJava16OrLater() {
        String version=CommonUtils.getJavaVersion();
        return  !version.startsWith("1.5")
        && !version.startsWith("1.4")
        && !version.startsWith("1.3") 
        && !version.startsWith("1.2") 
        && !version.startsWith("1.1")  
        && !version.startsWith("1.0");
    }
    
    /**
     * Determines if your version of java is out of date.
     */
    public static boolean isJavaOutOfDate() {
        return isWindows() &&
               !isSpecificJRE() &&
               (getJavaVersion().startsWith("1.3") ||
                getJavaVersion().startsWith("1.4.0"));
    }
    
    /**
     * Determines if this was loaded from a specific JRE.
     */
    public static boolean isSpecificJRE() {
        return new File(".", "jre").isDirectory();
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
     * Return the user's name.
     *
     * @return the <tt>String</tt> denoting the user's name.
     */
    public static String getUserName() {
        return PROPS.getProperty("user.name");
    }
    
    
    private static synchronized void setUserSettingsDir(File settingsDir) throws IOException, SecurityException {
        settingsDir = settingsDir.getAbsoluteFile();
        
        if(!settingsDir.isDirectory()) {
            settingsDir.delete(); // delete whatever it may have been
            if(!settingsDir.mkdirs()) {
                String msg = "could not create preferences directory: "+
                    settingsDir;
                throw new IOException(msg);
            }
        }

        if(!settingsDir.canWrite()) {
            throw new IOException("settings dir not writable");
        }

        if(!settingsDir.canRead()) {
            throw new IOException("settings dir not readable");
        }

        // make sure Windows files are moved
        moveWindowsFiles(settingsDir);
        // make sure old metadata files are moved
        moveXMLFiles(settingsDir);
        // cache the directory.
        SETTINGS_DIRECTORY = settingsDir;
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
        // LOGIC:
        
        // On all platforms other than Windows or OSX,
        // this will return <user-home>/.limewire
        
        // On OSX, this will return <user-home>/Library/Preferences/LimeWire
        
        // On Windows, this first tries to find:
        // a) <user-home>/$LIMEWIRE_PREFS_DIR/LimeWire
        // b) <user-home>/$APPDATA/LimeWire
        // c) <user-home/.limewire
        // If the $LIMEWIRE_PREFS_DIR variable doesn't exist, it falls back
        // to trying b).  If The $APPDATA variable can't be read or doesn't
        // exist, it falls back to a).
        // If using a) or b), and neither of those directories exist, but c)
        // does, then c) is used.  Once a) or b) exist, they are used indefinitely.
        // If neither a), b) nor c) exist, then the former is created in preference of
        // of a), then b).
        
        if ( SETTINGS_DIRECTORY != null )
            return SETTINGS_DIRECTORY;
        
        File settingsDir = new File(getUserHomeDir(), LIMEWIRE_PREFS_DIR_NAME);
        if (isWindows()) {
            String appdata = null;
            // In some Java 1.4 implementations, System.getenv() is 
            // depricated with prejudice (throws java.lang.Error).
            if (isJava15OrLater()) {
                appdata = System.getProperty("LIMEWIRE_PREFS_DIR", System.getenv("APPDATA"));
            } else {
                // null string will fall back on default
                appdata = System.getProperty("LIMEWIRE_PREFS_DIR",null);
            }
            
            if ("%APPDATA%".equals(appdata)) {
                appdata = null; // fall back on default
            }
            
            if (appdata != null && appdata.length() > 0) {
                File tempSettingsDir = new File(appdata, "LimeWire");
                if (tempSettingsDir.isDirectory() || !settingsDir.exists()) {
                    try {
                        setUserSettingsDir(tempSettingsDir);
                        return tempSettingsDir;
                    } catch (IOException e) { // Ignore errors and fall back on default
                    } catch (SecurityException e) {} // Ignore errors and fall back on default
                }
            }
        } else if(isMacOSX()) {
            settingsDir = new File(getUserHomeDir(), "Library/Preferences/LimeWire");
        } 
      
        // Default behavior
        try {
            setUserSettingsDir(settingsDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return settingsDir;
    }

    /**
     * Boolean for whether or not the windows files have been copied.
     */
    private static boolean _windowsFilesMoved = false;
    
    /**
     * Boolean for whether or not XML files have been copied.
     */
    private static boolean _xmlFilesMoved = false;

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
            if(!copy(curDirFile, curUserFile)) {
                throw new RuntimeException();
            }
        }
        _windowsFilesMoved = true;
    }

    /**
     * Old metadata definitions must be moved from ./lib/xml/data/*.*
     * This is done like the windows files copying, but for all files
     * in the data directory.
     */
    private synchronized static void moveXMLFiles(File settingsDir) {
        if(_xmlFilesMoved) return;
        // We must extend the currentDir & settingsDir to look 
        // in the right places (lib/xml/data & xml/data).
        File currentDir = new File( 
            CommonUtils.getCurrentDirectory().getPath() + "/lib/xml/data"
        );
        settingsDir = new File(settingsDir.getPath() + "/xml/data");
        settingsDir.mkdirs();
        String[] filesToMove = currentDir.list();
        if ( filesToMove != null ) {
            for(int i=0; i<filesToMove.length; i++) {
                File curUserFile = new File(settingsDir, filesToMove[i]);
                File curDirFile  = new File(currentDir,  filesToMove[i]);
                
                // if the file already exists in the user's home directory,
                // don't copy it
                if(curUserFile.isFile()) {
                    continue;
                }
                copy(curDirFile, curUserFile);
            }
        }
        _xmlFilesMoved = true;
    }
	     
    
    /**
     * Gets a resource file using the CommonUtils class loader,
     * or the system class loader if CommonUtils isn't loaded.
     */
    public static File getResourceFile(String location) {
        ClassLoader cl = CommonUtils.class.getClassLoader();            
        URL resource = null;

        if(cl == null) {
            resource = ClassLoader.getSystemResource(location);
        } else {
            resource = cl.getResource(location);
        }
        
        if( resource == null ) {
            // note: this will probably not work,
            // but it will ultimately trigger a better exception
            // than returning null.
            return new File(location);
        }
        
        //NOTE: The resource URL will contain %20 instead of spaces.
        // This is by design, but will not work when trying to make a file.
        // See BugParadeID: 4466485
        //(http://developer.java.sun.com/developer/bugParade/bugs/4466485.html)
        // The recommended workaround is to use the URI class, but that doesn't
        // exist until Java 1.4.  So, we can't use it here.
        // Thus, we manually have to parse out the %20s from the URL
        return new File( decode(resource.getFile()) );
    }
    
    /**
     * Gets an InputStream from a resource file.
     * 
     * @param location the location of the resource in the resource file
     * @return an <tt>InputStream</tt> for the resource
     * @throws IOException if the resource could not be located or there was
     *  another IO error accessing the resource
     */
    public static InputStream getResourceStream(String location) 
      throws IOException {
       ClassLoader cl = CommonUtils.class.getClassLoader();            
       URL resource = null;

        if(cl == null) {
            resource = ClassLoader.getSystemResource(location);
        } else {
            resource = cl.getResource(location);
        }
        
        if( resource == null) 
            throw new IOException("null resource: "+location);
        else
            return resource.openStream();
    }
    
    /**
     * Copied from URLDecoder.java
     */
    public static String decode(String s) {
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    sb.append(' ');
                    break;
                case '%':
                    try {
                        sb.append((char)Integer.parseInt(
                                        s.substring(i+1,i+3),16));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(s);
                    }
                    i += 2;
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        // Undo conversion to external encoding
        String result = sb.toString();
        try {
            byte[] inputBytes = result.getBytes("8859_1");
            result = new String(inputBytes);
        } catch (UnsupportedEncodingException e) {
            // The system should always have 8859_1
        }
        return result;
    }
        

	/**
	 * Copies the specified resource file into the current directory from
	 * the jar file. If the file already exists, no copy is performed.
	 *
	 * @param fileName the name of the file to copy, relative to the jar 
	 *  file -- such as "com/limegroup/gnutella/gui/images/image.gif"
	 */
	public static void copyResourceFile(final String fileName) {
		copyResourceFile(fileName, null);
	}  


	/**
	 * Copies the specified resource file into the current directory from
	 * the jar file. If the file already exists, no copy is performed.
	 *
	 * @param fileName the name of the file to copy, relative to the jar
	 *  file -- such as "com/limegroup/gnutella/gui/images/image.gif"
     * @param newFile the new <tt>File</tt> instance where the resource file
     *  will be copied to
	 */
	public static void copyResourceFile(final String fileName, File newFile) {
		copyResourceFile(fileName, newFile, false);		
	}

	/**
	 * Copies the specified resource file into the current directory from
	 * the jar file. If the file already exists, no copy is performed.
	 *
	 * @param fileName the name of the file to copy, relative to the jar 
	 *  file -- such as "com/limegroup/gnutella/gui/images/image.gif"
     * @param newFile the new <tt>File</tt> instance where the resource file
     *  will be copied to -- if this argument is null, the file will be
     *  copied to the current directory
     * @param forceOverwrite specifies whether or not to overwrite the 
     *  file if it already exists
	 */
    public static void copyResourceFile(final String fileName, File newFile, 
										final boolean forceOverwrite) {
		if(newFile == null) newFile = new File(".", fileName);

		// return quickly if the file is already there, no copy necessary
		if( !forceOverwrite && newFile.exists() ) return;
		String parentString = newFile.getParent();
        if(parentString == null) {
            return;
        }
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
            URL resource = cl != null
				?  cl.getResource(fileName)
				:  ClassLoader.getSystemResource(fileName);
                
            if(resource == null)
                throw new NullPointerException("resource: " + fileName +
                                               " doesn't exist.");
            
            InputStream is = resource.openStream();
			
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
                if (c > 0)
                    bos.write(buffer, 0, c);
			}
			while (c == bufferSize); //(# of bytes read)c will = bufferSize until EOF
			
		} catch(IOException e) {	
			//if there is any error, delete any portion of file that did write
			newFile.delete();
		} finally {
            if(bis != null) {
                try {
                    bis.close();
                } catch(IOException ignored) {}
            }
            if(bos != null) {
                try {
                    bos.close();
                } catch(IOException ignored) {}
            }
		} 
	}

    /** 
     * Replaces OS specific illegal characters from any filename with '_', 
	 * including ( / \n \r \t ) on all operating systems, ( ? * \  < > | " ) 
	 * on Windows, ( ` ) on unix.
     *
     * @param name the filename to check for illegal characters
     * @return String containing the cleaned filename
     */
    public static String convertFileName(String name) {
		
		// ensure that block-characters aren't in the filename.
        name = I18NConvert.instance().compose(name);

		// if the name is too long, reduce it.  We don't go all the way
		// up to 256 because we don't know how long the directory name is
		// We want to keep the extension, though.
		if(name.length() > 180) {
		    int extStart = name.lastIndexOf('.');
		    if ( extStart == -1) { // no extension, wierd, but possible
		        name = name.substring(0, 180);
		    } else {
		        // if extension is greater than 11, we concat it.
		        // ( 11 = '.' + 10 extension characters )
		        int extLength = name.length() - extStart;		        
		        int extEnd = extLength > 11 ? extStart + 11 : name.length();
			    name = name.substring(0, 180 - extLength) +
			           name.substring(extStart, extEnd);
            }          
		}
        for (int i = 0; i < ILLEGAL_CHARS_ANY_OS.length; i++) 
            name = name.replace(ILLEGAL_CHARS_ANY_OS[i], '_');
		
        if ( _isWindows || _isOS2 ) {
            for (int i = 0; i < ILLEGAL_CHARS_WINDOWS.length; i++) 
                name = name.replace(ILLEGAL_CHARS_WINDOWS[i], '_');
        } else if ( _isLinux || _isSolaris ) {
            for (int i = 0; i < ILLEGAL_CHARS_UNIX.length; i++) 
                name = name.replace(ILLEGAL_CHARS_UNIX[i], '_');
        } else if (_isMacOSX) {
            for(int i = 0; i < ILLEGAL_CHARS_MACOS.length; i++)
                name = name.replace(ILLEGAL_CHARS_MACOS[i], '_');
        }
        
        return name;
    }

	/**
	 * Converts a value in seconds to:
	 *     "d:hh:mm:ss" where d=days, hh=hours, mm=minutes, ss=seconds, or
	 *     "h:mm:ss" where h=hours<24, mm=minutes, ss=seconds, or
	 *     "m:ss" where m=minutes<60, ss=seconds
	 */
	public static String seconds2time(int seconds) {
	    int minutes = seconds / 60;
	    seconds = seconds - minutes * 60;
	    int hours = minutes / 60;
	    minutes = minutes - hours * 60;
	    int days = hours / 24;
	    hours = hours - days * 24;
	    // build the numbers into a string
	    StringBuffer time = new StringBuffer();
	    if (days != 0) {
	        time.append(Integer.toString(days));
	        time.append(":");
	        if (hours < 10) time.append("0");
	    }
	    if (days != 0 || hours != 0) {
	        time.append(Integer.toString(hours));
	        time.append(":");
	        if (minutes < 10) time.append("0");
	    }
	    time.append(Integer.toString(minutes));
	    time.append(":");
	    if (seconds < 10) time.append("0");
	    time.append(Integer.toString(seconds));
	    return time.toString();
	}
    
    /**
     * Returns the stack traces of all current Threads or an empty
     * String if LimeWire is running on Java 1.4 or if an error
     * occured.
     */
    public static String getAllStackTraces() {
        if (!CommonUtils.isJava15OrLater()) {
            return "";
        }
        
        try {
            Method m = Thread.class.getDeclaredMethod("getAllStackTraces", new Class[0]);
            Map map = (Map)m.invoke(null, new Object[0]);
            
            List sorted = new ArrayList(map.entrySet());
            Collections.sort(sorted, new Comparator() {
                public int compare(Object a, Object b) {
                    Thread threadA = (Thread)((Map.Entry)a).getKey();
                    Thread threadB = (Thread)((Map.Entry)b).getKey();
                    return threadA.getName().compareTo(threadB.getName());
                }
            });
            
            StringBuffer buffer = new StringBuffer();
            Iterator it = sorted.iterator();
            while(it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                Thread key = (Thread)entry.getKey();
                StackTraceElement[] value = (StackTraceElement[])entry.getValue();
                
                buffer.append(key.getName()).append("\n");
                for(int i = 0; i < value.length; i++) {
                    buffer.append("    ").append(value[i]).append("\n");
                }
                buffer.append("\n");
            }
            
            // Remove the last '\n'
            if (buffer.length() > 0) {
                buffer.setLength(buffer.length()-1);
            }
            
            return buffer.toString();
        } catch (Exception err) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("An error occured during getting the StackTraces of all active Threads");
            err.printStackTrace(pw);
            pw.flush();
            return sw.toString();
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
    
    public static String addLWInfoToUrl(String url) {
        if(url.indexOf('?') == -1)
            url += "?";
        else
            url += "&";
        url += "pro="   + isPro() + 
        "&lang=" + encode(ApplicationSettings.getLanguage()) +
        "&lv="   + encode(getLimeWireVersion()) +
        "&jv="   + encode(CommonUtils.getJavaVersion()) +
        "&os="   + encode(getOS()) +
        "&osv="  + encode(getOSVersion()) +
        "&guid=" + encode(new GUID(RouterService.getMyGUID()).toHexString());
        return url;
    }
    
    public static String encode(String string) {
        try {
            return URLEncoder.encode(string, "8859_1");
        } catch(UnsupportedEncodingException uee) {
            return string;
        }
    }
}



