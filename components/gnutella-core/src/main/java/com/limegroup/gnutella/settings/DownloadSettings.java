package com.limegroup.gnutella.settings;

/**
 * Settings for downloads
 */
public class DownloadSettings extends LimeProps {
    private DownloadSettings() {}
                                                        
	/**
	 * Setting for the number of bytes/second to allow for all uploads.
	 */
	public static final IntSetting DOWNLOAD_SPEED =
		FACTORY.createIntSetting("DOWNLOAD_SPEED", 100);
    
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
        FACTORY.createIntSetting("MAX_SIM_DOWNLOAD", 10);
    
    /**
     * Enable/disable skipping of acks
     */
    public static final BooleanSetting SKIP_ACKS =
        FACTORY.createSettableBooleanSetting("SKIP_ACKS",true,"skip_acks");
    
    /**
     * various parameters of the formulas for skipping acks.
     */
    public static final IntSetting MAX_SKIP_ACKS =
        FACTORY.createSettableIntSetting("MAX_SKIP_ACKS",5,"max_skip_ack",15,2);
    
    public static final FloatSetting DEVIATION =
        FACTORY.createSettableFloatSetting("SKIP_DEVIATION",1.3f,"skip_deviation",2.0f,1.0f);
    
    public static final IntSetting PERIOD_LENGTH =
        FACTORY.createSettableIntSetting("PERIOD_LENGTH",500,"period_length",2000,100);
    
    public static final IntSetting HISTORY_SIZE=
        FACTORY.createSettableIntSetting("HISTORY_SIZE",10,"history_size",50,2);
    
}
