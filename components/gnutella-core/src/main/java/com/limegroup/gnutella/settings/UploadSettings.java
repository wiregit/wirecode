package com.limegroup.gnutella.settings;

/**
 * Settings for uploads.
 */
public final class UploadSettings extends LimeProps {
    
    private UploadSettings() {}
    

	/**
	 * Setting for the number of kilobytes/second to allow for all uploads.
	 */
	public static final IntSetting UPLOAD_SPEED =
		FACTORY.createIntSetting("UPLOAD_SPEED", 100);

    /**
     * Setting for the size of the upload queue.
     */
    public static final IntSetting UPLOAD_QUEUE_SIZE =
        FACTORY.createIntSetting("UPLOAD_QUEUE_SIZE", 10);
        
	/**
     * Setting for the number of uploads per person.
     */
    public static final IntSetting UPLOADS_PER_PERSON =
        FACTORY.createIntSetting("UPLOADS_PER_PERSON", 3);
        
    /**
     * Setting for whether or not to allow partial files to be shared.
     */
    public static final BooleanSetting ALLOW_PARTIAL_SHARING =
        FACTORY.createBooleanSetting("ALLOW_PARTIAL_SHARING", true);
        
    /**
	 * The maximum number of upstream bytes per second ever passed by
	 * this node.
	 */
    public static final IntSetting MAX_UPLOAD_BYTES_PER_SEC =
        FACTORY.createExpirableIntSetting("MAX_UPLOAD_BYTES_PER_SEC", 0);
     
    /**
	 * The maximum number of simultaneous uploads to allow.
	 */
    public static final IntSetting HARD_MAX_UPLOADS =
        FACTORY.createIntSetting("HARD_MAX_UPLOADS", 20);
    
    /**
	 * The "soft" maximum number of simultaneous uploads to allow,
     * i.e., the minimum number of people to allow before determining
     * whether to allow more uploads.
	 */
    public static final IntSetting SOFT_MAX_UPLOADS =
        FACTORY.createIntSetting("SOFT_MAXIMUM_UPLOADS", 5);   

    public static final IntSetting TEST_UPLOAD_SETTING = 
        FACTORY.createSettableIntSetting("TEST_UPLOAD_SETTING",4, "test_upload");
}
