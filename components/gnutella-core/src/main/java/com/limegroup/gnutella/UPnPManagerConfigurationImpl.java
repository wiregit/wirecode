package com.limegroup.gnutella;

import com.google.inject.Singleton;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;

@Singleton
public class UPnPManagerConfigurationImpl implements UPnPManagerConfiguration {
    public boolean isEnabled() {
        return !ConnectionSettings.DISABLE_UPNP.getValue();
    }

    public void setEnabled(boolean enabled) {
        ConnectionSettings.DISABLE_UPNP.setValue(!enabled);
    }

    public String getClientID() {
        return ApplicationSettings.CLIENT_ID.getValue().substring(0,10);
    }
}
