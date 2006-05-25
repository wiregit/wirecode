package com.limegroup.gnutella.util;

import java.awt.Component;

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
                System.loadLibrary("SystemUtilities");
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
     * Gets the value of the Win32 HWND identifier of the native window that backs a given frame.
     * 
     * @param c The AWT Component, like a JFrame, that is backed by a native window
     * @param s The path to the Java bin folder we're running on, like "C:\Program Files\Java\jre1.5.0_05\bin"
     * @return  The Win32 HWND handle value
     */
    public static final int getWindowHandle(Component c, String s) {
    	return getWindowHandleNative(c, s);
    }

    /**
     * Changes the icon of a window.
     * 
     * Puts the given icon in the title bar, task bar, and Alt+Tab box.
     * Replaces the Swing icon with a real Windows .ico that supports multiple sizes, full color, and partially transparent pixels.
     * 
     * @param handle The Win32 HWND handle value
     * @param path   The path to a .ico file on the disk
     * @return       False on error
     */
    public static final boolean setWindowIcon(int handle, String path) {
    	return setWindowIconNative(handle, path);
    }

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
    public static final boolean isFirewallPresent() {
    	if (CommonUtils.isWindows() && isLoaded)
    		return firewallPresentNative();
    	return false;
    }

    /**
     * Determine if the Windows Firewall is enabled.
     * 
     * @return True if the setting on the "General" tab is "On (recommended)".
     *         False if the setting on the "General" tab is "Off (not recommended)".
     *         False on error.
     */
    public static final boolean isFirewallEnabled() {
    	if (CommonUtils.isWindows() && isLoaded)
    	    return firewallEnabledNative();
    	return false;
    }

    /**
     * Determine if the Windows Firewall is on with no exceptions.
     * 
     * @return True if the box on the "General" tab "Don't allow exceptions" is checked.
     *         False if the box is not checked.
     *         False on error.
     */
    public static final boolean isFirewallExceptionsNotAllowed() {
    	if (CommonUtils.isWindows() && isLoaded)
    		return firewallExceptionsNotAllowedNative();
    	return false;
    }

    /**
     * Determine if a program is listed on the Windows Firewall exceptions list.
     * 
     * @param path The path to the program, like "C:\Program Files\LimeWire\LimeWire.exe"
     * @return     True if it has a listing on the Exceptions list, false if not or on error
     */
    public static final boolean isProgramListedOnFirewall(String path) {
    	if (CommonUtils.isWindows() && isLoaded)
    		return firewallIsProgramListedNative(path);
    	return false;
    }

    /**
     * Determine if a program's listing on the Windows Firewall exceptions list has a check box making it enabled.
     * 
     * @param path The path to the program, like "C:\Program Files\LimeWire\LimeWire.exe"
     * @return     True if it's listing's check box is checked, false if not or on error
     */
    public static final boolean isProgramEnabledOnFirewall(String path) {
    	if (CommonUtils.isWindows() && isLoaded)
    		return firewallIsProgramEnabledNative(path);
    	return false;
    }

    /**
     * Add a program to the Windows Firewall exceptions list.
     * 
     * @param path The path to the program, like "C:\Program Files\LimeWire\LimeWire.exe"
     * @param name The name of the program, like "LimeWire", this is the text that will identify the item on the list
     * @return     False if error
     */
    public static final boolean addProgramToFirewall(String path, String name) {
    	if (CommonUtils.isWindows() && isLoaded)
    		return firewallAddNative(path, name);
    	return false;
    }

    /**
     * Remove a program from the Windows Firewall exceptions list.
     * 
     * @param path The path to the program, like "C:\Program Files\LimeWire\LimeWire.exe"
     * @return     False if error.
     */
    public static final boolean removeProgramFromFirewall(String path) {
    	if (CommonUtils.isWindows() && isLoaded)
    		return firewallRemoveNative(path);
    	return false;
    }

    private static final native int getWindowHandleNative(Component c, String s);
    private static final native boolean setWindowIconNative(int handle, String path);
    private static final native String getRunningPathNative();
    private static final native boolean firewallPresentNative();
    private static final native boolean firewallEnabledNative();
    private static final native boolean firewallExceptionsNotAllowedNative();
    private static final native boolean firewallIsProgramListedNative(String path);
    private static final native boolean firewallIsProgramEnabledNative(String path);
    private static final native boolean firewallAddNative(String path, String name);
    private static final native boolean firewallRemoveNative(String path);
}
