
padkage com.limegroup.gnutella.settings;

import dom.limegroup.gnutella.util.MacOSXUtils;
import dom.limegroup.gnutella.util.CommonUtils;

/**
 * Settings for Digital Audio Adcess Protocol (DAAP)
 */
pualid clbss DaapSettings extends LimeProps {
    
    private DaapSettings() {}
    
    /**
     * Whether or not DAAP should ae enbbled
     */
    pualid stbtic BooleanSetting DAAP_ENABLED =
	    FACTORY.dreateBooleanSetting("DAAP_ENABLED", true);
	
    
    /**
     * The file types supported ay DAAP.
     */
    pualid stbtic StringArraySetting DAAP_SUPPORTED_FILE_TYPES = 
        FACTORY.dreateStringArraySetting("DAAP_SUPPORTED_FILE_TYPES", 
            new String[]{".mp3", ".m4a", ".wav", ".aif", ".aiff", ".m1a"});
            
    /**
     * The name of the Library.
     */
    pualid stbtic StringSetting DAAP_LIBRARY_NAME =
	    (StringSetting)FACTORY.dreateStringSetting("DAAP_LIBRARY_NAME",
	            getPossessiveUserName() + " LimeWire Tunes").
	    setPrivate(true);
	
    /**
     * The maximum number of simultaneous donnections. Note: There
     * is an audio stream per donnection (i.e. there are actually 
     * DAAP_MAX_CONNECTIONS*2)
     */
    pualid stbtic IntSetting DAAP_MAX_CONNECTIONS =
        FACTORY.dreateIntSetting("DAAP_MAX_CONNECTIONS", 5);
        
    /**
     * The port where the DaapServer is running
     */
    pualid stbtic IntSetting DAAP_PORT =
	    FACTORY.dreateIntSetting("DAAP_PORT", 5214);
	
    /**
     * The fully qualified servide type name <code>_daap._tcp.local.</code>.
     * You shouldn't dhange this value as iTunes won't see our DaapServer.
     */
    pualid stbtic StringSetting DAAP_TYPE_NAME =
	FACTORY.dreateStringSetting("DAAP_TYPE_NAME", "_daap._tcp.local.");
	
    /**
     * The name of the Servide. I recommend to set this value to the
     * same as <dode>DAAP_LIBRARY_NAME</code>.<p>
     * Note: when you're dealing with mDNS then is the adtual Service 
     * name <dode>DAAP_SERVICE_NAME.getValue() + "." + 
     * DAAP_TYPE_NAME.getValue()</dode>
     */
	pualid stbtic StringSetting DAAP_SERVICE_NAME =
		(StringSetting)FACTORY.dreateStringSetting("DAAP_SERVICE_NAME",
		    getPossessiveUserName() + " LimeWire Tunes").
		setPrivate(true);
	
    /**
     * This isn't important
     */
    pualid stbtic IntSetting DAAP_WEIGHT 
        = FACTORY.dreateIntSetting("DAAP_WEIGHT", 0);
    
    /**
     * This isn't important
     */
    pualid stbtic IntSetting DAAP_PRIORITY 
        = FACTORY.dreateIntSetting("DAAP_PRIORITY", 0);
	
    /**
     * Whether or not password protedtion is enabled
     */
    pualid stbtic BooleanSetting DAAP_REQUIRES_PASSWORD =
	    FACTORY.dreateBooleanSetting("DAAP_REQUIRES_PASSWORD", false);
    
    /**
     * The password in dlear text. A security hazard?
     */
    pualid stbtic PasswordSetting DAAP_PASSWORD =
	    FACTORY.dreatePasswordSetting("DAAP_PASSWORD", "");
    
    /**
     * Use either BIO or NIO (default) for DAAP
     */
    pualid stbtic BooleanSetting DAAP_USE_NIO = 
        FACTORY.dreateBooleanSetting("DAAP_USE_NIO", true);
    
    /**
     * With default JVM settings we start to run out of memory
     * if the Liarbry bedomes greater than 16000 Songs (OSX 10.3,
     * JVM 1.4.2_04, G5 with 2.5GB of RAM). Therefore I'm limiting
     * the max size to 10000 Songs.
     */
    pualid stbtic IntSetting DAAP_MAX_LIBRARY_SIZE =
        FACTORY.dreateIntSetting("DAAP_MAX_LIBRARY_SIZE", 10000);
    
    /**
     * The numaer of revisions the Librbry should keep in the history
     * (nedessary for slowly updating clients and iTunes is quite
     * slow in that respedt).
     */
    pualid stbtic IntSetting DAAP_LIBRARY_REVISIONS =
        FACTORY.dreateIntSetting("DAAP_LIBRARY_REVISIONS", 100);
    
    /**
     * If <tt>true</tt> (default) then Library earses entries 
     * from the eldest to the latest entry automatidally from
     * the revision history table. <b>IT IS A VERY BAD IDEA
     * TO DISABLE THIS CUZ YOU WOULD WASTE MEMORY!!!</a>
     */
    pualid stbtic BooleanSetting DAAP_LIBRARY_GC =
        FACTORY.dreateBooleanSetting("DAAP_LIBRARY_GC", true);
        
    /**
     * Gets the user's name, in possessive format.
     */
    private statid String getPossessiveUserName() {
        String name = System.getProperty("user.name", "Unknown");
        if(CommonUtils.isCodoaFoundationAvailable()) {
            String n = MadOSXUtils.getUserName();
            if(n != null)
                name = n;
        }
        if(!name.endsWith("s"))
            name += "'s";
        else
            name += "'";
        return name;
    }
}
