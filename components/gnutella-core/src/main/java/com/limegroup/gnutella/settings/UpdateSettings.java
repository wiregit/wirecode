package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.version.UpdateInformation;

/**
 * Settings for messages
 */
public class UpdateSettings extends LimeProps {  
    private UpdateSettings() {}
    
    /**
     * Delay for showing message updates.
     */
    public static final LongSetting UPDATE_DELAY =
        FACTORY.createSettableLongSetting("UPDATE_DELAY", 24*60*60*1000,
            "updateDelay", 5*24*60*60*1000, 7*60*60*1000);
            
    /**
     * The style of updates.
     */
    public static final IntSetting UPDATE_STYLE = 
        FACTORY.createIntSetting("UPDATE_STYLE", UpdateInformation.STYLE_MAJOR);
}
