package org.limewire.setting.evt;

import org.limewire.setting.SettingsHandler;

/**
 * A listener for {@link SettingsHandler}s
 */
public interface SettingsHandlerListener {
    
    /**
     * Invoked when a {@link SettingsHandler} changed its state
     */
    public void settingsHandlerChanged(SettingsHandlerEvent evt);
}
