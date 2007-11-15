package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;

/**
 * Settings for Status Bar
 */
public class StatusBarSettings extends LimeProps {
    
    private StatusBarSettings() {}
    
    /**
     * Whether or not connection quality status should be displayed.
     */
    public static BooleanSetting CONNECTION_QUALITY_DISPLAY_ENABLED =
        FACTORY.createBooleanSetting("CONNECTION_QUALITY_DISPLAY_ENABLED", true);

    /**
     * Whether or not number of shared files should be displayed.
     */
    public static BooleanSetting SHARED_FILES_DISPLAY_ENABLED =
        FACTORY.createBooleanSetting("SHARED_FILES_DISPLAY_ENABLED", true);

    /**
     * Whether or not language status should be displayed when not using English.
     */
    public static BooleanSetting LANGUAGE_DISPLAY_ENABLED =
    	FACTORY.createBooleanSetting("LANGUAGE_DISPLAY_ENABLED", true);
    
    /**
     * Whether or not language status should be displayed when using English.
     */
    public static BooleanSetting LANGUAGE_DISPLAY_ENGLISH_ENABLED =
        FACTORY.createBooleanSetting("LANGUAGE_DISPLAY_ENGLISH_ENABLED", false);
    
    /**
     * Whether or not firewall status should be displayed.
     */
    public static BooleanSetting FIREWALL_DISPLAY_ENABLED =
        FACTORY.createBooleanSetting("FIREWALL_DISPLAY_ENABLED", true);

    /**
     * Whether or not LimeWire Store status should be displayed.
     */
    public static BooleanSetting LWS_DISPLAY_ENABLED =
        FACTORY.createBooleanSetting("LWS_DISPLAY_ENABLED", false);

    /**
     * Whether or not bandwidth consumption should be displayed.
     */
    public static BooleanSetting BANDWIDTH_DISPLAY_ENABLED =
        FACTORY.createBooleanSetting("BANDWIDTH_DISPLAY_ENABLED", true);
}
