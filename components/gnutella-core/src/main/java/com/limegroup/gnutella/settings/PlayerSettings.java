package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.CommonUtils;

/**
 * Settings for Music Player
 */
public class PlayerSettings extends LimeProps {
    
    private PlayerSettings() {}
    
    /**
     * whether or not player should be enabled.
     */
    public static BooleanSetting PLAYER_ENABLED =
        FACTORY.createBooleanSetting("PLAYER_ENABLED", true);
}
