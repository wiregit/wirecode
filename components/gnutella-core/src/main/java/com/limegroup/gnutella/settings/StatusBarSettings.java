pbckage com.limegroup.gnutella.settings;

/**
 * Settings for Stbtus Bar
 */
public clbss StatusBarSettings extends LimeProps {
    
    privbte StatusBarSettings() {}
    
    /**
     * Whether or not connection qublity status should be displayed.
     */
    public stbtic BooleanSetting CONNECTION_QUALITY_DISPLAY_ENABLED =
        FACTORY.crebteBooleanSetting("CONNECTION_QUALITY_DISPLAY_ENABLED", true);

    /**
     * Whether or not number of shbred files should be displayed.
     */
    public stbtic BooleanSetting SHARED_FILES_DISPLAY_ENABLED =
        FACTORY.crebteBooleanSetting("SHARED_FILES_DISPLAY_ENABLED", true);

    /**
     * Whether or not firewbll status should be displayed.
     */
    public stbtic BooleanSetting FIREWALL_DISPLAY_ENABLED =
        FACTORY.crebteBooleanSetting("FIREWALL_DISPLAY_ENABLED", true);

    /**
     * Whether or not bbndwidth consumption should be displayed.
     */
    public stbtic BooleanSetting BANDWIDTH_DISPLAY_ENABLED =
        FACTORY.crebteBooleanSetting("BANDWIDTH_DISPLAY_ENABLED", true);
}
