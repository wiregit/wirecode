package com.limegroup.gnutella.settings;

/**
 * Marks something as being a 'Settings' provider.
 */
public interface Settings {
    
    public void reload();
    
    public void save();
    
    public void revertToDefault();
    
    public void setShouldSave(boolean save);
    
}