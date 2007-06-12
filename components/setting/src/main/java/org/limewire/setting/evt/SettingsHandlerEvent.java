package org.limewire.setting.evt;

import org.limewire.setting.SettingsGroup;
import org.limewire.setting.SettingsHandler;

/**
 * SettingsHandlerEvent are fired when a {@link SettingsHandler} instance changed 
 */
public class SettingsHandlerEvent {
    
    /**
     * Various types of events that may occur
     */
    public static enum EventType {
        
        /**
         * Fired when Settings were added to the handler
         */
        SETTINGS_GROUP_ADDED,
        
        /**
         * Fired when Settings were removed from the handler
         */
        SETTINGS_GROUP_REMOVED,
        
        /**
         * Fired when all Settings were reloaded
         */
        RELOAD,
        
        /**
         * Fired when all Settings were saved
         */
        SAVE,
        
        /**
         * Fired when all Settings were revered back to
         * the default values
         */
        REVERT_TO_DEFAULT,
        
        /**
         * Fired when the should save flag was changed
         */
        SHOULD_SAVE;
    }
    
    private final EventType type;
    
    private final SettingsHandler handler;
    
    private final SettingsGroup group;
    
    /**
     * Constructs a SettingsHandlerEvent
     * 
     * @param type The type of the event
     * @param handler The handler that triggered this event
     * @param group The SettingsGroup instance that was added or removed (null in other cases)
     */
    public SettingsHandlerEvent(EventType type, SettingsHandler handler, SettingsGroup group) {
        if (type == null) {
            throw new NullPointerException("EventType is null");
        }
        
        if (handler == null) {
            throw new NullPointerException("SettingsHandler is null");
        }
        
        this.type = type;
        this.handler = handler;
        this.group = group;
    }
    
    /**
     * Returns the type of the event
     */
    public EventType getEventType() {
        return type;
    }
    
    /**
     * Returns the SettingsHandler instance that triggered this event
     */
    public SettingsHandler getSettingsHandler() {
        return handler;
    }
    
    /**
     * The SettingsGroup instance that was added or removed. It's null in
     * all other cases
     */
    public SettingsGroup getSettingsGroup() {
        return group;
    }
    
    public String toString() {
        return type.toString();
    }
}
