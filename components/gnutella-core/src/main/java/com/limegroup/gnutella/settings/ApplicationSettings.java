
// Edited for the Learning branch

package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.CommonUtils;

/**
 * Settings for LimeWire application
 */
public class ApplicationSettings extends LimeProps {

    private ApplicationSettings() {}

    //done

    /**
     * The client ID GUID that uniquely identifies us on the Gnutella network.
     * When LimeWire runs for the first time on a new computer, it makes a new GUID and sets it here.
     */
    public static final StringSetting CLIENT_ID = FACTORY.createStringSetting("CLIENT_ID", "");

    //do

    /**
     * The average time this user leaves the application running.
     */        
    public static final IntSetting AVERAGE_UPTIME =
        FACTORY.createExpirableIntSetting("AVERAGE_UPTIME", 20*60);
   
    /**
	 * The total time this user has used the application.
	 */    
    public static final IntSetting TOTAL_UPTIME =
        FACTORY.createIntSetting("TOTAL_UPTIME", 20*60);
    
    /**
     * The total number of times the application  has been run --
	 * used in calculating the average amount of time this user
	 * leaves the application on.
     */    
    public static final IntSetting SESSIONS =
        FACTORY.createIntSetting("SESSIONS", 1);
    
    /**
     * The time that this was last shutdown (system time in milliseconds).
     */
    public static final LongSetting LAST_SHUTDOWN_TIME =
        FACTORY.createLongSetting("LAST_SHUTDOWN_TIME", 0);
    
    /**
     * The fraction of time this is running, a unitless quality.  This is
     * used to identify highly available hosts with big pongs.  This value
     * should only be updated once per session.
     */    
    public static final FloatSetting FRACTIONAL_UPTIME =
        FACTORY.createFloatSetting("FRACTIONAL_UPTIME", 0.0f);
    
    /**
	 * Specifies whether or not the program has been installed, either by
	 * a third-party installer, or by our own.  This is the old value for 
     * legacy InstallShield installers that set the save directory and the
     * connection speed.
	 */
    public static final BooleanSetting INSTALLED =
        FACTORY.createBooleanSetting("INSTALLED", false);
    
    /**
	 * The width that the application should be.
	 */
    public static final IntSetting APP_WIDTH =
        FACTORY.createIntSetting("APP_WIDTH", 840);
	
    /**
	 * The height that the application should be.
	 */    
    public static final IntSetting APP_HEIGHT =
        FACTORY.createIntSetting("APP_HEIGHT", 800);
    
    /**
	 * A flag for whether or not the application has been run one
	 * time before this.
	 */    
    public static final BooleanSetting RUN_ONCE =
        FACTORY.createBooleanSetting("RUN_ONCE", false);
  
    /**
	 * The x position of the window for the next time the application
	 * is started.
	 */
    public static final IntSetting WINDOW_X =
        (IntSetting)FACTORY.createIntSetting("WINDOW_X", 0).setAlwaysSave(true);
    
    /**
	 * The y position of the window for the next time the application
	 * is started.
	 */
    public static final IntSetting WINDOW_Y =
        (IntSetting)FACTORY.createIntSetting("WINDOW_Y", 0).setAlwaysSave(true);
    
    /**
	 * A flag for whether or not the application should be minimized
	 * to the system tray on windows.
	 */
    public static final BooleanSetting MINIMIZE_TO_TRAY =
        FACTORY.createBooleanSetting("MINIMIZE_TO_TRAY", 
            CommonUtils.supportsTray());   
    
    /**
     * A flag for whether or not to display the system
     * tray icon while the application is visible. 
     */
    public static final BooleanSetting DISPLAY_TRAY_ICON =
        FACTORY.createBooleanSetting("DISPLAY_TRAY_ICON", true);
    
    /**
	 * A flag for whether or not the application should shutdown
	 * immediately, or when file transfers are complete
	 */
    public static final BooleanSetting SHUTDOWN_AFTER_TRANSFERS =
        FACTORY.createBooleanSetting("SHUTDOWN_AFTER_TRANSFERS", 
            CommonUtils.isMacOSX() ? false : !CommonUtils.supportsTray());
    
    /**
	 * The language to use for the application.
	 */
    public static final StringSetting LANGUAGE =
        FACTORY.createStringSetting("LANGUAGE", 
            System.getProperty("user.language", ""));
    
    /**
	 * The country to use for the application.
	 */
    public static final StringSetting COUNTRY =
        FACTORY.createStringSetting("COUNTRY", 
            System.getProperty("user.country", ""));
    
    /**
	 * The locale variant to use for the application.
	 */
    public static final StringSetting LOCALE_VARIANT =
        FACTORY.createStringSetting("LOCALE_VARIANT", "");
   
    /**
	 * Sets whether or not Monitor Tab should be enabled.
	 */    
    public static final BooleanSetting MONITOR_VIEW_ENABLED =
        FACTORY.createBooleanSetting("MONITOR_VIEW_ENABLED", true);
  
    /**
	 * Sets whether or not Connection Tab should be enabled.
	 */
    public static final BooleanSetting CONNECTION_VIEW_ENABLED =
        FACTORY.createBooleanSetting("CONNECTION_VIEW_ENABLED", false);
    
    /**
	 * Sets whether or not Library Tab should be enabled.
	 */
    public static final BooleanSetting LIBRARY_VIEW_ENABLED =
        FACTORY.createBooleanSetting("LIBRARY_VIEW_ENABLED", true);
    
    /**
	 * Sets whether or not Shopping Tab should be enabled.
	 */    
    public static final BooleanSetting SHOPPING_VIEW_ENABLED =
        FACTORY.createBooleanSetting("SHOPPING_VIEW_ENABLED", true);
    
    /**
	 * Sets the name of the jar file to load on startup, which is read
	 * in from the properties file by RunLime.
	 */
    public static final StringSetting JAR_NAME = 
        FACTORY.createStringSetting("JAR_NAME", "LimeWire.jar");
  
    /**
	 * Sets the classpath for legacy RunLime.jars.
	 */
    public static final StringSetting CLASSPATH = 
        FACTORY.createStringSetting("CLASSPATH", JAR_NAME.getValue());
        
    /**
     * Whether or not we are acting as a peer server.
     */
    public static final BooleanSetting SERVER =
        FACTORY.createBooleanSetting("SERVER", false);
            
    /**
     * Setting for whether or not to create an additional manual GC thread.
     */
    public static final BooleanSetting AUTOMATIC_MANUAL_GC =
        FACTORY.createBooleanSetting("AUTOMATIC_MANUAL_GC", CommonUtils.isMacOSX());

    /**
     * "en" English, the default locale to assume a remote computer has when it doesn't say.
     * 
     * If a remote computer doesn't tell us it's language preference, we'll assume it's English.
     * This DEFAULT_LOCALE setting isn't the same as our language preference.
     * If a LimeWire program in France is configured for French, this DEFAULT_LOCALE setting will still be "en" English.
     * 
     * the default locale to use if not specified
     * used to set the locale for connections which don't have X_LOCALE_PREF
     * header or pings and pongs that don't advertise locale preferences.
     */
    public static final StringSetting DEFAULT_LOCALE = FACTORY.createStringSetting("DEFAULT_LOCALE", "en");

    /**
     * Gets the current language setting.
     */
    public static String getLanguage() {
        String lc = LANGUAGE.getValue();
        String cc = COUNTRY.getValue();
        String lv = LOCALE_VARIANT.getValue();
        String lang = lc;
        if(cc != null && !cc.equals(""))
            lang += "_" + cc;
        if(lv != null && !lv.equals(""))
            lang += "_" + lv;
        return lang;
    }
}
