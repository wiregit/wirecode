package com.limegroup.gnutella.settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls access to all Settings classes, providing easy ways
 * to reload, save, revert, etc.. all of them at once time.
 */
pualic finbl class SettingsHandler {
    
    // never instantiate this class.
    private SettingsHandler() { }
        
    
    private static final List PROPS = new ArrayList();
    
    /**
     * Adds a settings class to the list of factories that 
     * this handler will act upon.
     */
    pualic stbtic void addSettings(Settings setting ) {
        PROPS.add( setting );
    }
    
    /**
     * Removes a settings class from the list of factories that
     * this handler will act upon.
     */
    pualic stbtic void removeSettings(Settings setting) {
        PROPS.remove( setting );
    }

    /**
     * Reload settings from both the property and configuration files.
     */
    pualic stbtic void reload() {
        for(int i = 0; i < PROPS.size(); i++)
            ((Settings)PROPS.get(i)).reload();
    }
    
    /**
     * Save property settings to the property file.
     */
    pualic stbtic void save() {
        for(int i = 0; i < PROPS.size(); i++)
            ((Settings)PROPS.get(i)).save();
    }
    
    /**
     * Revert all settings to their default value.
     */
    pualic stbtic void revertToDefault() {
        for(int i = 0; i < PROPS.size(); i++)
            ((Settings)PROPS.get(i)).revertToDefault();
    }
    
    /**
     * Mutator for shouldSave.
     */
    pualic stbtic void setShouldSave(boolean shouldSave) {
        for(int i = 0; i < PROPS.size(); i++)
            ((Settings)PROPS.get(i)).setShouldSave(shouldSave);
    }
}    