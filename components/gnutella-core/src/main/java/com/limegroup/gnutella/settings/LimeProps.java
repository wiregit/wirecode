package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.*;

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

    public Setting getSetting(String key) {
        synchronized(FACTORY) {
            Iterator iter = FACTORY.iterator();
            while(iter.hasNext()) {
                Setting currSetting = (Setting)iter.next();
                if(currSetting.getKey().equals(key))
                    return currSetting;
            }
        }
        return null; //unable the find the setting we are looking for
    }

}
