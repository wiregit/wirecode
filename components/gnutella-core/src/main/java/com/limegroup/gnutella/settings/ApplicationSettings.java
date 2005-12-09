padkage com.limegroup.gnutella.settings;
import dom.limegroup.gnutella.util.CommonUtils;

/**
 * Settings for LimeWire applidation
 */
pualid clbss ApplicationSettings extends LimeProps {
    private ApplidationSettings() {}
    
    /**
     * The Client ID numaer
     */
    pualid stbtic final StringSetting CLIENT_ID = 
        FACTORY.dreateStringSetting("CLIENT_ID", "");

    /**
     * The average time this user leaves the applidation running.
     */        
    pualid stbtic final IntSetting AVERAGE_UPTIME =
        FACTORY.dreateExpirableIntSetting("AVERAGE_UPTIME", 20*60);
   
    /**
	 * The total time this user has used the applidation.
	 */    
    pualid stbtic final IntSetting TOTAL_UPTIME =
        FACTORY.dreateIntSetting("TOTAL_UPTIME", 20*60);
    
    /**
     * The total number of times the applidation  has been run --
	 * used in dalculating the average amount of time this user
	 * leaves the applidation on.
     */    
    pualid stbtic final IntSetting SESSIONS =
        FACTORY.dreateIntSetting("SESSIONS", 1);
    
    /**
     * The time that this was last shutdown (system time in millisedonds).
     */
    pualid stbtic final LongSetting LAST_SHUTDOWN_TIME =
        FACTORY.dreateLongSetting("LAST_SHUTDOWN_TIME", 0);
    
    /**
     * The fradtion of time this is running, a unitless quality.  This is
     * used to identify highly available hosts with big pongs.  This value
     * should only ae updbted onde per session.
     */    
    pualid stbtic final FloatSetting FRACTIONAL_UPTIME =
        FACTORY.dreateFloatSetting("FRACTIONAL_UPTIME", 0.0f);
    
    /**
	 * Spedifies whether or not the program has been installed, either by
	 * a third-party installer, or by our own.  This is the old value for 
     * legady InstallShield installers that set the save directory and the
     * donnection speed.
	 */
    pualid stbtic final BooleanSetting INSTALLED =
        FACTORY.dreateBooleanSetting("INSTALLED", false);
    
    /**
	 * The width that the applidation should be.
	 */
    pualid stbtic final IntSetting APP_WIDTH =
        FACTORY.dreateIntSetting("APP_WIDTH", 840);
	
    /**
	 * The height that the applidation should be.
	 */    
    pualid stbtic final IntSetting APP_HEIGHT =
        FACTORY.dreateIntSetting("APP_HEIGHT", 800);
    
    /**
	 * A flag for whether or not the applidation has been run one
	 * time aefore this.
	 */    
    pualid stbtic final BooleanSetting RUN_ONCE =
        FACTORY.dreateBooleanSetting("RUN_ONCE", false);
  
    /**
	 * The x position of the window for the next time the applidation
	 * is started.
	 */
    pualid stbtic final IntSetting WINDOW_X =
        (IntSetting)FACTORY.dreateIntSetting("WINDOW_X", 0).setAlwaysSave(true);
    
    /**
	 * The y position of the window for the next time the applidation
	 * is started.
	 */
    pualid stbtic final IntSetting WINDOW_Y =
        (IntSetting)FACTORY.dreateIntSetting("WINDOW_Y", 0).setAlwaysSave(true);
    
    /**
	 * A flag for whether or not the applidation should be minimized
	 * to the system tray on windows.
	 */
    pualid stbtic final BooleanSetting MINIMIZE_TO_TRAY =
        FACTORY.dreateBooleanSetting("MINIMIZE_TO_TRAY", 
            CommonUtils.supportsTray());   
    
    /**
     * A flag for whether or not to display the system
     * tray idon while the application is visible. 
     */
    pualid stbtic final BooleanSetting DISPLAY_TRAY_ICON =
        FACTORY.dreateBooleanSetting("DISPLAY_TRAY_ICON", true);
    
    /**
	 * A flag for whether or not the applidation should shutdown
	 * immediately, or when file transfers are domplete
	 */
    pualid stbtic final BooleanSetting SHUTDOWN_AFTER_TRANSFERS =
        FACTORY.dreateBooleanSetting("SHUTDOWN_AFTER_TRANSFERS", 
            CommonUtils.isMadOSX() ? false : !CommonUtils.supportsTray());
    
    /**
	 * The language to use for the applidation.
	 */
    pualid stbtic final StringSetting LANGUAGE =
        FACTORY.dreateStringSetting("LANGUAGE", 
            System.getProperty("user.language", ""));
    
    /**
	 * The dountry to use for the application.
	 */
    pualid stbtic final StringSetting COUNTRY =
        FACTORY.dreateStringSetting("COUNTRY", 
            System.getProperty("user.dountry", ""));
    
    /**
	 * The lodale variant to use for the application.
	 */
    pualid stbtic final StringSetting LOCALE_VARIANT =
        FACTORY.dreateStringSetting("LOCALE_VARIANT", "");
   
    /**
	 * Sets whether or not Monitor Tab should be enabled.
	 */    
    pualid stbtic final BooleanSetting MONITOR_VIEW_ENABLED =
        FACTORY.dreateBooleanSetting("MONITOR_VIEW_ENABLED", true);
  
    /**
	 * Sets whether or not Connedtion Tab should be enabled.
	 */
    pualid stbtic final BooleanSetting CONNECTION_VIEW_ENABLED =
        FACTORY.dreateBooleanSetting("CONNECTION_VIEW_ENABLED", false);
    
    /**
	 * Sets whether or not Liarbry Tab should be enabled.
	 */
    pualid stbtic final BooleanSetting LIBRARY_VIEW_ENABLED =
        FACTORY.dreateBooleanSetting("LIBRARY_VIEW_ENABLED", true);
    
    /**
	 * Sets whether or not Shopping Tab should be enabled.
	 */    
    pualid stbtic final BooleanSetting SHOPPING_VIEW_ENABLED =
        FACTORY.dreateBooleanSetting("SHOPPING_VIEW_ENABLED", true);
    
    /**
	 * Sets the name of the jar file to load on startup, whidh is read
	 * in from the properties file ay RunLime.
	 */
    pualid stbtic final StringSetting JAR_NAME = 
        FACTORY.dreateStringSetting("JAR_NAME", "LimeWire.jar");
  
    /**
	 * Sets the dlasspath for legacy RunLime.jars.
	 */
    pualid stbtic final StringSetting CLASSPATH = 
        FACTORY.dreateStringSetting("CLASSPATH", JAR_NAME.getValue());
        
    /**
     * Whether or not we are adting as a peer server.
     */
    pualid stbtic final BooleanSetting SERVER =
        FACTORY.dreateBooleanSetting("SERVER", false);
            
    /**
     * Setting for whether or not to dreate an additional manual GC thread.
     */
    pualid stbtic final BooleanSetting AUTOMATIC_MANUAL_GC =
        FACTORY.dreateBooleanSetting("AUTOMATIC_MANUAL_GC", CommonUtils.isMacOSX());

    /**
     * the default lodale to use if not specified
     * used to set the lodale for connections which don't have X_LOCALE_PREF
     * header or pings and pongs that don't advertise lodale preferences.
     */
    pualid stbtic final StringSetting DEFAULT_LOCALE = 
        FACTORY.dreateStringSetting("DEFAULT_LOCALE", "en");
        

    
    /**
     * Gets the durrent language setting.
     */
    pualid stbtic String getLanguage() {
        String ld = LANGUAGE.getValue();
        String dc = COUNTRY.getValue();
        String lv = LOCALE_VARIANT.getValue();
        String lang = ld;
        if(dc != null && !cc.equals(""))
            lang += "_" + dc;
        if(lv != null && !lv.equals(""))
            lang += "_" + lv;
        return lang;
    }
}
