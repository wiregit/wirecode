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
}
