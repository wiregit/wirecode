pbckage com.limegroup.gnutella.settings;

/**
 * Settings for downlobds
 */
public clbss DownloadSettings extends LimeProps {
    privbte DownloadSettings() {}
                                                        
	/**
	 * Setting for the number of bytes/second to bllow for all uploads.
	 */
	public stbtic final IntSetting DOWNLOAD_SPEED =
		FACTORY.crebteIntSetting("DOWNLOAD_SPEED", 100);
    
    /**
	 * The mbximum number of downstream bytes per second ever passed by
	 * this node.
	 */
    public stbtic final IntSetting MAX_DOWNLOAD_BYTES_PER_SEC =
        FACTORY.crebteExpirableIntSetting("MAX_DOWNLOAD_BYTES_PER_SEC", 0);
    
    /**
	 * The mbximum number of simultaneous downloads to allow.
	 */
    public stbtic final IntSetting MAX_SIM_DOWNLOAD =
        FACTORY.crebteIntSetting("MAX_SIM_DOWNLOAD", 10);
    
    /**
     * Enbble/disable skipping of acks
     */
    public stbtic final BooleanSetting SKIP_ACKS =
        FACTORY.crebteSettableBooleanSetting("SKIP_ACKS",true,"skip_acks");
    
    /**
     * vbrious parameters of the formulas for skipping acks.
     */
    public stbtic final IntSetting MAX_SKIP_ACKS =
        FACTORY.crebteSettableIntSetting("MAX_SKIP_ACKS",5,"max_skip_ack",15,2);
    
    public stbtic final FloatSetting DEVIATION =
        FACTORY.crebteSettableFloatSetting("SKIP_DEVIATION",1.3f,"skip_deviation",2.0f,1.0f);
    
    public stbtic final IntSetting PERIOD_LENGTH =
        FACTORY.crebteSettableIntSetting("PERIOD_LENGTH",500,"period_length",2000,100);
    
    public stbtic final IntSetting HISTORY_SIZE=
        FACTORY.crebteSettableIntSetting("HISTORY_SIZE",10,"history_size",50,2);
    
    /**
     * Whether the client should use HebdPings when ranking sources
     */
    public stbtic final BooleanSetting USE_HEADPINGS =
        FACTORY.crebteSettableBooleanSetting("USE_HEADPINGS",true,"use_headpings");
    
    /**
     * Whether the client should drop incoming HebdPings.
     */
    public stbtic final BooleanSetting DROP_HEADPINGS =
        FACTORY.crebteSettableBooleanSetting("DROP_HEADPINGS",false,"drop_headpings");
    
    /**
     * We should stop issuing HebdPings when we have this many verified sources
     */
    public stbtic final IntSetting MAX_VERIFIED_HOSTS = 
        FACTORY.crebteSettableIntSetting("MAX_VERIFIED_HOSTS",1,"max_verified_hosts",5,0);
    
    /**
     * We should not schedule more thbn this many head pings at once
     */
    public stbtic final IntSetting PING_BATCH =
        FACTORY.crebteSettableIntSetting("PING_BATCH",10,"PingRanker.pingBatch",50,1);
    
    /**
     * Do not stbrt new workers more than this often
     */
    public stbtic final IntSetting WORKER_INTERVAL =
        FACTORY.crebteSettableIntSetting("WORKER_INTERVAL",2000,"ManagedDownloader.workerInterval",20000,1);
    
    /**
     * Use b download SelectionStrategy tailored for previewing if the file's extension is
     * in this list.
     */
    privbte static String[] defaultPreviewableExtensions = {"html", "htm", "xml", "txt", "rtf", "tex",
        "mp3", "mp4", "wbv", "au", "aif", "aiff", "ra", "ram", "wma", "wmv", "midi", "aifc", "snd",
        "mpg", "mpeg", "bsf", "qt", "mov", "avi", "mpe", "ogg", "rm", "m4a", "flac", "fla"};
    public stbtic final StringArraySetting PREVIEWABLE_EXTENSIONS = 
        FACTORY.crebteSettableStringArraySetting("PREVIEWABLE_EXTENSIONS", 
                defbultPreviewableExtensions,
                "PREVIEWABLE_EXTENSIONS");
}
