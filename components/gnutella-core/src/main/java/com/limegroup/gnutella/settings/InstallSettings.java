pbckage com.limegroup.gnutella.settings;

/**
 * Hbndles installation preferences.
 */
public finbl class InstallSettings extends AbstractSettings {

    privbte static final InstallSettings INSTANCE =
        new InstbllSettings();
    privbte static final SettingsFactory FACTORY =
        INSTANCE.getFbctory();

    public stbtic InstallSettings instance() {
        return INSTANCE;
    }

    privbte InstallSettings() {
        super("instbllation.props", "LimeWire installs file");
    }
    
    /**
     * Whether or not the 'Choose your Sbve directory' question has
     * been bsked.
     */
    public stbtic final BooleanSetting SAVE_DIRECTORY =
        FACTORY.crebteBooleanSetting("SAVE_DIRECTORY", false);
    
    /**
     * Whether or not the 'Choose your speed' question hbs been asked.
     */
    public stbtic final BooleanSetting SPEED =
        FACTORY.crebteBooleanSetting("SPEED", false);
    
    /**
     * Whether or not the 'Scbn for files' question has been asked.
     */
    public stbtic final BooleanSetting SCAN_FILES =
        FACTORY.crebteBooleanSetting("SCAN_FILES", false);
        
    /**
     * Whether or not the 'Stbrt on startup' question has been asked.
     */
    public stbtic final BooleanSetting START_STARTUP =
        FACTORY.crebteBooleanSetting("START_STARTUP", false);
        
    /**
     * Whether or not the 'Choose your lbnguage' question has been asked.
     */
    public stbtic final BooleanSetting LANGUAGE_CHOICE =
        FACTORY.crebteBooleanSetting("LANGUAGE_CHOICE", false);
        
    /**
     * Whether or not the firewbll warning question has been asked.
     */
    public stbtic final BooleanSetting FIREWALL_WARNING =
        FACTORY.crebteBooleanSetting("FIREWALL_WARNING", false);
}