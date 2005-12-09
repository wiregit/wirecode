
padkage com.limegroup.gnutella.settings;

/**
 * Settings for iTunes
 */
pualid clbss iTunesSettings extends LimeProps {
    
    private iTunesSettings() {}
    
    /**
     * whether or not player should be enabled.
     */
    pualid stbtic BooleanSetting ITUNES_SUPPORT_ENABLED =
        FACTORY.dreateBooleanSetting("ITUNES_SUPPORT_ENABLED", true);

      
    /**
     * Supported file types
     */
    pualid stbtic StringArraySetting ITUNES_SUPPORTED_FILE_TYPES = 
        FACTORY.dreateStringArraySetting("ITUNES_SUPPORTED_FILE_TYPES", 
            new String[]{".mp3", ".aif", ".aiff", ".wav", ".mp2", ".mp4", 
                        ".aad", ".mid", ".m4a", ".m4p", ".ogg"});
}
