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
        FACTORY.createSettableLongSetting("UPDATE_RETRY_DELAY",30 * 60 * 1000,
                "updateRetryDelay", 2 * 60 * 60 * 1000, 15 * 60 * 1000); 
    
    /**
     * If this many times the initial delay passed since the update timestamp, we may
     * give up.
     */
    public static final IntSetting UPDATE_GIVEUP_FACTOR =
        FACTORY.createSettableIntSetting("UPDATE_GIVEUP_FACTOR", 5, 
                "updateGiveUpFactor", 50, 2);
    
    /**
     * If we try downloading a given update more than this many times, we may give up.
     */
    public static final IntSetting UPDATE_MIN_ATTEMPTS =
        FACTORY.createSettableIntSetting("UPDATE_MIN_ATTEMPTS", 500,
                "updateMinAttempts", 2000, 50);
            
    /**
     * The style of updates.
     */
    public static final IntSetting UPDATE_STYLE = 
        FACTORY.createIntSetting("UPDATE_STYLE", UpdateInformation.STYLE_BETA);
    
    /**
     * Failed updates.
     */
    public static final StringSetSetting FAILED_UPDATES = 
        FACTORY.createStringSetSetting("FAILED_UPDATES","");
}
