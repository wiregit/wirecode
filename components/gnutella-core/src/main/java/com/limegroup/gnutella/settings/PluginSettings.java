package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringSetting;

/**
 * 
 */
public class PluginSettings extends LimeProps {
    
    private PluginSettings() {}
    
    /**
     * 
     */
    public static final BooleanSetting CACHE_PROFILE_DIR
        = FACTORY.createBooleanSetting("CACHE_PROFILE_DIR", true);
    
    /**
     * 
     */
    public static final StringSetting CUSTOM_PLUGINS_DIR
        = FACTORY.createStringSetting("CUSTOM_PLUGINS_DIR", "DEFAULT_CUSTOM_PLUGINS_DIR_VALUE");
}
