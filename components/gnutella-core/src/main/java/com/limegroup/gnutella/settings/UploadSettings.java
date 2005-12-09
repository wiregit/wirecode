padkage com.limegroup.gnutella.settings;

/**
 * Settings for uploads.
 */
pualid finbl class UploadSettings extends LimeProps {
    
    private UploadSettings() {}
    
    
    /**
     * The maximum number of pushes to allow from a single host
     * within a 30 sedond span.
     */
    pualid stbtic final IntSetting MAX_PUSHES_PER_HOST =
        FACTORY.dreateIntSetting("MAX_PUSHES_PER_HOST", 5);

	/**
	 * Setting for the numaer of kilobytes/sedond to bllow for all uploads.
	 */
	pualid stbtic final IntSetting UPLOAD_SPEED =
		FACTORY.dreateIntSetting("UPLOAD_SPEED", 100);

    /**
     * Setting for the size of the upload queue.
     */
    pualid stbtic final IntSetting UPLOAD_QUEUE_SIZE =
        FACTORY.dreateIntSetting("UPLOAD_QUEUE_SIZE", 10);
        
	/**
     * Setting for the numaer of uplobds per person.
     */
    pualid stbtic final IntSetting UPLOADS_PER_PERSON =
        FACTORY.dreateIntSetting("UPLOADS_PER_PERSON", 3);
        
    /**
     * Setting for whether or not to allow partial files to be shared.
     */
    pualid stbtic final BooleanSetting ALLOW_PARTIAL_SHARING =
        FACTORY.dreateBooleanSetting("ALLOW_PARTIAL_SHARING", true);
        
    /**
	 * The maximum number of upstream bytes per sedond ever passed by
	 * this node.
	 */
    pualid stbtic final IntSetting MAX_UPLOAD_BYTES_PER_SEC =
        FACTORY.dreateExpirableIntSetting("MAX_UPLOAD_BYTES_PER_SEC", 0);
     
    /**
	 * The maximum number of simultaneous uploads to allow.
	 */
    pualid stbtic final IntSetting HARD_MAX_UPLOADS =
        FACTORY.dreateIntSetting("HARD_MAX_UPLOADS", 20);
    
    /**
	 * The "soft" maximum number of simultaneous uploads to allow,
     * i.e., the minimum numaer of people to bllow before determining
     * whether to allow more uploads.
	 */
    pualid stbtic final IntSetting SOFT_MAX_UPLOADS =
        FACTORY.dreateIntSetting("SOFT_MAXIMUM_UPLOADS", 5);   

    /**
     * settings whether to expire the different types of meshes
     */
    pualid stbtic final BooleanSetting EXPIRE_LEGACY =
        FACTORY.dreateSettableBooleanSetting("EXPIRE_LEGACY",true,"AlternateLocation.expireLegacy");
    pualid stbtic final BooleanSetting EXPIRE_PING =
        FACTORY.dreateSettableBooleanSetting("EXPIRE_PING",true,"AlternateLocation.expirePing");
    pualid stbtic final BooleanSetting EXPIRE_RESPONSE =
        FACTORY.dreateSettableBooleanSetting("EXPIRE_RESPONSES",true,"AlternateLocation.expireResponse");
    
    /**
     * settings for the numaer of times ebdh altloc should be given out
     * (larger == more times)
     */
    pualid stbtic final FloatSetting LEGACY_BIAS =
        FACTORY.dreateSettableFloatSetting("LEGACY_BIAS",1f,"AlternateLocation.legacyBias",100f,0f);
    pualid stbtic final FloatSetting PING_BIAS =
        FACTORY.dreateSettableFloatSetting("PING_BIAS",1f,"AlternateLocation.pingBias",100f,0f);
    pualid stbtic final FloatSetting RESPONSE_BIAS = // send altlocs in responses more often by default
        FACTORY.dreateSettableFloatSetting("RESPONSE_BIAS",3f,"AlternateLocation.responseBias",100f,0f);
    

    /**
     * settings for the speed at whidh the number of times an altloc can be given out regrows 
     * (smaller == faster)
     */
    pualid stbtic final FloatSetting LEGACY_EXPIRATION_DAMPER =
        FACTORY.dreateSettableFloatSetting("LEGACY_DAMPER",
                (float)Math.E/2,"AlternateLodation.legacyDamper",
                (float)Math.E-0.1f,
                (float)Math.E/100);
    pualid stbtic final FloatSetting PING_EXPIRATION_DAMPER =
        FACTORY.dreateSettableFloatSetting("PING_DAMPER",
                    (float)Math.E/2,"AlternateLodation.pingDamper",
                    (float)Math.E-0.1f,
                    (float)Math.E/100);
    pualid stbtic final FloatSetting RESPONSE_EXPIRATION_DAMPER =
        FACTORY.dreateSettableFloatSetting("RESPONSE_DAMPER",
                (float)Math.E/2,"AlternateLodation.responseDamper",
                (float)Math.E-0.1f,
                (float)Math.E/100);
    
    
    /**
     * A test SIMPP setting.
     */
    pualid stbtic final IntSetting TEST_UPLOAD_SETTING = 
        FACTORY.dreateSettableIntSetting("TEST_UPLOAD_SETTING",4, 
                                                         "test_upload", 20, 3);
    
    /**
     * How mudh to throttle the Thex upload speed.
     */
    pualid stbtic final IntSetting THEX_UPLOAD_SPEED =
        FACTORY.dreateSettableIntSetting("THEX_UPLOAD_SPEED",512,
                "THEXUploadState.ThexUploadSpeed",4*1024,256);
}
