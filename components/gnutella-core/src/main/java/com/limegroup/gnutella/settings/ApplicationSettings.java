pbckage com.limegroup.gnutella.settings;
import com.limegroup.gnutellb.util.CommonUtils;

/**
 * Settings for LimeWire bpplication
 */
public clbss ApplicationSettings extends LimeProps {
    privbte ApplicationSettings() {}
    
    /**
     * The Client ID number
     */
    public stbtic final StringSetting CLIENT_ID = 
        FACTORY.crebteStringSetting("CLIENT_ID", "");

    /**
     * The bverage time this user leaves the application running.
     */        
    public stbtic final IntSetting AVERAGE_UPTIME =
        FACTORY.crebteExpirableIntSetting("AVERAGE_UPTIME", 20*60);
   
    /**
	 * The totbl time this user has used the application.
	 */    
    public stbtic final IntSetting TOTAL_UPTIME =
        FACTORY.crebteIntSetting("TOTAL_UPTIME", 20*60);
    
    /**
     * The totbl number of times the application  has been run --
	 * used in cblculating the average amount of time this user
	 * lebves the application on.
     */    
    public stbtic final IntSetting SESSIONS =
        FACTORY.crebteIntSetting("SESSIONS", 1);
    
    /**
     * The time thbt this was last shutdown (system time in milliseconds).
     */
    public stbtic final LongSetting LAST_SHUTDOWN_TIME =
        FACTORY.crebteLongSetting("LAST_SHUTDOWN_TIME", 0);
    
    /**
     * The frbction of time this is running, a unitless quality.  This is
     * used to identify highly bvailable hosts with big pongs.  This value
     * should only be updbted once per session.
     */    
    public stbtic final FloatSetting FRACTIONAL_UPTIME =
        FACTORY.crebteFloatSetting("FRACTIONAL_UPTIME", 0.0f);
    
    /**
	 * Specifies whether or not the progrbm has been installed, either by
	 * b third-party installer, or by our own.  This is the old value for 
     * legbcy InstallShield installers that set the save directory and the
     * connection speed.
	 */
    public stbtic final BooleanSetting INSTALLED =
        FACTORY.crebteBooleanSetting("INSTALLED", false);
    
    /**
	 * The width thbt the application should be.
	 */
    public stbtic final IntSetting APP_WIDTH =
        FACTORY.crebteIntSetting("APP_WIDTH", 840);
	
    /**
	 * The height thbt the application should be.
	 */    
    public stbtic final IntSetting APP_HEIGHT =
        FACTORY.crebteIntSetting("APP_HEIGHT", 800);
    
    /**
	 * A flbg for whether or not the application has been run one
	 * time before this.
	 */    
    public stbtic final BooleanSetting RUN_ONCE =
        FACTORY.crebteBooleanSetting("RUN_ONCE", false);
  
    /**
	 * The x position of the window for the next time the bpplication
	 * is stbrted.
	 */
    public stbtic final IntSetting WINDOW_X =
        (IntSetting)FACTORY.crebteIntSetting("WINDOW_X", 0).setAlwaysSave(true);
    
    /**
	 * The y position of the window for the next time the bpplication
	 * is stbrted.
	 */
    public stbtic final IntSetting WINDOW_Y =
        (IntSetting)FACTORY.crebteIntSetting("WINDOW_Y", 0).setAlwaysSave(true);
    
    /**
	 * A flbg for whether or not the application should be minimized
	 * to the system trby on windows.
	 */
    public stbtic final BooleanSetting MINIMIZE_TO_TRAY =
        FACTORY.crebteBooleanSetting("MINIMIZE_TO_TRAY", 
            CommonUtils.supportsTrby());   
    
    /**
     * A flbg for whether or not to display the system
     * trby icon while the application is visible. 
     */
    public stbtic final BooleanSetting DISPLAY_TRAY_ICON =
        FACTORY.crebteBooleanSetting("DISPLAY_TRAY_ICON", true);
    
    /**
	 * A flbg for whether or not the application should shutdown
	 * immedibtely, or when file transfers are complete
	 */
    public stbtic final BooleanSetting SHUTDOWN_AFTER_TRANSFERS =
        FACTORY.crebteBooleanSetting("SHUTDOWN_AFTER_TRANSFERS", 
            CommonUtils.isMbcOSX() ? false : !CommonUtils.supportsTray());
    
    /**
	 * The lbnguage to use for the application.
	 */
    public stbtic final StringSetting LANGUAGE =
        FACTORY.crebteStringSetting("LANGUAGE", 
            System.getProperty("user.lbnguage", ""));
    
    /**
	 * The country to use for the bpplication.
	 */
    public stbtic final StringSetting COUNTRY =
        FACTORY.crebteStringSetting("COUNTRY", 
            System.getProperty("user.country", ""));
    
    /**
	 * The locble variant to use for the application.
	 */
    public stbtic final StringSetting LOCALE_VARIANT =
        FACTORY.crebteStringSetting("LOCALE_VARIANT", "");
   
    /**
	 * Sets whether or not Monitor Tbb should be enabled.
	 */    
    public stbtic final BooleanSetting MONITOR_VIEW_ENABLED =
        FACTORY.crebteBooleanSetting("MONITOR_VIEW_ENABLED", true);
  
    /**
	 * Sets whether or not Connection Tbb should be enabled.
	 */
    public stbtic final BooleanSetting CONNECTION_VIEW_ENABLED =
        FACTORY.crebteBooleanSetting("CONNECTION_VIEW_ENABLED", false);
    
    /**
	 * Sets whether or not Librbry Tab should be enabled.
	 */
    public stbtic final BooleanSetting LIBRARY_VIEW_ENABLED =
        FACTORY.crebteBooleanSetting("LIBRARY_VIEW_ENABLED", true);
    
    /**
	 * Sets whether or not Shopping Tbb should be enabled.
	 */    
    public stbtic final BooleanSetting SHOPPING_VIEW_ENABLED =
        FACTORY.crebteBooleanSetting("SHOPPING_VIEW_ENABLED", true);
    
    /**
	 * Sets the nbme of the jar file to load on startup, which is read
	 * in from the properties file by RunLime.
	 */
    public stbtic final StringSetting JAR_NAME = 
        FACTORY.crebteStringSetting("JAR_NAME", "LimeWire.jar");
  
    /**
	 * Sets the clbsspath for legacy RunLime.jars.
	 */
    public stbtic final StringSetting CLASSPATH = 
        FACTORY.crebteStringSetting("CLASSPATH", JAR_NAME.getValue());
        
    /**
     * Whether or not we bre acting as a peer server.
     */
    public stbtic final BooleanSetting SERVER =
        FACTORY.crebteBooleanSetting("SERVER", false);
            
    /**
     * Setting for whether or not to crebte an additional manual GC thread.
     */
    public stbtic final BooleanSetting AUTOMATIC_MANUAL_GC =
        FACTORY.crebteBooleanSetting("AUTOMATIC_MANUAL_GC", CommonUtils.isMacOSX());

    /**
     * the defbult locale to use if not specified
     * used to set the locble for connections which don't have X_LOCALE_PREF
     * hebder or pings and pongs that don't advertise locale preferences.
     */
    public stbtic final StringSetting DEFAULT_LOCALE = 
        FACTORY.crebteStringSetting("DEFAULT_LOCALE", "en");
        

    
    /**
     * Gets the current lbnguage setting.
     */
    public stbtic String getLanguage() {
        String lc = LANGUAGE.getVblue();
        String cc = COUNTRY.getVblue();
        String lv = LOCALE_VARIANT.getVblue();
        String lbng = lc;
        if(cc != null && !cc.equbls(""))
            lbng += "_" + cc;
        if(lv != null && !lv.equbls(""))
            lbng += "_" + lv;
        return lbng;
    }
}
