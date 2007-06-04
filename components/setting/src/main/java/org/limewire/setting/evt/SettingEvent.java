package org.limewire.setting.evt;

import org.limewire.setting.Setting;

public class SettingEvent {
    
    public static enum Type {
        RELOAD,
        REVERT_TO_DEFAULT,
        ALWAYS_SAVE_CHANGED,
        PRIVACY_CANGED,
        VALUE_CHANGED;
    }
    
    private final Type type;
    
    private final Setting setting;
    
    public SettingEvent(Type type, Setting setting) {
        this.type = type;
        this.setting = setting;
    }
    
    public Type getType() {
        return type;
    }
    
    public Setting getSetting() {
        return setting;
    }
    
    public String toString() {
        return type + ": " + setting;
    }
}
