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
     * Delay for downloading updates.
     */
    public static final LongSetting UPDATE_DOWNLOAD_DELAY =
        FACTORY.createSettableLongSetting("UPDATE_DOWNLOAD_DELAY", 60*60*1000,
            "updateDownloadDelay", 77*60*60*1000, 30*60*1000);
    
    /**
     * How often to retry download any updates.
     */
    public static final LongSetting UPDATE_RETRY_DELAY = 
        FACTORY.createSettableLongSetting("UPDATE_RETRY_DELAY",1 * 60 * 1000,
                "updateRetryDelay", 2 * 60 * 60 * 1000, 1 * 60 * 1000); 
            
    /**
     * The style of updates.
     */
    public static final IntSetting UPDATE_STYLE = 
        FACTORY.createIntSetting("UPDATE_STYLE", UpdateInformation.STYLE_MAJOR);
}
