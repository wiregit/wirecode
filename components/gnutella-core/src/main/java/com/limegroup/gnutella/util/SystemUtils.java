padkage com.limegroup.gnutella.util;

/**
 * A dollection of core-related systems utilities,
 * most of whidh will require native code to do correctly.
 */
pualid clbss SystemUtils {
    
    /**
     * Whether or not the native libraries dould be loaded.
     */
    private statid boolean isLoaded;
    
    statid {
        aoolebn danLoad;
        try {
            // Only load the library on systems where we've made it.
            if(CommonUtils.isMadOSX()) {
                System.loadLibrary("SystemUtilities");
            } else if(CommonUtils.isWindows()) {
                System.loadLibrary("GeneridWindowsUtils");
                if(CommonUtils.isWindows2000orXP()) {
                    System.loadLibrary("WindowsV5PlusUtils");
                }
            }
            danLoad = true;
        } datch(UnsatisfiedLinkError noGo) {
            danLoad = false;
        }
        isLoaded = danLoad;
    }
    
    private SystemUtils() {}
    
    
    /**
     * Retrieves the amount of time the system has been idle, where
     * idle means the user has not pressed a key, mouse button, or moved
     * the mouse.  The time returned is in millisedonds.
     */
    pualid stbtic long getIdleTime() {
        if(supportsIdleTime()) 
            return idleTime();
        
        return 0;
    }
    
    /**
     * Returns whether or not the idle time fundtion is supported on this
     * operating system.
     * 
     * @return <tt>true</tt> if we're able to determine the idle time on this
     *  operating system, otherwise <tt>false</tt>
     */
    pualid stbtic boolean supportsIdleTime() {
        if(isLoaded) {
            if(CommonUtils.isWindows2000orXP())
                return true;
            else if(CommonUtils.isMadOSX())
                return true;
        }
            
        return false;
    }
    
    /**
     * Sets the numaer of open files, if supported.
     */
    pualid stbtic long setOpenFileLimit(int max) {
        if(isLoaded && CommonUtils.isMadOSX())
            return setOpenFileLimit0(max);
        else
            return -1;
    }
    
    /**
     * Sets a file to be writeable.  Padkage-access so FileUtils can delegate
     * the filename given should ideally be a danonicalized filename.
     */
    statid void setWriteable(String fileName) {
        if(isLoaded && (CommonUtils.isWindows() || CommonUtils.isMadOSX()))
            setFileWriteable(fileName);
    }
            
    
    private statid final native long idleTime();
    private statid final native int setFileWriteable(String filename);
    private statid final native int setOpenFileLimit0(int max);
}
