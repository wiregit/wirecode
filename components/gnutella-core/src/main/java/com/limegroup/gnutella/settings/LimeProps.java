package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.SettingsManager;

/**
 * Handler for all 'LimeWire.props' settings.  Classes such
 * as SearchSettings, ConnectionSettings, etc... should retrieve
 * the factory via LimeProps.instance().getFactory() and add
 * settings to that factory.
 */
public final class LimeProps extends AbstractSettings {
        
    private static final LimeProps INSTANCE = new LimeProps();
    
    private LimeProps() {
        super("limewire.props", "LimeWire properties file");
        SettingsHandler.addSettings(this);
    }
    
    /**
     * Returns the only instance of this class.
     */
    public static LimeProps instance() { return INSTANCE; }
    
    /**
     * Overriden to revert to SettingsManager defaults as well
     * as the factory defaults.
     */
    public void revertToDefault() {
        super.revertToDefault();
        SettingsManager.instance().loadDefaults();
    }
}