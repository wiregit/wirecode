package org.limewire.setting.evt;

import org.limewire.setting.Setting;

/**
 * A listener for {@link Setting}s
 */
public interface SettingListener {
    
    /**
     * Invoked when a {@link Setting} changed its state
     */
    public void settingEvent(SettingEvent evt);
}
