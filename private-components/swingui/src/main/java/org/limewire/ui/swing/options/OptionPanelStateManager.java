package org.limewire.ui.swing.options;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.limewire.setting.Setting;

import com.google.inject.Inject;

/**
 * Shared class to allow the options panel to use an {@link OptionPanel}
 *  in multiple locations.
 *  
 * TODO: an option panel should not be used in two places!
 */
public class OptionPanelStateManager {
    
    private final Map<Setting, Object> activeSettingMap;
    private final Set<SettingChangedListener> listeners;
    
    @Inject
    public OptionPanelStateManager() {
        activeSettingMap = new HashMap<Setting, Object>();
        listeners = new HashSet<SettingChangedListener>();
    }
    
    public Object getValue(Setting setting) {
        Object value = activeSettingMap.get(setting);
        if (value != null) {
            return value;
        }
        else {
            return setting.get();
        }
    }
    
    public void setValue(Setting setting, Object value) {
        if (!value.equals(activeSettingMap.get(setting))) {
            activeSettingMap.put(setting, value);
            fireChanges(setting);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void saveSettings() {
        for ( Setting key : activeSettingMap.keySet() ) {
            key.set(activeSettingMap.get(key));
        }
    }
    
    public boolean hasPendingChanges() {
        for ( Setting key : activeSettingMap.keySet() ) {
            if (!activeSettingMap.get(key).equals(key.get())) {
                return true;
            }
        }
        return false;
    }
    
    private void fireChanges(Setting setting) {
        for ( SettingChangedListener listener : listeners ) {
            listener.settingChanged(setting);
        }
    }
    
    public void addSettingChangedListener(SettingChangedListener listener) {
        listeners.add(listener);
    }
    
    public void removeSettingChangedListener(SettingChangedListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Interface to report pending changes to a given setting.
     */
    public static interface SettingChangedListener {
        public void settingChanged(Setting setting);
    }
    
}
