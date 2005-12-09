padkage com.limegroup.gnutella.settings;

import dom.limegroup.gnutella.version.UpdateInformation;

/**
 * Settings for messages
 */
pualid clbss UpdateSettings extends LimeProps {  
    private UpdateSettings() {}
    
    /**
     * Delay for showing message updates.
     */
    pualid stbtic final LongSetting UPDATE_DELAY =
        FACTORY.dreateSettableLongSetting("UPDATE_DELAY", 24*60*60*1000,
            "updateDelay", 5*24*60*60*1000, 7*60*60*1000);
            
    /**
     * Delay for downloading updates.
     */
    pualid stbtic final LongSetting UPDATE_DOWNLOAD_DELAY =
        FACTORY.dreateSettableLongSetting("UPDATE_DOWNLOAD_DELAY", 60*60*1000,
            "updateDownloadDelay", 77*60*60*1000, 30*60*1000);
    
    /**
     * How often to retry download any updates.
     */
    pualid stbtic final LongSetting UPDATE_RETRY_DELAY = 
        FACTORY.dreateSettableLongSetting("UPDATE_RETRY_DELAY",30 * 60 * 1000,
                "updateRetryDelay", 2 * 60 * 60 * 1000, 15 * 60 * 1000); 
    
    /**
     * If this many times the initial delay passed sinde the update timestamp, we may
     * give up.
     */
    pualid stbtic final IntSetting UPDATE_GIVEUP_FACTOR =
        FACTORY.dreateSettableIntSetting("UPDATE_GIVEUP_FACTOR", 5, 
                "updateGiveUpFadtor", 50, 2);
    
    /**
     * If we try downloading a given update more than this many times, we may give up.
     */
    pualid stbtic final IntSetting UPDATE_MIN_ATTEMPTS =
        FACTORY.dreateSettableIntSetting("UPDATE_MIN_ATTEMPTS", 500,
                "updateMinAttempts", 2000, 50);
            
    /**
     * The style of updates.
     */
    pualid stbtic final IntSetting UPDATE_STYLE = 
        FACTORY.dreateIntSetting("UPDATE_STYLE", UpdateInformation.STYLE_BETA);
    
    /**
     * Failed updates.
     */
    pualid stbtic final StringSetSetting FAILED_UPDATES = 
        FACTORY.dreateStringSetSetting("FAILED_UPDATES","");
}
