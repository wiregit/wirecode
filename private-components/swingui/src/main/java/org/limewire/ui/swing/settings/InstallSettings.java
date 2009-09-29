package org.limewire.ui.swing.settings;

import org.limewire.core.settings.LimeWireSettings;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.ProbabilisticBooleanSetting;
import org.limewire.setting.SettingsFactory;
import org.limewire.setting.StringSetSetting;
import org.limewire.setting.StringSetting;
import org.limewire.util.StringUtils;

/**
 * Handles installation preferences.
 */
public final class InstallSettings extends LimeWireSettings {

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
    
    /** Whether or not the filter question has been asked. */
    public static final BooleanSetting FILTER_OPTION =
        FACTORY.createBooleanSetting("FILTER_OPTION", false);
    
    /** Whether Auto-Sharing question has been asked. */
    public static final BooleanSetting AUTO_SHARING_OPTION =
        FACTORY.createBooleanSetting("AUTO_SHARING_OPTION", false);
    
    /** Whether the anonymous data collection option has been asked. */
    public static final BooleanSetting ANONYMOUS_DATA_COLLECTION =
        FACTORY.createBooleanSetting("ANONYMOUS_DATA_COLLECTION", false);
    
    /** Whether the association option has been asked. */
    public static final IntSetting ASSOCIATION_OPTION =
    	FACTORY.createIntSetting("ASSOCIATION_OPTION", 0);

    /** Whether the association option has been asked. */
    public static final BooleanSetting EXTENSION_OPTION =
        FACTORY.createBooleanSetting("EXTENSION_OPTION", false);
    
    /** Whether the setup wizard has been completed on 5. */
    public static final BooleanSetting UPGRADED_TO_5 =
        FACTORY.createBooleanSetting("UPGRADED_TO_5", false);
    
    /** Whether to use the old style pro dialog or the new one. */
    public static final ProbabilisticBooleanSetting RANDOM_USE_MODAL = 
        FACTORY.createRemoteProbabilisticBooleanSetting("RANDOM_USE_MODAL", .5f, "InstallSettings.UseOldProNag", 0f, 1f);

    public enum NagStyles {
        NON_MODAL, MODAL, RANDOM}
    
    private static final StringSetting NAG_STYLE = 
        FACTORY.createStringSetting("NAG_STYLE", "");
    
    public static NagStyles getNagStyle() {
        if(StringUtils.isEmpty(NAG_STYLE.get())) {
            NAG_STYLE.set(pickNagStyle().toString());    
        } 
        try {
            return NagStyles.valueOf(NAG_STYLE.get());
        } catch (IllegalArgumentException e) {
            return NagStyles.RANDOM;
        }
    }
    
    private static InstallSettings.NagStyles pickNagStyle() {
        double style = Math.random();
        if(style < (1d/3)) {
            return InstallSettings.NagStyles.MODAL;
        } else if(style < (2d/3)) {
            return InstallSettings.NagStyles.NON_MODAL;
        } else {
            return InstallSettings.NagStyles.RANDOM;
        }
    }
    
    public static boolean isRandomNag() {
        if(RANDOM_USE_MODAL.get() == 1f || RANDOM_USE_MODAL.get() == 0f) {
            return false;    
        } else {
            return NagStyles.RANDOM == getNagStyle();
        }
    }
    
    /**
     * Stores the value of the last known version of limewire that has been run. Will be null on a clean install until the program is run and a value is set for it.
     * This setting starts with versions > 5.2.2 
     */
    public static final StringSetting LAST_VERSION_RUN = FACTORY.createStringSetting("LAST_VERSION_RUN", "");
    
    /**
     * Stores an array of all the known versions of limewire that have been run.
     * This setting starts with versions > 5.2.2
     */
    public static final StringSetSetting PREVIOUS_RAN_VERSIONS = FACTORY.createStringSetSetting("PREVIOUS_RAN_VERSIONS", "");
    
}
