package org.limewire.setting;

import org.limewire.core.settings.LimeProps;

public class ActivationSettings extends LimeProps {
    
    private ActivationSettings() {}

    public static final StringSetting ACTIVATION_HOST = FACTORY.createRemoteStringSetting(
            "ACTIVATION_HOST", "www.limewire.com", "ActivationSettings.activationHost");
    
    /**
     * Returns whether this was considered a PRO version at the last shutdown. This is
     * needed for starting up some UI components before ActivationManager can be fully
     * initialized.
     */
    public static final BooleanSetting LAST_START_WAS_PRO = FACTORY.createBooleanSetting(
            "LAST_START_WAS_PRO", false);
    
    public static final StringSetting ACTIVATION_KEY = FACTORY.createStringSetting(
            "ACTIVATION_KEY", "");
}
