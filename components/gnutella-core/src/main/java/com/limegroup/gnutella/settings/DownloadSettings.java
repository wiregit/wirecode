package com.limegroup.gnutella.settings;

/**
 * Settings for downloads
 */
public class DownloadSettings extends LimeProps {
    private DownloadSettings() {}                                                        
    /**
	 * The maximum number of downstream bytes per second ever passed by
	 * this node.
	 */
    public static final IntSetting MAX_DOWNLOAD_BYTES_PER_SEC =
        FACTORY.createExpirableIntSetting("MAX_DOWNLOAD_BYTES_PER_SEC", 0);
    
    /**
	 * The maximum number of simultaneous downloads to allow.
	 */
    public static final IntSetting MAX_SIM_DOWNLOAD =
        FACTORY.createIntSetting("MAX_SIM_DOWNLOAD", 4); 
}