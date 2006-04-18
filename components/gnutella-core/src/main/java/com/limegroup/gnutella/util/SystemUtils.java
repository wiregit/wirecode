package com.limegroup.gnutella.util;

/**
 * A collection of core-related systems utilities,
 * most of which will require native code to do correctly.
 */
public class SystemUtils {
    
    /**
     * Whether or not the native libraries could be loaded.
     */
    private static boolean isLoaded;
    
    static {
        boolean canLoad;
        try {
            // Only load the library on systems where we've made it.
            if(CommonUtils.isMacOSX()) {
                System.loadLibrary("SystemUtilities");
            } else if(CommonUtils.isWindows()) {
                System.loadLibrary("GenericWindowsUtils");
                if(CommonUtils.isWindows2000orXP()) {
                    System.loadLibrary("WindowsV5PlusUtils");
                }
                System.loadLibrary("WindowsFirewall");
            }
            canLoad = true;
        } catch(UnsatisfiedLinkError noGo) {
            canLoad = false;
        }
        isLoaded = canLoad;
    }
    
    private SystemUtils() {}
    
    
    /**
     * Retrieves the amount of time the system has been idle, where
     * idle means the user has not pressed a key, mouse button, or moved
     * the mouse.  The time returned is in milliseconds.
     */
    public static long getIdleTime() {
        if(supportsIdleTime()) 
            return idleTime();
        
        return 0;
    }
    
    /**
     * Returns whether or not the idle time function is supported on this
     * operating system.
     * 
     * @return <tt>true</tt> if we're able to determine the idle time on this
     *  operating system, otherwise <tt>false</tt>
     */
    public static boolean supportsIdleTime() {
        if(isLoaded) {
            if(CommonUtils.isWindows2000orXP())
                return true;
            else if(CommonUtils.isMacOSX())
                return true;
        }
            
        return false;
    }
    
    /**
     * Sets the number of open files, if supported.
     */
    public static long setOpenFileLimit(int max) {
        if(isLoaded && CommonUtils.isMacOSX())
            return setOpenFileLimit0(max);
        else
            return -1;
    }
    
    /**
     * Sets a file to be writeable.  Package-access so FileUtils can delegate
     * the filename given should ideally be a canonicalized filename.
     */
    static void setWriteable(String fileName) {
        if(isLoaded && (CommonUtils.isWindows() || CommonUtils.isMacOSX()))
            setFileWriteable(fileName);
    }

    private static final native long idleTime();
    private static final native int setFileWriteable(String filename);
    private static final native int setOpenFileLimit0(int max);

	/**
	 * Get the path to the Windows launcher .exe file that is us running right now.
	 * 
	 * @return A String like "c:\Program Files\LimeWire\LimeWire.exe".
	 *         Blank on error.
	 */
    public static final String getRunningPath() {
    	if (CommonUtils.isWindows() && isLoaded)
    		return getRunningPathNative();
    	return "";
    }

    /**
     * Determine if this Windows computer has Windows Firewall on it.
     * 
     * @return True if it does, false if it does not or there was an error
     */
    public static final boolean isWindowsFirewallPresent() {
    	if (CommonUtils.isWindows() && isLoaded)
    		return windowsFirewallPresentNative();
    	return false;
    }

    /**
     * Determine if the Windows Firewall is enabled.
     * 
     * @return True if the setting on the "General" tab is "On (recommended)".
     *         False if the setting on the "General" tab is "Off (not recommended)".
     *         False on error.
     */
    public static final boolean isWindowsFirewallEnabled() {
    	if (CommonUtils.isWindows() && isLoaded)
    	    return windowsFirewallEnabledNative();
    	return false;
    }

    /**
     * Determine if the Windows Firewall is on with no exceptions.
     * 
     * @return True if the box on the "General" tab "Don't allow exceptions" is checked.
     *         False if the box is not checked.
     *         False on error.
     */
    public static final boolean areWindowsFirewallExceptionsNotAllowed() {
    	if (CommonUtils.isWindows() && isLoaded)
    		return windowsFirewallExceptionsNotAllowedNative();
    	return false;
    }

    /**
     * Determine if a program is listed on the Windows Firewall exceptions list.
     * 
     * @param path The path to the program, like "C:\Program Files\LimeWire\LimeWire.exe"
     * @return     True if it has a listing on the Exceptions list, false if not or on error
     */
    public static final boolean isProgramListedOnWindowsFirewall(String path) {
    	if (CommonUtils.isWindows() && isLoaded)
    		return windowsFirewallIsProgramListedNative(path);
    	return false;
    }

    /**
     * Determine if a program's listing on the Windows Firewall exceptions list has a check box making it enabled.
     * 
     * @param path The path to the program, like "C:\Program Files\LimeWire\LimeWire.exe"
     * @return     True if it's listing's check box is checked, false if not or on error
     */
    public static final boolean isProgramEnabledOnWindowsFirewall(String path) {
    	if (CommonUtils.isWindows() && isLoaded)
    		return windowsFirewallIsProgramEnabledNative(path);
    	return false;
    }

    /**
     * Add a program to the Windows Firewall exceptions list.
     * 
     * @param path The path to the program, like "C:\Program Files\LimeWire\LimeWire.exe"
     * @param name The name of the program, like "LimeWire", this is the text that will identify the item on the list
     * @return     False if error
     */
    public static final boolean addProgramToWindowsFirewall(String path, String name) {
    	if (CommonUtils.isWindows() && isLoaded)
    		return windowsFirewallAddNative(path, name);
    	return false;
    }

    /**
     * Remove a program from the Windows Firewall exceptions list.
     * 
     * @param path The path to the program, like "C:\Program Files\LimeWire\LimeWire.exe"
     * @return     False if error.
     */
    public static final boolean removeProgramFromWindowsFirewall(String path) {
    	if (CommonUtils.isWindows() && isLoaded)
    		return windowsFirewallRemoveNative(path);
    	return false;
    }

    // Native methods implemented in C++ code in WindowsFirewall.dll
    private static final native String getRunningPathNative();
    private static final native boolean windowsFirewallPresentNative();
    private static final native boolean windowsFirewallEnabledNative();
    private static final native boolean windowsFirewallExceptionsNotAllowedNative();
    private static final native boolean windowsFirewallIsProgramListedNative(String path);
    private static final native boolean windowsFirewallIsProgramEnabledNative(String path);
    private static final native boolean windowsFirewallAddNative(String path, String name);
    private static final native boolean windowsFirewallRemoveNative(String path);
}
