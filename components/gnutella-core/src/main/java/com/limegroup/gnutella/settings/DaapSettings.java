
pbckage com.limegroup.gnutella.settings;

import com.limegroup.gnutellb.util.MacOSXUtils;
import com.limegroup.gnutellb.util.CommonUtils;

/**
 * Settings for Digitbl Audio Access Protocol (DAAP)
 */
public clbss DaapSettings extends LimeProps {
    
    privbte DaapSettings() {}
    
    /**
     * Whether or not DAAP should be enbbled
     */
    public stbtic BooleanSetting DAAP_ENABLED =
	    FACTORY.crebteBooleanSetting("DAAP_ENABLED", true);
	
    
    /**
     * The file types supported by DAAP.
     */
    public stbtic StringArraySetting DAAP_SUPPORTED_FILE_TYPES = 
        FACTORY.crebteStringArraySetting("DAAP_SUPPORTED_FILE_TYPES", 
            new String[]{".mp3", ".m4b", ".wav", ".aif", ".aiff", ".m1a"});
            
    /**
     * The nbme of the Library.
     */
    public stbtic StringSetting DAAP_LIBRARY_NAME =
	    (StringSetting)FACTORY.crebteStringSetting("DAAP_LIBRARY_NAME",
	            getPossessiveUserNbme() + " LimeWire Tunes").
	    setPrivbte(true);
	
    /**
     * The mbximum number of simultaneous connections. Note: There
     * is bn audio stream per connection (i.e. there are actually 
     * DAAP_MAX_CONNECTIONS*2)
     */
    public stbtic IntSetting DAAP_MAX_CONNECTIONS =
        FACTORY.crebteIntSetting("DAAP_MAX_CONNECTIONS", 5);
        
    /**
     * The port where the DbapServer is running
     */
    public stbtic IntSetting DAAP_PORT =
	    FACTORY.crebteIntSetting("DAAP_PORT", 5214);
	
    /**
     * The fully qublified service type name <code>_daap._tcp.local.</code>.
     * You shouldn't chbnge this value as iTunes won't see our DaapServer.
     */
    public stbtic StringSetting DAAP_TYPE_NAME =
	FACTORY.crebteStringSetting("DAAP_TYPE_NAME", "_daap._tcp.local.");
	
    /**
     * The nbme of the Service. I recommend to set this value to the
     * sbme as <code>DAAP_LIBRARY_NAME</code>.<p>
     * Note: when you're debling with mDNS then is the actual Service 
     * nbme <code>DAAP_SERVICE_NAME.getValue() + "." + 
     * DAAP_TYPE_NAME.getVblue()</code>
     */
	public stbtic StringSetting DAAP_SERVICE_NAME =
		(StringSetting)FACTORY.crebteStringSetting("DAAP_SERVICE_NAME",
		    getPossessiveUserNbme() + " LimeWire Tunes").
		setPrivbte(true);
	
    /**
     * This isn't importbnt
     */
    public stbtic IntSetting DAAP_WEIGHT 
        = FACTORY.crebteIntSetting("DAAP_WEIGHT", 0);
    
    /**
     * This isn't importbnt
     */
    public stbtic IntSetting DAAP_PRIORITY 
        = FACTORY.crebteIntSetting("DAAP_PRIORITY", 0);
	
    /**
     * Whether or not pbssword protection is enabled
     */
    public stbtic BooleanSetting DAAP_REQUIRES_PASSWORD =
	    FACTORY.crebteBooleanSetting("DAAP_REQUIRES_PASSWORD", false);
    
    /**
     * The pbssword in clear text. A security hazard?
     */
    public stbtic PasswordSetting DAAP_PASSWORD =
	    FACTORY.crebtePasswordSetting("DAAP_PASSWORD", "");
    
    /**
     * Use either BIO or NIO (defbult) for DAAP
     */
    public stbtic BooleanSetting DAAP_USE_NIO = 
        FACTORY.crebteBooleanSetting("DAAP_USE_NIO", true);
    
    /**
     * With defbult JVM settings we start to run out of memory
     * if the Librbry becomes greater than 16000 Songs (OSX 10.3,
     * JVM 1.4.2_04, G5 with 2.5GB of RAM). Therefore I'm limiting
     * the mbx size to 10000 Songs.
     */
    public stbtic IntSetting DAAP_MAX_LIBRARY_SIZE =
        FACTORY.crebteIntSetting("DAAP_MAX_LIBRARY_SIZE", 10000);
    
    /**
     * The number of revisions the Librbry should keep in the history
     * (necessbry for slowly updating clients and iTunes is quite
     * slow in thbt respect).
     */
    public stbtic IntSetting DAAP_LIBRARY_REVISIONS =
        FACTORY.crebteIntSetting("DAAP_LIBRARY_REVISIONS", 100);
    
    /**
     * If <tt>true</tt> (defbult) then Library earses entries 
     * from the eldest to the lbtest entry automatically from
     * the revision history tbble. <b>IT IS A VERY BAD IDEA
     * TO DISABLE THIS CUZ YOU WOULD WASTE MEMORY!!!</b>
     */
    public stbtic BooleanSetting DAAP_LIBRARY_GC =
        FACTORY.crebteBooleanSetting("DAAP_LIBRARY_GC", true);
        
    /**
     * Gets the user's nbme, in possessive format.
     */
    privbte static String getPossessiveUserName() {
        String nbme = System.getProperty("user.name", "Unknown");
        if(CommonUtils.isCocobFoundationAvailable()) {
            String n = MbcOSXUtils.getUserName();
            if(n != null)
                nbme = n;
        }
        if(!nbme.endsWith("s"))
            nbme += "'s";
        else
            nbme += "'";
        return nbme;
    }
}
