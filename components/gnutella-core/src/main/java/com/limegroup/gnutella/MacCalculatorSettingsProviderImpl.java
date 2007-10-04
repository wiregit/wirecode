package com.limegroup.gnutella;

import org.limewire.security.SettingsProvider;

import com.limegroup.gnutella.settings.SecuritySettings;

public class MacCalculatorSettingsProviderImpl implements SettingsProvider {
    public long getChangePeriod() {
        return SecuritySettings.CHANGE_QK_EVERY.getValue();
    }

    public long getGracePeriod() {
        return SecuritySettings.QK_GRACE_PERIOD.getValue();
    }
}
