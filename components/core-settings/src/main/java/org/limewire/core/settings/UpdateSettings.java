package org.limewire.core.settings;

import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.StringSetSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for messages.
 */
public class UpdateSettings extends LimeProps {  
    private UpdateSettings() {}
    
    /**
     * Delay for showing message updates, in milliseconds.
     */
    public static final LongSetting UPDATE_DELAY =
        FACTORY.createRemoteLongSetting("UPDATE_DELAY", 24*60*60*1000,
            "updateDelay", 7*60*60*1000, 5*24*60*60*1000);
            
    /**
     * Delay for downloading updates, in milliseconds.
     */
    public static final LongSetting UPDATE_DOWNLOAD_DELAY =
        FACTORY.createRemoteLongSetting("UPDATE_DOWNLOAD_DELAY", 60*60*1000,
            "updateDownloadDelay", 30*60*1000, 77*60*60*1000);
    
    /**
     * How often to retry download any updates, in milliseconds.
     */
    public static final LongSetting UPDATE_RETRY_DELAY = 
        FACTORY.createRemoteLongSetting("UPDATE_RETRY_DELAY",30 * 60 * 1000,
                "updateRetryDelay", 15 * 60 * 1000, 2 * 60 * 60 * 1000); 
    
    /**
     * If this many times the initial delay passed since the update timestamp, we may
     * give up.
     */
    public static final IntSetting UPDATE_GIVEUP_FACTOR =
        FACTORY.createRemoteIntSetting("UPDATE_GIVEUP_FACTOR", 5, 
                "updateGiveUpFactor", 2, 50);
    
    /**
     * If we try downloading a given update more than this many times, we may give up.
     */
    public static final IntSetting UPDATE_MIN_ATTEMPTS =
        FACTORY.createRemoteIntSetting("UPDATE_MIN_ATTEMPTS", 500,
                "updateMinAttempts", 50, 2000);
            
    /**
     * The style of updates.
     */
    public static final IntSetting UPDATE_STYLE = 
        FACTORY.createIntSetting("UPDATE_STYLE", 1).setAlwaysSave(true);
    
    /**
     * Failed updates.
     */
    public static final StringSetSetting FAILED_UPDATES = 
        FACTORY.createStringSetSetting("FAILED_UPDATES","");
    
    /**
     * The timestamp of the last update message we've received.
     */
    public static final LongSetting LAST_UPDATE_TIMESTAMP = 
        FACTORY.createLongSetting("LAST_UPDATE_TIMESTAMP", -1L);
    
    /**
     * The last time we checked the failover url for updates.
     */
    public static final LongSetting LAST_HTTP_FAILOVER = 
        FACTORY.createLongSetting("LAST_HTTP_FAILOVER", -1L);
    
    public static final LongSetting LAST_SIMPP_FAILOVER =
        FACTORY.createLongSetting("LAST_SIMPP_FAILOVER", -1);
    
    /**
     * command to execute to install the updates.
     */
    public static final StringSetting AUTO_UPDATE_COMMAND = 
        FACTORY.createStringSetting("AUTO_UPDATE_COMMAND", "");
    
    /**
     * latest limewire version available.
     */
    public static final StringSetting AUTO_UPDATE_VERSION = 
        FACTORY.createRemoteStringSetting("AUTO_UPDATE_VERSION", "none", "AUTO_UPDATE_VERSION");
    
    /**
     * guid prefix for which updates are available.
     */
    public static final StringSetting AUTO_UPDATE_PREFIX =
        FACTORY.createRemoteStringSetting("AUTO_UPDATE_PREFIX", "", "AUTO_UPDATE_PREFIX");
    
    /**
     * max retry attempts for auto-update after which limewire will close and force 
     * upload on next launch
     */
    public static final IntSetting AUTO_UPDATE_MAX_ATTEMPTS = 
        FACTORY.createRemoteIntSetting("AUTO_UPDATE_MAX_ATTEMPTS", 5, "AUTO_UPDATE_MAX_ATTEMPTS", 1, 10);
    
    /**
     * URL location for update xml
     */
    public static final StringSetting AUTO_UPDATE_XML_URL =
        FACTORY.createRemoteStringSetting("AUTO_UPDATE_XML_URL", "http://127.0.0.1/update.xml", "AUTO_UPDATE_XML_URL");
}
