package org.limewire.setting;

/**
 * Defines the interface to reload and save a value, revert to a 
 * default value and mark a value as always saving. 
 * <p>
 * If saving is turned off, then underlying settings will not be saved. If 
 * saving is turned on, then underlying settings still have the option not
 * to save settings to disk.
 * 
 */
public interface Settings {
    
    public void reload();
    
    public void save();
    
    public void revertToDefault();
    
    public void setShouldSave(boolean save);
    
}