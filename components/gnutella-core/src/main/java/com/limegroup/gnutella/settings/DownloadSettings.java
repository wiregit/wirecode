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
    
    /**
     * Whether the client should use HeadPings when ranking sources
     */
    public static final BooleanSetting USE_HEADPINGS =
        FACTORY.createSettableBooleanSetting("USE_HEADPINGS",true,"use_headpings");
    
    /**
     * Whether the client should drop incoming HeadPings.
     */
    public static final BooleanSetting DROP_HEADPINGS =
        FACTORY.createSettableBooleanSetting("DROP_HEADPINGS",false,"drop_headpings");
    
    /**
     * We should stop issuing HeadPings when we have this many verified sources
     */
    public static final IntSetting MAX_VERIFIED_HOSTS = 
        FACTORY.createSettableIntSetting("MAX_VERIFIED_HOSTS",1,"max_verified_hosts",5,0);
    
    /**
     * We should not schedule more than this many head pings at once
     */
    public static final IntSetting PING_BATCH =
        FACTORY.createSettableIntSetting("PING_BATCH",10,"PingRanker.pingBatch",50,1);
    
    /**
     * Do not start new workers more than this often
     */
    public static final IntSetting WORKER_INTERVAL =
        FACTORY.createSettableIntSetting("WORKER_INTERVAL",2000,"ManagedDownloader.workerInterval",20000,1);
    
    /**
     * Use a download SelectionStrategy tailored for previewing if the file's extension is
     * in this list.
     */
    private static String[] defaultPreviewableExtensions = {"html", "htm", "xml", "txt", "rtf", "tex",
        "mp3", "mp4", "wav", "au", "aif", "aiff", "ra", "ram", "wma", "wmv", "midi", "aifc", "snd",
        "mpg", "mpeg", "asf", "qt", "mov", "avi", "mpe", "ogg", "rm", "m4a", "flac", "fla"};
    public static final StringArraySetting PREVIEWABLE_EXTENSIONS = 
        FACTORY.createSettableStringArraySetting("PREVIEWABLE_EXTENSIONS", 
                defaultPreviewableExtensions,
                "PREVIEWABLE_EXTENSIONS");
}
