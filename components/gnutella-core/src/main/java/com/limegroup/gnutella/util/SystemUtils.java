package com.limegroup.gnutella.util;

/**
 * A collection of core-related systems utilities,
 * most of which will require native code to do correctly.
 */
public class SystemUtils {
    
    static {
        // Only load the library on systems where we've made it.
        if(CommonUtils.isWindows() || CommonUtils.isMacOSX())
            System.loadLibrary("SystemUtilities");
    }
    
    private SystemUtils() {}
    
    
    /**
     * Retrieves the amount of time the system has been idle, where
     * idle means the user has not pressed a key, mouse button, or moved
     * the mouse.  The time returned is in milliseconds.
     */
    public static long getIdleTime() {
        if(CommonUtils.isWindows2000orXP())
            return idleTime();
        else if(CommonUtils.isMacOSX())
            return idleTime();
        else
            return 0;
    }
    
    private static final native long idleTime();
}