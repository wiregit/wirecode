package org.limewire.setting.evt;

import org.limewire.setting.Setting;

/**
 * SettingEvent are fired when a {@link Setting} changed its state
 */
public class SettingEvent {
    
    /**
     * Various types of events that may occur
     */
    public static enum EventType {
        /**
         * A Setting was reloaded
         */
        RELOAD,
        
        /**
         * A Setting was reverted to the default value
         */
        REVERT_TO_DEFAULT,
        
        /**
         * The always save flag was changed
         */
        ALWAYS_SAVE_CHANGED,
        
        /**
         * The privacy flag was changed
         */
        PRIVACY_CANGED,
        
        /**
         * The value changed
         */
        VALUE_CHANGED;
    }
    
    private final EventType type;
    
    private final Setting setting;
    
    /**
     * Create a SettingEvent
     * 
     * @param type The type of the Event
     * @param setting The Setting that triggered the event
     */
    public SettingEvent(EventType type, Setting setting) {
        assert (type != null);
        assert (setting != null);
        this.type = type;
        this.setting = setting;
    }
    
    /**
     * Returns the type of the event  
     */
    public EventType getEventType() {
        return type;
    }
    
    /**
     * Returns the Setting that triggered the event
     */
    public Setting getSetting() {
        return setting;
    }
    
    public String toString() {
        return type + ": " + setting;
    }
}
