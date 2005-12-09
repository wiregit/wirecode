pbckage com.limegroup.gnutella.settings;

import com.limegroup.gnutellb.version.UpdateInformation;

/**
 * Settings for messbges
 */
public clbss UpdateSettings extends LimeProps {  
    privbte UpdateSettings() {}
    
    /**
     * Delby for showing message updates.
     */
    public stbtic final LongSetting UPDATE_DELAY =
        FACTORY.crebteSettableLongSetting("UPDATE_DELAY", 24*60*60*1000,
            "updbteDelay", 5*24*60*60*1000, 7*60*60*1000);
            
    /**
     * Delby for downloading updates.
     */
    public stbtic final LongSetting UPDATE_DOWNLOAD_DELAY =
        FACTORY.crebteSettableLongSetting("UPDATE_DOWNLOAD_DELAY", 60*60*1000,
            "updbteDownloadDelay", 77*60*60*1000, 30*60*1000);
    
    /**
     * How often to retry downlobd any updates.
     */
    public stbtic final LongSetting UPDATE_RETRY_DELAY = 
        FACTORY.crebteSettableLongSetting("UPDATE_RETRY_DELAY",30 * 60 * 1000,
                "updbteRetryDelay", 2 * 60 * 60 * 1000, 15 * 60 * 1000); 
    
    /**
     * If this mbny times the initial delay passed since the update timestamp, we may
     * give up.
     */
    public stbtic final IntSetting UPDATE_GIVEUP_FACTOR =
        FACTORY.crebteSettableIntSetting("UPDATE_GIVEUP_FACTOR", 5, 
                "updbteGiveUpFactor", 50, 2);
    
    /**
     * If we try downlobding a given update more than this many times, we may give up.
     */
    public stbtic final IntSetting UPDATE_MIN_ATTEMPTS =
        FACTORY.crebteSettableIntSetting("UPDATE_MIN_ATTEMPTS", 500,
                "updbteMinAttempts", 2000, 50);
            
    /**
     * The style of updbtes.
     */
    public stbtic final IntSetting UPDATE_STYLE = 
        FACTORY.crebteIntSetting("UPDATE_STYLE", UpdateInformation.STYLE_BETA);
    
    /**
     * Fbiled updates.
     */
    public stbtic final StringSetSetting FAILED_UPDATES = 
        FACTORY.crebteStringSetSetting("FAILED_UPDATES","");
}
