package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.SettingsManager;

/**
 * Handler for all 'LimeWire.props' settings.  Classes such
 * as SearchSettings, ConnectionSettings, etc... should retrieve
 * the factory via LimeProps.instance().getFactory() and add
 * settings to that factory.
 */
public class LimeProps extends AbstractSettings {
        
    private static final LimeProps INSTANCE = new LimeProps();
    
    // The FACTORY is used for subclasses of LimeProps, so they know
    // which factory to add classes to.
    protected static final SettingsFactory FACTORY = INSTANCE.getFactory();
    
    // This is protected so that subclasses can extend from it, but
    // subclasses should NEVER instantiate a copy themselves.
    protected LimeProps() {
        super("limewire.props", "LimeWire properties file");
        Assert.that( getClass() == LimeProps.class,
            "should not have a subclass instantiate");
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