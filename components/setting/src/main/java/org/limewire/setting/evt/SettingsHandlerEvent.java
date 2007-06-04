package org.limewire.setting.evt;

import org.limewire.setting.Settings;
import org.limewire.setting.SettingsHandler;

public class SettingsHandlerEvent {
    
    public static enum Type {
        SETTINGS_ADDED,
        SETTINGS_REMOVED,
        RELOAD,
        SAVE,
        REVERT_TO_DEFAULT,
        SHOULD_SAVE;
    }
    
    private final Type type;
    
    private final SettingsHandler handler;
    
    private final Settings settings;
    
    public SettingsHandlerEvent(Type type, SettingsHandler handler, Settings settings) {
        this.type = type;
        this.handler = handler;
        this.settings = settings;
    }
    
    public Type getType() {
        return type;
    }
    
    public SettingsHandler getSettingsHandler() {
        return handler;
    }
    
    public Settings getSettings() {
        return settings;
    }
}
