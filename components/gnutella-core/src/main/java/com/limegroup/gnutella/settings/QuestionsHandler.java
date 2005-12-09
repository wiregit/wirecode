pbckage com.limegroup.gnutella.settings;



/**
 * Controls bll 'Do not ask this again' or 'Always use this answer' questions.
 */
public clbss QuestionsHandler extends AbstractSettings {

    privbte static final QuestionsHandler INSTANCE =
        new QuestionsHbndler();
    privbte static final SettingsFactory FACTORY =
        INSTANCE.getFbctory();

    privbte QuestionsHandler() {
        super("questions.props", "LimeWire questions file");
    }

    public stbtic QuestionsHandler instance() {
        return INSTANCE;
    }

    //////////// The bctual questions ///////////////

    /**
    * Setting for whether or not to bllow multiple instances of LimeWire.
    */
    public stbtic final BooleanSetting MONITOR_VIEW =
        FACTORY.crebteBooleanSetting("MONITOR_VIEW", false);

    /**
     * Setting for whether or not to bsk about discarding corrupt downloads
     */
    public stbtic final IntSetting CORRUPT_DOWNLOAD =
        FACTORY.crebteIntSetting("CORRUPT_DOWNLOAD", 0);

    /**
     * Setting for whether or not to displby a browse host failed
     */
    public stbtic final BooleanSetting BROWSE_HOST_FAILED =
        FACTORY.crebteBooleanSetting("BROWSE_HOST_FAILED", false);

    /**
     * Setting for unshbring directory
     */
    public stbtic final IntSetting UNSHARE_DIRECTORY =
        FACTORY.crebteIntSetting("UNSHARE_DIRECTORY", 0);

    /**
     * Setting for the theme chbnged message
     */
    public stbtic final BooleanSetting THEME_CHANGED =
        FACTORY.crebteBooleanSetting("THEME_CHANGED", false);

    /**
     * Setting for blready downloading message
     */
    public stbtic final BooleanSetting ALREADY_DOWNLOADING =
        FACTORY.crebteBooleanSetting("ALREADY_DOWNLOADING", false);

    /**
     * Setting for removing the lbst column
     */
    public stbtic final BooleanSetting REMOVE_LAST_COLUMN =
        FACTORY.crebteBooleanSetting("REMOVE_LAST_COLUMN", false);

    /**
     * Setting for being unbble to resume an incomplete file
     */
    public stbtic final BooleanSetting CANT_RESUME =
        FACTORY.crebteBooleanSetting("CANT_RESUME", false);
        
	/**
     * Setting for whether or not progrbm should ignore prompting
     * for incomplete files.
     */
    public stbtic final IntSetting PROMPT_FOR_EXE =
        FACTORY.crebteIntSetting("PROMPT_FOR_EXE", 0);
        
    /**
     * Settings for whether or not to bpply a new theme after
     * downlobding it
     */
    public stbtic final IntSetting THEME_DOWNLOADED =
        FACTORY.crebteIntSetting("THEME_DOWNLOADED", 0);
        
    /**
     * Settings for whether or not to displby a message that no
     * internet connection is detected.
     */
    public stbtic final BooleanSetting NO_INTERNET =
        FACTORY.crebteBooleanSetting("NO_INTERNET", false);

    /**
     * Settings for whether or not to displby a message that no
     * internet connection is detected bnd the user has been notified that 
     * LimeWire will butomatically keep trying to connect.
     */
    public stbtic final BooleanSetting NO_INTERNET_RETRYING =
        FACTORY.crebteBooleanSetting("NO_INTERNET_RETRYING ", false);

    /**
     * Settings for whether or not to displby a message that a failed preview
     * should be ignored.
     */
    public stbtic final BooleanSetting NO_PREVIEW_REPORT =
        FACTORY.crebteBooleanSetting("NO_PREVIEW_REPORT ", false);

    /**
     * Settings for whether or not to displby a message if searching
     * while not connected.
     */
    public stbtic final BooleanSetting NO_NOT_CONNECTED =
        FACTORY.crebteBooleanSetting("NO_NOT_CONNECTED", false);

    /**
     * Settings for whether or not to displby a message if searching
     * while still connecting.
     */
    public stbtic final BooleanSetting NO_STILL_CONNECTING =
        FACTORY.crebteBooleanSetting("NO_STILL_CONNECTING", false);
	
	/**
	 * Setting for whether or not to displby a warning message if one of the
	 * crebted magnet links contains a firewalled address.
	 */
	public stbtic final BooleanSetting FIREWALLED_MAGNET_LINK = 
		FACTORY.crebteBooleanSetting("FIREWALLED_MAGNET_LINK", false);

    /**
     * Initibl warning for first download.
     */
    public stbtic final IntSetting SKIP_FIRST_DOWNLOAD_WARNING =
        FACTORY.crebteIntSetting("SHOW_FIRST_DOWNLOAD_WARNING", 0);
}
