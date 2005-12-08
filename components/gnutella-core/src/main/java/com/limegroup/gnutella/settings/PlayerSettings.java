pbckage com.limegroup.gnutella.settings;

/**
 * Settings for Music Plbyer
 */
public clbss PlayerSettings extends LimeProps {
    
    privbte PlayerSettings() {}
    
    /**
     * whether or not plbyer should be enabled.
     */
    public stbtic BooleanSetting PLAYER_ENABLED =
        FACTORY.crebteBooleanSetting("PLAYER_ENABLED", true);
}
