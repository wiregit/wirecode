package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for handling Activation.
 */
public class ActivationSettings extends LimeProps {

    private ActivationSettings() {}

    //TODO: set the correct link here. This is a working test
    //TODO: move this out of settings to prevent it being changed from props
    public static final StringSetting ACTIVATION_HOST = FACTORY.createRemoteStringSetting(
            "ACTIVATION_HOST", "https://qa-activate.limewire.com/lookup", "ActivationSettings.activationHost");
    
    public static final StringSetting ACTIVATION_CUSTOMER_SUPPORT_HOST = FACTORY.createRemoteStringSetting(
            "ACTIVATION_CUSTOMER_SUPPORT_HOST", "http://www.limewire.com/client_redirect/?page=proSupport", "ActivationSettings.activationCustomerSupportHost");

    public static final StringSetting ACTIVATION_ACCOUNT_SETTINGS_HOST = FACTORY.createRemoteStringSetting(
            "ACTIVATION_ACCOUNT_SETTINGS_HOST", "http://www.limewire.com/client_redirect/?page=accountDetails", "ActivationSettings.activationAccountSettingsHost");

    // TODO: This property probably shouldn't be in this class. It's here temporarily until we can find it a better home.
    public static final StringSetting LIMEWIRE_UPSELL_PRO_DOWNLOAD_HOST = FACTORY.createRemoteStringSetting(
            "LIMEWIRE_UPSELL_PRO_DOWNLOAD_HOST", "http://www.limewire.com/client_redirect/?page=downloadPro", "ActivationSettings.limewireDownloadUpsellHost");

    public static final StringSetting LIMEWIRE_DOWNLOAD_UPDATE_HOST = FACTORY.createRemoteStringSetting(
            "LIMEWIRE_DOWNLOAD_UPDATE_HOST", "http://www.limewire.com/client_redirect/?page=update", "ActivationSettings.limewireDownloadUpdateHost");

    public static final StringSetting RENEW_PRO_HOST = FACTORY.createRemoteStringSetting(
            "RENEW_PRO_HOST", "http://www.limewire.com/client_redirect/?page=renewPro", "ActivationSettings.activationRenewProHost");

    /**
     * Returns whether this was considered a PRO version at the last shutdown. This is
     * needed for starting up some UI components before ActivationManager can be fully
     * initialized.
     */
    public static final BooleanSetting LAST_START_WAS_PRO = FACTORY.createBooleanSetting(
            "LAST_START_WAS_PRO", false);//LimeWireUtils.shouldShowProSplashScreen());

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
