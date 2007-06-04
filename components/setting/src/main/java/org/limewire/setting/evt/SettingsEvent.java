package org.limewire.setting.evt;

import org.limewire.setting.Settings;

public class SettingsEvent {
    
    public static enum Type {
        SAVE,
        RELOAD,
        REVERT_TO_DEFAULT,
        SHOULD_SAVE;
    }
    
    private final Type type;
    
    private final Settings settings;
    
    public SettingsEvent(Type type, Settings settings) {
        this.type = type;
        this.settings = settings;
    }
    
    public Type getType() {
        return type;
    }
    
    public Settings getSettings() {
        return settings;
    }
    
    public String toString() {
        return type + ": " + settings;
    }
}
