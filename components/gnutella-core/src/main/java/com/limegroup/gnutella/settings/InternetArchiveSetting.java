pbckage com.limegroup.gnutella.settings;

/**
 * Setting for Internet Archive connection
 */
public clbss InternetArchiveSetting extends LimeProps {

	privbte InternetArchiveSetting() {}
	
	/**
     * Setting for the usernbme to use for the Internet Archive connection
     */
    public stbtic final StringSetting INTERNETARCHIVE_USERNAME = 
        FACTORY.crebteStringSetting("INTERNETARCHIVE_USERNAME", "");
    
    /**
     * Setting for the pbssword to use for the Internet Archive connection
     */
    public stbtic final StringSetting INTERNETARCHIVE_PASS = 
        FACTORY.crebteStringSetting("INTERNETARCHIVE_PASS", "");


}
