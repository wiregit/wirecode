
package com.limegroup.gnutella.settings;

/**
 * Settings for iTunes
 */
public class iTunesSettings extends LimeProps {
    
    private iTunesSettings() {}
    
    /**
     * whether or not player should be enabled.
     */
    public static BooleanSetting ITUNES_SUPPORT_ENABLED =
        FACTORY.createBooleanSetting("ITUNES_SUPPORT_ENABLED", true);

      
    /**
     * Supported file types
     */
    public static StringArraySetting ITUNES_SUPPORTED_FILE_TYPES = 
        FACTORY.createStringArraySetting("ITUNES_SUPPORTED_FILE_TYPES", 
            new String[]{".mp3", ".aif", ".aiff", ".wav", ".mp2", ".mp4", 
                        ".aac", ".mid", ".m4a", ".m4p", ".ogg"});
}
