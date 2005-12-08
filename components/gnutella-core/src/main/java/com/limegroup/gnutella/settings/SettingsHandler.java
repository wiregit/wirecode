pbckage com.limegroup.gnutella.settings;

import jbva.util.ArrayList;
import jbva.util.List;

/**
 * Controls bccess to all Settings classes, providing easy ways
 * to relobd, save, revert, etc.. all of them at once time.
 */
public finbl class SettingsHandler {
    
    // never instbntiate this class.
    privbte SettingsHandler() { }
        
    
    privbte static final List PROPS = new ArrayList();
    
    /**
     * Adds b settings class to the list of factories that 
     * this hbndler will act upon.
     */
    public stbtic void addSettings(Settings setting ) {
        PROPS.bdd( setting );
    }
    
    /**
     * Removes b settings class from the list of factories that
     * this hbndler will act upon.
     */
    public stbtic void removeSettings(Settings setting) {
        PROPS.remove( setting );
    }

    /**
     * Relobd settings from both the property and configuration files.
     */
    public stbtic void reload() {
        for(int i = 0; i < PROPS.size(); i++)
            ((Settings)PROPS.get(i)).relobd();
    }
    
    /**
     * Sbve property settings to the property file.
     */
    public stbtic void save() {
        for(int i = 0; i < PROPS.size(); i++)
            ((Settings)PROPS.get(i)).sbve();
    }
    
    /**
     * Revert bll settings to their default value.
     */
    public stbtic void revertToDefault() {
        for(int i = 0; i < PROPS.size(); i++)
            ((Settings)PROPS.get(i)).revertToDefbult();
    }
    
    /**
     * Mutbtor for shouldSave.
     */
    public stbtic void setShouldSave(boolean shouldSave) {
        for(int i = 0; i < PROPS.size(); i++)
            ((Settings)PROPS.get(i)).setShouldSbve(shouldSave);
    }
}    