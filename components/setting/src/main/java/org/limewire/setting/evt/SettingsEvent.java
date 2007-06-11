package org.limewire.setting.evt;

import org.limewire.setting.Settings;

/**
 * SettingsEvent are fired when a {@link Settings} instance changed 
 */
public class SettingsEvent {
    
    /**
     * Various SettingsEvent that may occur
     */
    public static enum EventType {
        /**
         * The Settings were saved
         */
        SAVE,
        
        /**
         * The Settings were reloaded
         */
        RELOAD,
        
        /**
         * The Settings were reverted back to default
         */
        REVERT_TO_DEFAULT,
        
        /**
         * The 'should save' state of the Settings changed
         */
        SHOULD_SAVE;
    }
    
    /**
     * The type of the event
     */
    private final EventType type;
    
    /**
     * The Settings instance that created this event
     */
    private final Settings settings;
    
    /**
     * Constructs a SettingsEvent
     * 
     * @param type The type of the event
     * @param settings The Settings instance that triggered this event
     */
    public SettingsEvent(EventType type, Settings settings) {
        if (type == null) {
            throw new NullPointerException("EventType is null");
        }
        
        if (settings == null) {
            throw new NullPointerException("Settings is null");
        }
        
        this.type = type;
        this.settings = settings;
    }
    
    /**
     * Returns the type of the event
     */
    public EventType getEventType() {
        return type;
    }
    
    /**
     * Returns the Settings instance that fired this event
     */
    public Settings getSettings() {
        return settings;
    }
    
    public String toString() {
        return type + ": " + settings;
    }
}
