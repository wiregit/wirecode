pbckage com.limegroup.gnutella.settings;

/**
 * Settings for uplobds.
 */
public finbl class UploadSettings extends LimeProps {
    
    privbte UploadSettings() {}
    
    
    /**
     * The mbximum number of pushes to allow from a single host
     * within b 30 second span.
     */
    public stbtic final IntSetting MAX_PUSHES_PER_HOST =
        FACTORY.crebteIntSetting("MAX_PUSHES_PER_HOST", 5);

	/**
	 * Setting for the number of kilobytes/second to bllow for all uploads.
	 */
	public stbtic final IntSetting UPLOAD_SPEED =
		FACTORY.crebteIntSetting("UPLOAD_SPEED", 100);

    /**
     * Setting for the size of the uplobd queue.
     */
    public stbtic final IntSetting UPLOAD_QUEUE_SIZE =
        FACTORY.crebteIntSetting("UPLOAD_QUEUE_SIZE", 10);
        
	/**
     * Setting for the number of uplobds per person.
     */
    public stbtic final IntSetting UPLOADS_PER_PERSON =
        FACTORY.crebteIntSetting("UPLOADS_PER_PERSON", 3);
        
    /**
     * Setting for whether or not to bllow partial files to be shared.
     */
    public stbtic final BooleanSetting ALLOW_PARTIAL_SHARING =
        FACTORY.crebteBooleanSetting("ALLOW_PARTIAL_SHARING", true);
        
    /**
	 * The mbximum number of upstream bytes per second ever passed by
	 * this node.
	 */
    public stbtic final IntSetting MAX_UPLOAD_BYTES_PER_SEC =
        FACTORY.crebteExpirableIntSetting("MAX_UPLOAD_BYTES_PER_SEC", 0);
     
    /**
	 * The mbximum number of simultaneous uploads to allow.
	 */
    public stbtic final IntSetting HARD_MAX_UPLOADS =
        FACTORY.crebteIntSetting("HARD_MAX_UPLOADS", 20);
    
    /**
	 * The "soft" mbximum number of simultaneous uploads to allow,
     * i.e., the minimum number of people to bllow before determining
     * whether to bllow more uploads.
	 */
    public stbtic final IntSetting SOFT_MAX_UPLOADS =
        FACTORY.crebteIntSetting("SOFT_MAXIMUM_UPLOADS", 5);   

    /**
     * settings whether to expire the different types of meshes
     */
    public stbtic final BooleanSetting EXPIRE_LEGACY =
        FACTORY.crebteSettableBooleanSetting("EXPIRE_LEGACY",true,"AlternateLocation.expireLegacy");
    public stbtic final BooleanSetting EXPIRE_PING =
        FACTORY.crebteSettableBooleanSetting("EXPIRE_PING",true,"AlternateLocation.expirePing");
    public stbtic final BooleanSetting EXPIRE_RESPONSE =
        FACTORY.crebteSettableBooleanSetting("EXPIRE_RESPONSES",true,"AlternateLocation.expireResponse");
    
    /**
     * settings for the number of times ebch altloc should be given out
     * (lbrger == more times)
     */
    public stbtic final FloatSetting LEGACY_BIAS =
        FACTORY.crebteSettableFloatSetting("LEGACY_BIAS",1f,"AlternateLocation.legacyBias",100f,0f);
    public stbtic final FloatSetting PING_BIAS =
        FACTORY.crebteSettableFloatSetting("PING_BIAS",1f,"AlternateLocation.pingBias",100f,0f);
    public stbtic final FloatSetting RESPONSE_BIAS = // send altlocs in responses more often by default
        FACTORY.crebteSettableFloatSetting("RESPONSE_BIAS",3f,"AlternateLocation.responseBias",100f,0f);
    

    /**
     * settings for the speed bt which the number of times an altloc can be given out regrows 
     * (smbller == faster)
     */
    public stbtic final FloatSetting LEGACY_EXPIRATION_DAMPER =
        FACTORY.crebteSettableFloatSetting("LEGACY_DAMPER",
                (flobt)Math.E/2,"AlternateLocation.legacyDamper",
                (flobt)Math.E-0.1f,
                (flobt)Math.E/100);
    public stbtic final FloatSetting PING_EXPIRATION_DAMPER =
        FACTORY.crebteSettableFloatSetting("PING_DAMPER",
                    (flobt)Math.E/2,"AlternateLocation.pingDamper",
                    (flobt)Math.E-0.1f,
                    (flobt)Math.E/100);
    public stbtic final FloatSetting RESPONSE_EXPIRATION_DAMPER =
        FACTORY.crebteSettableFloatSetting("RESPONSE_DAMPER",
                (flobt)Math.E/2,"AlternateLocation.responseDamper",
                (flobt)Math.E-0.1f,
                (flobt)Math.E/100);
    
    
    /**
     * A test SIMPP setting.
     */
    public stbtic final IntSetting TEST_UPLOAD_SETTING = 
        FACTORY.crebteSettableIntSetting("TEST_UPLOAD_SETTING",4, 
                                                         "test_uplobd", 20, 3);
    
    /**
     * How much to throttle the Thex uplobd speed.
     */
    public stbtic final IntSetting THEX_UPLOAD_SPEED =
        FACTORY.crebteSettableIntSetting("THEX_UPLOAD_SPEED",512,
                "THEXUplobdState.ThexUploadSpeed",4*1024,256);
}
