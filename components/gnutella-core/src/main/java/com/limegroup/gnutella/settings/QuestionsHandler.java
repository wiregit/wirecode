package com.limegroup.gnutella.settings;



/**
 * Controls all 'Do not ask this again' or 'Always use this answer' questions.
 */
public class QuestionsHandler extends AbstractSettings {

    private static final QuestionsHandler INSTANCE =
        new QuestionsHandler();
    private static final SettingsFactory FACTORY =
        INSTANCE.getFactory();

    private QuestionsHandler() {
        super("questions.props", "LimeWire questions file");
    }

    public static QuestionsHandler instance() {
        return INSTANCE;
    }

    //////////// The actual questions ///////////////

    /**
    * Setting for whether or not to allow multiple instances of LimeWire.
    */
    public static final BooleanSetting MONITOR_VIEW =
        FACTORY.createBooleanSetting("MONITOR_VIEW", false);

    /**
     * Setting for whether or not to ask about discarding corrupt downloads
     */
    public static final IntSetting CORRUPT_DOWNLOAD =
        FACTORY.createIntSetting("CORRUPT_DOWNLOAD", 0);

    /**
     * Setting for whether or not to display a browse host failed
     */
    public static final BooleanSetting BROWSE_HOST_FAILED =
        FACTORY.createBooleanSetting("BROWSE_HOST_FAILED", false);

    /**
     * Setting for unsharing directory
     */
    public static final IntSetting UNSHARE_DIRECTORY =
        FACTORY.createIntSetting("UNSHARE_DIRECTORY", 0);

    /**
     * Setting for the theme changed message
     */
    public static final BooleanSetting THEME_CHANGED =
        FACTORY.createBooleanSetting("THEME_CHANGED", false);

    /**
     * Setting for already downloading message
     */
    public static final BooleanSetting ALREADY_DOWNLOADING =
        FACTORY.createBooleanSetting("ALREADY_DOWNLOADING", false);

    /**
     * Setting for removing the last column
     */
    public static final BooleanSetting REMOVE_LAST_COLUMN =
        FACTORY.createBooleanSetting("REMOVE_LAST_COLUMN", false);

    /**
     * Setting for being unable to resume an incomplete file
     */
    public static final BooleanSetting CANT_RESUME =
        FACTORY.createBooleanSetting("CANT_RESUME", false);
        
	/**
     * Setting for whether or not program should ignore prompting
     * for incomplete files.
     */
    public static final IntSetting PROMPT_FOR_EXE =
        FACTORY.createIntSetting("PROMPT_FOR_EXE", 0);
        
    /**
     * Settings for whether or not to apply a new theme after
     * downloading it
     */
    public static final IntSetting THEME_DOWNLOADED =
        FACTORY.createIntSetting("THEME_DOWNLOADED", 0);
        
    /**
     * Settings for whether or not to display a message that no
     * internet connection is detected.
     */
    public static final BooleanSetting NO_INTERNET =
        FACTORY.createBooleanSetting("NO_INTERNET", false);

    /**
     * Settings for whether or not to display a message that no
     * internet connection is detected and the user has been notified that 
     * LimeWire will automatically keep trying to connect.
     */
    public static final BooleanSetting NO_INTERNET_RETRYING =
        FACTORY.createBooleanSetting("NO_INTERNET_RETRYING ", false);

    /**
     * Settings for whether or not to display a message that a failed preview
     * should be ignored.
     */
    public static final BooleanSetting NO_PREVIEW_REPORT =
        FACTORY.createBooleanSetting("NO_PREVIEW_REPORT ", false);

    /**
     * Settings for whether or not to display a message if searching
     * while not connected.
     */
    public static final BooleanSetting NO_NOT_CONNECTED =
        FACTORY.createBooleanSetting("NO_NOT_CONNECTED", false);

    /**
     * Settings for whether or not to display a message if searching
     * while still connecting.
     */
    public static final BooleanSetting NO_STILL_CONNECTING =
        FACTORY.createBooleanSetting("NO_STILL_CONNECTING", false);
	
	/**
	 * Setting for whether or not to display a warning message if one of the
	 * created magnet links contains a firewalled address.
	 */
	public static final BooleanSetting FIREWALLED_MAGNET_LINK = 
		FACTORY.createBooleanSetting("FIREWALLED_MAGNET_LINK", false);

    /**
     * Initial warning for first download.
     */
    public static final IntSetting SKIP_FIRST_DOWNLOAD_WARNING =
        FACTORY.createIntSetting("SHOW_FIRST_DOWNLOAD_WARNING", 0);
}
