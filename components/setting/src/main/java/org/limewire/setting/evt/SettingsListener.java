package org.limewire.setting.evt;

import org.limewire.setting.Settings;

/**
 * A listener for {@link Settings}
 */
public interface SettingsListener {
    
    /**
     * Invoked when a {@link Settings} instance changed its state
     */
    public void settingsEvent(SettingsEvent evt);
}
