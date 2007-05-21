package org.limewire.setting;

/**
 * Defines the interface for a class to reload and save a value, revert to a 
 * default value and mark a value as savable.
 */
public interface Settings {
    
    public void reload();
    
    public void save();
    
    public void revertToDefault();
    
    public void setShouldSave(boolean save);
    
}