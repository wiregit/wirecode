package org.limewire.util;

import java.util.Locale;

/**
 * Provides methods to get operating system properties, resources and versions, 
 * and determine operating system criteria.
 */
public class OSUtils {
    
    static {
        setOperatingSystems();
    }
    
    /** 
     * Variable for whether or not we're on Windows.
     */
    private static boolean _isWindows;

    /** 
     * Variable for whether or not we're on Windows NT.
     */
    private static boolean _isWindowsNT;

    /** 
     * Variable for whether or not we're on Windows XP.
     */
    private static boolean _isWindowsXP;

    /** 
     * Variable for whether or not we're on Windows NT, 2000, or XP.
     */
    private static boolean _isWindowsNTor2000orXP;

    /** 
     * Variable for whether or not we're on 2000 or XP.
     */
    private static boolean _isWindows2000orXP;

    /** 
     * Variable for whether or not we're on Windows 95.
     */
    private static boolean _isWindows95;

    /** 
     * Variable for whether or not we're on Windows 98.
     */
    private static boolean _isWindows98;

    /** 
     * Variable for whether or not we're on Windows Me.
     */
    private static boolean _isWindowsMe;
    
    /** 
     * Variable for whether or not we're on Windows Vista.
     */
    private static boolean _isWindowsVista;

    /** 
     * Variable for whether or not the operating system allows the 
     * application to be reduced to the system tray.
     */
    private static boolean _supportsTray;

    /** 
     * Variable for whether or not we're on MacOSX.
     */
    private static boolean _isMacOSX;

    /** 
     * Variable for whether or not we're on Linux.
     */
    private static boolean _isLinux;

    /** 
     * Variable for whether or not we're on Solaris.
     */
    private static boolean _isSolaris;

    /**
     * Variable for whether or not we're on OS/2.
     */
    private static boolean _isOS2;

    /**
     * Sets the operating system variables.
     */
    public static void setOperatingSystems() {
    	_isWindows = false;
    	_isWindowsNTor2000orXP = false;
    	_isWindows2000orXP = false;
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
    	if (os.indexOf("windows nt") != -1 || os.indexOf("windows 2000")!= -1 || os.indexOf("windows xp")!= -1)
    		_isWindowsNTor2000orXP = true;
    	if (os.indexOf("windows 2000")!= -1 || os.indexOf("windows xp")!= -1)
    		_isWindows2000orXP = true;
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
        
        if(_isWindows || _isLinux)
            _supportsTray = true;
        
    	if(os.startsWith("mac os")) {
    		if(os.endsWith("x")) {
    			_isMacOSX = true;
    		}
    	}
    }    

    /**
     * Returns the operating system.
     */
    public static String getOS() {
        return System.getProperty("os.name");
    }

    /**
     * Returns the operating system version.
     */
    public static String getOSVersion() {
        return System.getProperty("os.version");
    }

    /**
     * Returns true if this is Windows NT or Windows 2000 and
     * hence can support a system tray feature.
     */
    public static boolean supportsTray() {
    	return _supportsTray;
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
     * Returns whether or not the OS is 2000 or XP.
     *
     * @return <tt>true</tt> if the application is running on 2000 or XP,
     *  <tt>false</tt> otherwise
     */
    public static boolean isWindows2000orXP() {
    	return _isWindows2000orXP;
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
     * Returns whether or not this operating system is considered
     * capable of meeting the requirements of a high load server.
     *
     * @return <tt>true</tt> if this OS meets high load server requirements,
     *         <tt>false</tt> otherwise
     */
    public static boolean isHighLoadOS() {
        return !(_isWindows98 || _isWindows95 || _isWindowsMe || _isWindowsNT);
    }

    /**
     * @return true if this is a well-supported version of windows.
     * (not 95, 98, nt or me)
     */
    public static boolean isGoodWindows() {
    	return isWindows() && isHighLoadOS();
    }

    /**
     * Return whether the current operating system supports moving files
     * to the trash. 
     */
    public static boolean supportsTrash() {
        return isWindows() || isMacOSX();
    }
}
