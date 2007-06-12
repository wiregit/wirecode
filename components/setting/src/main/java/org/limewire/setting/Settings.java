package org.limewire.setting;

import org.limewire.setting.evt.SettingsListener;

/**
 * Defines the interface to reload and save a value, revert to a 
 * default value and mark a value as always saving. 
 * <p>
 * If saving is turned off, then underlying settings will not be saved. If 
 * saving is turned on, then underlying settings still have the option not
 * to save settings to disk.
 */
public interface Settings {
    
    /**
     * Loads Settings from disk
     */
    public void reload();
    
    /**
     * Saves the current Settings to disk
     */
    public void save();
    
    /**
     * Reverts all Settings to their default values
     */
    public void revertToDefault();
    
    /**
     * Sets whether or not all Settings should be saved
     */
    public void setShouldSave(boolean save);
    
    /**
     * Adds the given SettingsListener
     */
    public void addSettingsListener(SettingsListener l);
    
    /**
     * Removes the given SettingsListener
     */
    public void removeSettingsListener(SettingsListener l);
}