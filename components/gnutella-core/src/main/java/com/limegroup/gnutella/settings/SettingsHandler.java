package com.limegroup.gnutella.settings;

/**
 * Controls access to all Settings classes, providing easy ways
 * to reload, save, revert, etc.. all of them at once time.
 */
public final class SettingsHandler {
    
    // never instantiate this class.
    private SettingsHandler() { }
        
    
    private static final AbstractSettings[] PROPS =
    {
        LimeProps.instance(), 
        QuestionsHandler.instance()
    };
    

    /**
     * reload settings from both the property and configuration files
     */
    public static void reload() {
        for(int i = 0; i < PROPS.length; i++)
            PROPS[i].reload();
    }
    
    /**
     * Save property settings to the property file
     */
    public static void save() {
        for(int i = 0; i < PROPS.length; i++)
            PROPS[i].save();
    }
    
    /**
     * Revert all settings to their default value
     */
    public static void revertToDefault() {
        for(int i = 0; i < PROPS.length; i++)
            PROPS[i].revertToDefault();
    }
    
    /**
     * Mutator for shouldSave
     */
    public static void setShouldSave(boolean shouldSave) {
        for(int i = 0; i < PROPS.length; i++)
            PROPS[i].setShouldSave(shouldSave);
    }
}    