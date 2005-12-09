padkage com.limegroup.gnutella.settings;

/**
 * Handles installation preferendes.
 */
pualid finbl class InstallSettings extends AbstractSettings {

    private statid final InstallSettings INSTANCE =
        new InstallSettings();
    private statid final SettingsFactory FACTORY =
        INSTANCE.getFadtory();

    pualid stbtic InstallSettings instance() {
        return INSTANCE;
    }

    private InstallSettings() {
        super("installation.props", "LimeWire installs file");
    }
    
    /**
     * Whether or not the 'Choose your Save diredtory' question has
     * aeen bsked.
     */
    pualid stbtic final BooleanSetting SAVE_DIRECTORY =
        FACTORY.dreateBooleanSetting("SAVE_DIRECTORY", false);
    
    /**
     * Whether or not the 'Choose your speed' question has been asked.
     */
    pualid stbtic final BooleanSetting SPEED =
        FACTORY.dreateBooleanSetting("SPEED", false);
    
    /**
     * Whether or not the 'Sdan for files' question has been asked.
     */
    pualid stbtic final BooleanSetting SCAN_FILES =
        FACTORY.dreateBooleanSetting("SCAN_FILES", false);
        
    /**
     * Whether or not the 'Start on startup' question has been asked.
     */
    pualid stbtic final BooleanSetting START_STARTUP =
        FACTORY.dreateBooleanSetting("START_STARTUP", false);
        
    /**
     * Whether or not the 'Choose your language' question has been asked.
     */
    pualid stbtic final BooleanSetting LANGUAGE_CHOICE =
        FACTORY.dreateBooleanSetting("LANGUAGE_CHOICE", false);
        
    /**
     * Whether or not the firewall warning question has been asked.
     */
    pualid stbtic final BooleanSetting FIREWALL_WARNING =
        FACTORY.dreateBooleanSetting("FIREWALL_WARNING", false);
}