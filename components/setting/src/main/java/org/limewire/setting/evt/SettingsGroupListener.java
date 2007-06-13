package org.limewire.setting.evt;

import org.limewire.setting.AbstractSettingsGroup;

/**
 * A listener for {@link AbstractSettingsGroup}
 */
public interface SettingsGroupListener {
    
    /**
     * Invoked when a {@link AbstractSettingsGroup} instance changed its state
     */
    public void settingsGroupChanged(SettingsGroupEvent evt);
}
