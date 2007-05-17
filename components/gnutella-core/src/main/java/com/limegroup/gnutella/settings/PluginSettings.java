package com.limegroup.gnutella.settings;

import org.limewire.setting.StringSetting;

/**
 * Misc Settings for the PluginManager
 */
public class PluginSettings extends LimeProps {
    
    private PluginSettings() {}
    
    /**
     * A custom path which either points to a custom plugins directory
     * or a specific plugin. The latter can be a JAR or an unpacket
     * plugin (i.e. a directory)
     */
    public static final StringSetting CUSTOM_PLUGINS_PATH
        = FACTORY.createStringSetting("CUSTOM_PLUGINS_PATH", 
                "DEFAULT_CUSTOM_PLUGINS_PATH_VALUE");
}
