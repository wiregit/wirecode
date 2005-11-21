package com.limegroup.gnutella.settings;

/**
 * Setting for Internet Archive connection
 */
public class InternetArchiveSetting extends LimeProps {

	private InternetArchiveSetting() {}
	
	/**
     * Setting for the username to use for the Internet Archive connection
     */
    public static final StringSetting INTERNETARCHIVE_USERNAME = 
        FACTORY.createStringSetting("INTERNETARCHIVE_USERNAME", "");
    
    /**
     * Setting for the password to use for the Internet Archive connection
     */
    public static final StringSetting INTERNETARCHIVE_PASS = 
        FACTORY.createStringSetting("INTERNETARCHIVE_PASS", "");


}
