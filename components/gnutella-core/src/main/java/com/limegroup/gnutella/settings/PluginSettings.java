package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;

public class PluginSettings extends LimeProps {
    
    private PluginSettings() {}
    
    public static final BooleanSetting CACHE_PROFILE_DIR
        = FACTORY.createBooleanSetting("CACHE_PROFILE_DIR", true);
}
