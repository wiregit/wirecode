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
     * Setting for the amount of uploads per person.
     */
    public static final IntSetting UPLOADS_PER_PERSON =
        FACTORY.createIntSetting("UPLOADS_PER_PERSON", 3);
        
    /**
     * Setting for whether or not to allow partial files to be shared.
     */
    public static final BooleanSetting ALLOW_PARTIAL_SHARING =
        FACTORY.createBooleanSetting("ALLOW_PARTIAL_SHARING", true);
}
