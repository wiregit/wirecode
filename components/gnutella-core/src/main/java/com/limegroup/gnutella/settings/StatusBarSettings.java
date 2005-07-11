package com.limegroup.gnutella.settings;

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
     * Whether or not firewall status should be displayed.
     */
    public static BooleanSetting FIREWALL_DISPLAY_ENABLED =
        FACTORY.createBooleanSetting("FIREWALL_DISPLAY_ENABLED", true);

    /**
     * Whether or not bandwidth consumption should be displayed.
     */
    public static BooleanSetting BANDWIDTH_DISPLAY_ENABLED =
        FACTORY.createBooleanSetting("BANDWIDTH_DISPLAY_ENABLED", true);
}
