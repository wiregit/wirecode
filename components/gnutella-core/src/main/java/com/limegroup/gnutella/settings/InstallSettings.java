package com.limegroup.gnutella.settings;

/**
 * Handles installation preferences.
 */
public final class InstallSettings extends AbstractSettings {

    private static final InstallSettings INSTANCE =
        new InstallSettings();
    private static final SettingsFactory FACTORY =
        INSTANCE.getFactory();

    public static InstallSettings instance() {
        return INSTANCE;
    }

    private InstallSettings() {
        super("installation.props", "LimeWire installs file");
    }
    
    /**
     * Whether or not the 'Choose your Save directory' question has
     * been asked.
     */
    public static final BooleanSetting SAVE_DIRECTORY =
        FACTORY.createBooleanSetting("SAVE_DIRECTORY", false);
    
    /**
     * Whether or not the 'Choose your speed' question has been asked.
     */
    public static final BooleanSetting SPEED =
        FACTORY.createBooleanSetting("SPEED", false);
    
    /**
     * Whether or not the 'Scan for files' question has been asked.
     */
    public static final BooleanSetting SCAN_FILES =
        FACTORY.createBooleanSetting("SCAN_FILES", false);
        
    /**
     * Whether or not the 'Start on startup' question has been asked.
     */
    public static final BooleanSetting START_STARTUP =
        FACTORY.createBooleanSetting("START_STARTUP", false);
        
    /**
     * Whether or not the 'Choose your language' question has been asked.
     */
    public static final BooleanSetting LANGUAGE_CHOICE =
        FACTORY.createBooleanSetting("LANGUAGE_CHOICE", false);
        
    /**
     * Whether or not the firewall warning question has been asked.
     */
    public static final BooleanSetting FIREWALL_WARNING =
        FACTORY.createBooleanSetting("FIREWALL_WARNING", false);
}