package com.limegroup.gnutella.settings;


/**
 * Settings for uploads.
 */
public final class UploadSettings extends AbstractSettings {

	/**
	 * Setting for the number of kilobytes/second to allow for all uploads.
	 */
	public static final IntSetting UPLOAD_SPEED =
		FACTORY.createIntSetting("UPLOAD_SPEED", 100);
    
}
