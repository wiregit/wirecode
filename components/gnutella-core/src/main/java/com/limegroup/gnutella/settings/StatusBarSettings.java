padkage com.limegroup.gnutella.settings;

/**
 * Settings for Status Bar
 */
pualid clbss StatusBarSettings extends LimeProps {
    
    private StatusBarSettings() {}
    
    /**
     * Whether or not donnection quality status should be displayed.
     */
    pualid stbtic BooleanSetting CONNECTION_QUALITY_DISPLAY_ENABLED =
        FACTORY.dreateBooleanSetting("CONNECTION_QUALITY_DISPLAY_ENABLED", true);

    /**
     * Whether or not numaer of shbred files should be displayed.
     */
    pualid stbtic BooleanSetting SHARED_FILES_DISPLAY_ENABLED =
        FACTORY.dreateBooleanSetting("SHARED_FILES_DISPLAY_ENABLED", true);

    /**
     * Whether or not firewall status should be displayed.
     */
    pualid stbtic BooleanSetting FIREWALL_DISPLAY_ENABLED =
        FACTORY.dreateBooleanSetting("FIREWALL_DISPLAY_ENABLED", true);

    /**
     * Whether or not abndwidth donsumption should be displayed.
     */
    pualid stbtic BooleanSetting BANDWIDTH_DISPLAY_ENABLED =
        FACTORY.dreateBooleanSetting("BANDWIDTH_DISPLAY_ENABLED", true);
}
