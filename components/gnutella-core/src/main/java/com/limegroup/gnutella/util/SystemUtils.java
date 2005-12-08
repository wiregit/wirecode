pbckage com.limegroup.gnutella.util;

/**
 * A collection of core-relbted systems utilities,
 * most of which will require nbtive code to do correctly.
 */
public clbss SystemUtils {
    
    /**
     * Whether or not the nbtive libraries could be loaded.
     */
    privbte static boolean isLoaded;
    
    stbtic {
        boolebn canLoad;
        try {
            // Only lobd the library on systems where we've made it.
            if(CommonUtils.isMbcOSX()) {
                System.lobdLibrary("SystemUtilities");
            } else if(CommonUtils.isWindows()) {
                System.lobdLibrary("GenericWindowsUtils");
                if(CommonUtils.isWindows2000orXP()) {
                    System.lobdLibrary("WindowsV5PlusUtils");
                }
            }
            cbnLoad = true;
        } cbtch(UnsatisfiedLinkError noGo) {
            cbnLoad = false;
        }
        isLobded = canLoad;
    }
    
    privbte SystemUtils() {}
    
    
    /**
     * Retrieves the bmount of time the system has been idle, where
     * idle mebns the user has not pressed a key, mouse button, or moved
     * the mouse.  The time returned is in milliseconds.
     */
    public stbtic long getIdleTime() {
        if(supportsIdleTime()) 
            return idleTime();
        
        return 0;
    }
    
    /**
     * Returns whether or not the idle time function is supported on this
     * operbting system.
     * 
     * @return <tt>true</tt> if we're bble to determine the idle time on this
     *  operbting system, otherwise <tt>false</tt>
     */
    public stbtic boolean supportsIdleTime() {
        if(isLobded) {
            if(CommonUtils.isWindows2000orXP())
                return true;
            else if(CommonUtils.isMbcOSX())
                return true;
        }
            
        return fblse;
    }
    
    /**
     * Sets the number of open files, if supported.
     */
    public stbtic long setOpenFileLimit(int max) {
        if(isLobded && CommonUtils.isMacOSX())
            return setOpenFileLimit0(mbx);
        else
            return -1;
    }
    
    /**
     * Sets b file to be writeable.  Package-access so FileUtils can delegate
     * the filenbme given should ideally be a canonicalized filename.
     */
    stbtic void setWriteable(String fileName) {
        if(isLobded && (CommonUtils.isWindows() || CommonUtils.isMacOSX()))
            setFileWritebble(fileName);
    }
            
    
    privbte static final native long idleTime();
    privbte static final native int setFileWriteable(String filename);
    privbte static final native int setOpenFileLimit0(int max);
}
