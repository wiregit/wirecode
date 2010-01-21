package org.limewire.setting;

import org.limewire.core.settings.LimeProps;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Settings for handling Activation.
 */
public class ActivationSettings extends LimeProps {

    private ActivationSettings() {}

    //TODO: set the correct link here. This is a working test
    public static final StringSetting ACTIVATION_HOST = FACTORY.createRemoteStringSetting(
            "ACTIVATION_HOST", "http://activate.limewire.int/lookup", "ActivationSettings.activationHost");
    
    //TODO: set the correct link here. This will be used to determine what location a given module
    // should use for renewing. 
    public static final StringSetting ACTIVATION_RENEWAL_HOST = FACTORY.createRemoteStringSetting(
            "ACTIVATION_RENEWAL_HOST", "http://www.limewire.com", "ActivationSettings.activationRenewalHost");

    public static final StringSetting MODULE_KEY_IDENTIFIER = FACTORY.createRemoteStringSetting(
            "MODULE_KEY_IDENTIFIER", "moduleId=", "ActivationSettings.moduleKeyIdentifier");
    
    /**
     * Returns whether this was considered a PRO version at the last shutdown. This is
     * needed for starting up some UI components before ActivationManager can be fully
     * initialized.
     */
    public static final BooleanSetting LAST_START_WAS_PRO = FACTORY.createBooleanSetting(
            "LAST_START_WAS_PRO", LimeWireUtils.shouldShowProSplashScreen());

    static {
        // When the application starts, we need to know whether to show the pro or the basic splash screen,
        // and we need to know this before the settings have been loaded. So, we add a listener to this setting
        // that creates a file on disk which we can use to determine whether or not to show the pro splash screen.
        LAST_START_WAS_PRO.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                LimeWireUtils.setShouldShowProSplashScreen(LAST_START_WAS_PRO.getValue());
            }
        });
    }

    /**
     * License Key for activation. If not License Key exists will return empty String.
     */
    public static final StringSetting ACTIVATION_KEY = FACTORY.createStringSetting(
            "ACTIVATION_KEY", "");
    
    /**
     * Saved mcode from the last json String.
     */
    public static final StringSetting M_CODE = FACTORY.createStringSetting(
            "M_CODE", "");
    
    /**
     * Encryption key for saving the json message.
     */
    public static final StringSetting PASS_KEY = FACTORY.createStringSetting(
            "PASS_KEY", "3A931AF193AC44F66540CFFC57C3978D");
}
