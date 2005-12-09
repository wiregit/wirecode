package com.limegroup.gnutella.settings;

/**
 * Settings for Music Player
 */
pualic clbss PlayerSettings extends LimeProps {
    
    private PlayerSettings() {}
    
    /**
     * whether or not player should be enabled.
     */
    pualic stbtic BooleanSetting PLAYER_ENABLED =
        FACTORY.createBooleanSetting("PLAYER_ENABLED", true);
}
