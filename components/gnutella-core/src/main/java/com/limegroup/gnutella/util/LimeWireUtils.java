package com.limegroup.gnutella.util;

import java.io.File;
import java.io.IOException;

import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;
import org.limewire.util.VersionUtils;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ApplicationSettings;


/**
 * This class handles common utility functions that many classes
 * may want to access.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class LimeWireUtils {

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
	 * Cached constant for the HTTP Server: header value.
	 */
	private static final String HTTP_SERVER;

    private static final String LIMEWIRE_PREFS_DIR_NAME = ".limewire";

	/**
	 * Constant for the current running directory.
	 */
	private static final File CURRENT_DIRECTORY =
		new File(System.getProperty("user.dir"));

    /**
     * Variable for whether or not this is a PRO version of LimeWire. 
     */
    private static boolean _isPro = false;
    
    /**
     * Variable for the settings directory.
     */
    private static File SETTINGS_DIRECTORY = null;


	/**
	 * Make sure the constructor can never be called.
	 */
	private LimeWireUtils() {}
    
	/**
	 * Initialize the settings statically. 
	 */
	static {
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
	 * Returns the user's current working directory as a <tt>File</tt>
	 * instance, or <tt>null</tt> if the property is not set.
	 *
	 * @return the user's current working directory as a <tt>File</tt>
	 *  instance, or <tt>null</tt> if the property is not set
	 */
	public static File getCurrentDirectory() {
		return CURRENT_DIRECTORY;
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
        
        File settingsDir = new File(CommonUtils.getUserHomeDir(), LIMEWIRE_PREFS_DIR_NAME);
        if (OSUtils.isWindows()) {
            String appdata = System.getProperty("LIMEWIRE_PREFS_DIR", SystemUtils.getSpecialPath("ApplicationData"));
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
        } else if(OSUtils.isMacOSX()) {
            settingsDir = new File(CommonUtils.getUserHomeDir(), "Library/Preferences/LimeWire");
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
     * Updates a URL to contain common information about the LW installation.
     */
    public static String addLWInfoToUrl(String url) {
        if(url.indexOf('?') == -1)
            url += "?";
        else
            url += "&";
        url += "pro="   + LimeWireUtils.isPro() + 
               "&lang=" + EncodingUtils.encode(ApplicationSettings.getLanguage()) +
               "&lv="   + EncodingUtils.encode(LimeWireUtils.getLimeWireVersion()) +
               "&jv="   + EncodingUtils.encode(VersionUtils.getJavaVersion()) +
               "&os="   + EncodingUtils.encode(OSUtils.getOS()) +
               "&osv="  + EncodingUtils.encode(OSUtils.getOSVersion()) +
               "&guid=" + EncodingUtils.encode(new GUID(RouterService.getMyGUID()).toHexString());
        return url;
    }
}



