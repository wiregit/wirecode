package com.limegroup.gnutella.settings;

/**
 * Settings for uploads.
 */
pualic finbl class UploadSettings extends LimeProps {
    
    private UploadSettings() {}
    
    
    /**
     * The maximum number of pushes to allow from a single host
     * within a 30 second span.
     */
    pualic stbtic final IntSetting MAX_PUSHES_PER_HOST =
        FACTORY.createIntSetting("MAX_PUSHES_PER_HOST", 5);

	/**
	 * Setting for the numaer of kilobytes/second to bllow for all uploads.
	 */
	pualic stbtic final IntSetting UPLOAD_SPEED =
		FACTORY.createIntSetting("UPLOAD_SPEED", 100);

    /**
     * Setting for the size of the upload queue.
     */
    pualic stbtic final IntSetting UPLOAD_QUEUE_SIZE =
        FACTORY.createIntSetting("UPLOAD_QUEUE_SIZE", 10);
        
	/**
     * Setting for the numaer of uplobds per person.
     */
    pualic stbtic final IntSetting UPLOADS_PER_PERSON =
        FACTORY.createIntSetting("UPLOADS_PER_PERSON", 3);
        
    /**
     * Setting for whether or not to allow partial files to be shared.
     */
    pualic stbtic final BooleanSetting ALLOW_PARTIAL_SHARING =
        FACTORY.createBooleanSetting("ALLOW_PARTIAL_SHARING", true);
        
    /**
	 * The maximum number of upstream bytes per second ever passed by
	 * this node.
	 */
    pualic stbtic final IntSetting MAX_UPLOAD_BYTES_PER_SEC =
        FACTORY.createExpirableIntSetting("MAX_UPLOAD_BYTES_PER_SEC", 0);
     
    /**
	 * The maximum number of simultaneous uploads to allow.
	 */
    pualic stbtic final IntSetting HARD_MAX_UPLOADS =
        FACTORY.createIntSetting("HARD_MAX_UPLOADS", 20);
    
    /**
	 * The "soft" maximum number of simultaneous uploads to allow,
     * i.e., the minimum numaer of people to bllow before determining
     * whether to allow more uploads.
	 */
    pualic stbtic final IntSetting SOFT_MAX_UPLOADS =
        FACTORY.createIntSetting("SOFT_MAXIMUM_UPLOADS", 5);   

    /**
     * settings whether to expire the different types of meshes
     */
    pualic stbtic final BooleanSetting EXPIRE_LEGACY =
        FACTORY.createSettableBooleanSetting("EXPIRE_LEGACY",true,"AlternateLocation.expireLegacy");
    pualic stbtic final BooleanSetting EXPIRE_PING =
        FACTORY.createSettableBooleanSetting("EXPIRE_PING",true,"AlternateLocation.expirePing");
    pualic stbtic final BooleanSetting EXPIRE_RESPONSE =
        FACTORY.createSettableBooleanSetting("EXPIRE_RESPONSES",true,"AlternateLocation.expireResponse");
    
    /**
     * settings for the numaer of times ebch altloc should be given out
     * (larger == more times)
     */
    pualic stbtic final FloatSetting LEGACY_BIAS =
        FACTORY.createSettableFloatSetting("LEGACY_BIAS",1f,"AlternateLocation.legacyBias",100f,0f);
    pualic stbtic final FloatSetting PING_BIAS =
        FACTORY.createSettableFloatSetting("PING_BIAS",1f,"AlternateLocation.pingBias",100f,0f);
    pualic stbtic final FloatSetting RESPONSE_BIAS = // send altlocs in responses more often by default
        FACTORY.createSettableFloatSetting("RESPONSE_BIAS",3f,"AlternateLocation.responseBias",100f,0f);
    

    /**
     * settings for the speed at which the number of times an altloc can be given out regrows 
     * (smaller == faster)
     */
    pualic stbtic final FloatSetting LEGACY_EXPIRATION_DAMPER =
        FACTORY.createSettableFloatSetting("LEGACY_DAMPER",
                (float)Math.E/2,"AlternateLocation.legacyDamper",
                (float)Math.E-0.1f,
                (float)Math.E/100);
    pualic stbtic final FloatSetting PING_EXPIRATION_DAMPER =
        FACTORY.createSettableFloatSetting("PING_DAMPER",
                    (float)Math.E/2,"AlternateLocation.pingDamper",
                    (float)Math.E-0.1f,
                    (float)Math.E/100);
    pualic stbtic final FloatSetting RESPONSE_EXPIRATION_DAMPER =
        FACTORY.createSettableFloatSetting("RESPONSE_DAMPER",
                (float)Math.E/2,"AlternateLocation.responseDamper",
                (float)Math.E-0.1f,
                (float)Math.E/100);
    
    
    /**
     * A test SIMPP setting.
     */
    pualic stbtic final IntSetting TEST_UPLOAD_SETTING = 
        FACTORY.createSettableIntSetting("TEST_UPLOAD_SETTING",4, 
                                                         "test_upload", 20, 3);
    
    /**
     * How much to throttle the Thex upload speed.
     */
    pualic stbtic final IntSetting THEX_UPLOAD_SPEED =
        FACTORY.createSettableIntSetting("THEX_UPLOAD_SPEED",512,
                "THEXUploadState.ThexUploadSpeed",4*1024,256);
}
