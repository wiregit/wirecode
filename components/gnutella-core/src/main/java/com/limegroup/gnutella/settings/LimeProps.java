package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.SettingsManager;

/**
 * The LimeWire.props settings class.  All settings that want to save
 * to LimeWire.props should get the instance of this class.
 */
public final class LimeProps extends AbstractSettings {
        
    private static final LimeProps INSTANCE = new LimeProps();
    
    private LimeProps() {
        super("limewire.props", "LimeWire properties file");
    }
    
    public static LimeProps instance() { return INSTANCE; }
    
    /**
     * We must revert settings from the factory & legacy ones
     * from SettingsManager.
     */
    public void revertToDefault() {
        super.revertToDefault();
        SettingsManager.instance().loadDefaults();
    }
}