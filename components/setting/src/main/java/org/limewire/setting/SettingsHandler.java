package org.limewire.setting;

import java.util.ArrayList;
import java.util.List;


/**
 * Groups related {@link Settings} objects in one location to reload, revert to
 * a default value, save, or mark as save-able all <code>Settings</code> 
 * objects at once.
 */
public final class SettingsHandler {
    
    // never instantiate this class.
    private SettingsHandler() { }
        
    
    private static final List<Settings> PROPS = new ArrayList<Settings>();
    
    /**
     * Adds a settings class to the list of factories that 
     * this handler will act upon.
     */
    public static void addSettings(Settings setting ) {
        PROPS.add( setting );
    }
    
    /**
     * Removes a settings class from the list of factories that
     * this handler will act upon.
     */
    public static void removeSettings(Settings setting) {
        PROPS.remove( setting );
    }

    /**
     * Reload settings from both the property and configuration files.
     */
    public static void reload() {
        for(int i = 0; i < PROPS.size(); i++)
            PROPS.get(i).reload();
    }
    
    /**
     * Save property settings to the property file.
     */
    public static void save() {
        for(int i = 0; i < PROPS.size(); i++)
            PROPS.get(i).save();
    }
    
    /**
     * Revert all settings to their default value.
     */
    public static void revertToDefault() {
        for(int i = 0; i < PROPS.size(); i++)
            PROPS.get(i).revertToDefault();
    }
    
    /**
     * Mutator for shouldSave.
     */
    public static void setShouldSave(boolean shouldSave) {
        for(int i = 0; i < PROPS.size(); i++)
            PROPS.get(i).setShouldSave(shouldSave);
    }
}    