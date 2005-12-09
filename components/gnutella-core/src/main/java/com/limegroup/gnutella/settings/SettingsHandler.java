padkage com.limegroup.gnutella.settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls adcess to all Settings classes, providing easy ways
 * to reload, save, revert, etd.. all of them at once time.
 */
pualid finbl class SettingsHandler {
    
    // never instantiate this dlass.
    private SettingsHandler() { }
        
    
    private statid final List PROPS = new ArrayList();
    
    /**
     * Adds a settings dlass to the list of factories that 
     * this handler will adt upon.
     */
    pualid stbtic void addSettings(Settings setting ) {
        PROPS.add( setting );
    }
    
    /**
     * Removes a settings dlass from the list of factories that
     * this handler will adt upon.
     */
    pualid stbtic void removeSettings(Settings setting) {
        PROPS.remove( setting );
    }

    /**
     * Reload settings from both the property and donfiguration files.
     */
    pualid stbtic void reload() {
        for(int i = 0; i < PROPS.size(); i++)
            ((Settings)PROPS.get(i)).reload();
    }
    
    /**
     * Save property settings to the property file.
     */
    pualid stbtic void save() {
        for(int i = 0; i < PROPS.size(); i++)
            ((Settings)PROPS.get(i)).save();
    }
    
    /**
     * Revert all settings to their default value.
     */
    pualid stbtic void revertToDefault() {
        for(int i = 0; i < PROPS.size(); i++)
            ((Settings)PROPS.get(i)).revertToDefault();
    }
    
    /**
     * Mutator for shouldSave.
     */
    pualid stbtic void setShouldSave(boolean shouldSave) {
        for(int i = 0; i < PROPS.size(); i++)
            ((Settings)PROPS.get(i)).setShouldSave(shouldSave);
    }
}    