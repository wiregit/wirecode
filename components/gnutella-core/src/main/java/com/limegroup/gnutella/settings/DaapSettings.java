
package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.MacOSXUtils;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * Settings for Digital Audio Access Protocol (DAAP)
 */
public class DaapSettings extends LimeProps {
    
    private DaapSettings() {}
    
    /**
     * Whether or not DAAP should be enabled
     */
    public static BooleanSetting DAAP_ENABLED =
	    FACTORY.createBooleanSetting("DAAP_ENABLED", true);
	
    
    /**
     * The file types supported by DAAP.
     */
    public static StringArraySetting DAAP_SUPPORTED_FILE_TYPES = 
        FACTORY.createStringArraySetting("DAAP_SUPPORTED_FILE_TYPES", 
            new String[]{".mp3", ".m4a", ".wav", ".aif", ".aiff", ".m1a"});
            
    /**
     * The name of the Library.
     */
    public static StringSetting DAAP_LIBRARY_NAME =
	    (StringSetting)FACTORY.createStringSetting("DAAP_LIBRARY_NAME",
	            getPossessiveUserName() + " LimeWire Tunes").
	    setPrivate(true);
	
    /**
     * The maximum number of simultaneous connections. Note: There
     * is an audio stream per connection (i.e. there are actually 
     * DAAP_MAX_CONNECTIONS*2)
     */
    public static IntSetting DAAP_MAX_CONNECTIONS =
        FACTORY.createIntSetting("DAAP_MAX_CONNECTIONS", 5);
        
    /**
     * The port where the DaapServer is running
     */
    public static IntSetting DAAP_PORT =
	    FACTORY.createIntSetting("DAAP_PORT", 5214);
	
    /**
     * The fully qualified service type name <code>_daap._tcp.local.</code>.
     * You shouldn't change this value as iTunes won't see our DaapServer.
     */
    public static StringSetting DAAP_TYPE_NAME =
	FACTORY.createStringSetting("DAAP_TYPE_NAME", "_daap._tcp.local.");
	
    /**
     * The name of the Service. I recommend to set this value to the
     * same as <code>DAAP_LIBRARY_NAME</code>.<p>
     * Note: when you're dealing with mDNS then is the actual Service 
     * name <code>DAAP_SERVICE_NAME.getValue() + "." + 
     * DAAP_TYPE_NAME.getValue()</code>
     */
	public static StringSetting DAAP_SERVICE_NAME =
		(StringSetting)FACTORY.createStringSetting("DAAP_SERVICE_NAME",
		    getPossessiveUserName() + " LimeWire Tunes").
		setPrivate(true);
	
    /**
     * This isn't important
     */
    public static IntSetting DAAP_WEIGHT 
        = FACTORY.createIntSetting("DAAP_WEIGHT", 0);
    
    /**
     * This isn't important
     */
    public static IntSetting DAAP_PRIORITY 
        = FACTORY.createIntSetting("DAAP_PRIORITY", 0);
	
    /**
     * Whether or not password protection is enabled
     */
    public static BooleanSetting DAAP_REQUIRES_PASSWORD =
	    FACTORY.createBooleanSetting("DAAP_REQUIRES_PASSWORD", false);
    
    /**
     * The password in clear text. A security hazard?
     */
    public static PasswordSetting DAAP_PASSWORD =
	    FACTORY.createPasswordSetting("DAAP_PASSWORD", "");
    
    /**
     * Use either BIO or NIO (default) for DAAP
     */
    public static BooleanSetting DAAP_USE_NIO = 
        FACTORY.createBooleanSetting("DAAP_USE_NIO", true);
    
    /**
     * With default JVM settings we start to run out of memory
     * if the Library becomes greater than 16000 Songs (OSX 10.3,
     * JVM 1.4.2_04, G5 with 2.5GB of RAM). Therefore I'm limiting
     * the max size to 10000 Songs.
     */
    public static IntSetting DAAP_MAX_LIBRARY_SIZE =
        FACTORY.createIntSetting("DAAP_MAX_LIBRARY_SIZE", 10000);
    
    /**
     * The number of revisions the Library should keep in the history
     * (necessary for slowly updating clients and iTunes is quite
     * slow in that respect).
     */
    public static IntSetting DAAP_LIBRARY_REVISIONS =
        FACTORY.createIntSetting("DAAP_LIBRARY_REVISIONS", 100);
    
    /**
     * If <tt>true</tt> (default) then Library earses entries 
     * from the eldest to the latest entry automatically from
     * the revision history table. <b>IT IS A VERY BAD IDEA
     * TO DISABLE THIS CUZ YOU WOULD WASTE MEMORY!!!</b>
     */
    public static BooleanSetting DAAP_LIBRARY_GC =
        FACTORY.createBooleanSetting("DAAP_LIBRARY_GC", true);
        
    /**
     * Gets the user's name, in possessive format.
     */
    private static String getPossessiveUserName() {
        String name = System.getProperty("user.name", "Unknown");
        if(CommonUtils.isCocoaFoundationAvailable()) {
            String n = MacOSXUtils.getUserName();
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
