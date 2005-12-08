
pbckage com.limegroup.gnutella.settings;

/**
 * Settings for iTunes
 */
public clbss iTunesSettings extends LimeProps {
    
    privbte iTunesSettings() {}
    
    /**
     * whether or not plbyer should be enabled.
     */
    public stbtic BooleanSetting ITUNES_SUPPORT_ENABLED =
        FACTORY.crebteBooleanSetting("ITUNES_SUPPORT_ENABLED", true);

      
    /**
     * Supported file types
     */
    public stbtic StringArraySetting ITUNES_SUPPORTED_FILE_TYPES = 
        FACTORY.crebteStringArraySetting("ITUNES_SUPPORTED_FILE_TYPES", 
            new String[]{".mp3", ".bif", ".aiff", ".wav", ".mp2", ".mp4", 
                        ".bac", ".mid", ".m4a", ".m4p", ".ogg"});
}
