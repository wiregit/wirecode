package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.*;
import java.util.Properties;
import com.sun.java.util.collections.*;
import java.io.*;


/**
 * Abstract Settings class that all settings classes should extend.
 * Provides basic functionality for all settings classes.
 */
public abstract class AbstractSettings {
        
    /**
     * properties file
     */
    private final File PROPS_FILE;
    
	/**
	 * Constant for the <tt>SettingsFactory</tt> that subclasses can use
	 * to create new settings which will be stored in the properties file.
	 */
	private final SettingsFactory FACTORY;
    
    /**
     * Value for whether or not settings should be saved to file.
     */
    private boolean _shouldSave = true;
    
    /**
     * Basic constructor that creates the FACTORY and PROPS_FILE.
     */
    protected AbstractSettings(String fileName, String header) {
        File settingsDir = CommonUtils.getUserSettingsDir();
        PROPS_FILE = new File(settingsDir, fileName);
        FACTORY = new SettingsFactory(PROPS_FILE, header);
        SettingsHandler.addSettings(this);
    }

    /**
     * Accessor for the <tt>Properties</tt> instance that stores all settings.
     *
     * @return the <tt>Properties</tt> instance for storing settings
     */
	public Properties getProperties() {
        return FACTORY.getProperties();
	}
    
    /**
     * Accessor for the <tt>File</tt> instance taht stores all properties
     */
    public File getPropertiesFile() {
        return PROPS_FILE;
    }
    
    /**
     * Accessor for the <tt>SettingsFactory</tt> instance that stores the properties.
     */
    public SettingsFactory getFactory() {
        return FACTORY;
    }
    
    /**
     * reload settings from both the property and configuration files
     */
    public void reload() {
        FACTORY.reload();
    }
    
    /**
     * Save property settings to the property file
     */
    public void save() {
        if ( _shouldSave) {
            FACTORY.save();
        }
    }
    
    /**
     * Revert all settings to their default value
     */
    public void revertToDefault() {
        FACTORY.revertToDefault();
    }
    
    /**
     * Mutator for shouldSave
     */
    public void setShouldSave(boolean shouldSave) {
        _shouldSave = shouldSave;
    }
    
    /**
     * Access for shouldSave
     */
    public boolean getShouldSave() {
        return _shouldSave;
    }

    public Setting getSetting(String key) {
        synchronized(FACTORY) {
            Iterator iter = FACTORY.iterator();
            while(iter.hasNext()) {
                Setting currSetting = (Setting)iter.next();
                if(currSetting.getKey().equals(key))
                    return currSetting;
            }
        }
        return null; //unable the find the setting we are looking for
    }
}
