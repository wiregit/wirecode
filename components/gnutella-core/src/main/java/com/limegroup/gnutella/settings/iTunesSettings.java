
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
}