package org.limewire.setting;

import java.io.File;
import java.util.Properties;

import org.limewire.setting.evt.SettingsEvent.Type;


/**
 * Gives basic features including get, reload and save for a
 * {@link SettingsFactory}.
 */
public class BasicSettings extends AbstractSettings implements Settings {
    
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
     * Basic constructor that creates the FACTORY and PROPS_FILE.
     */
    protected BasicSettings(File settingsFile, String header) {
        PROPS_FILE = settingsFile;
        FACTORY = new SettingsFactory(PROPS_FILE, header);
        
        SettingsHandler.instance().addSettings(this);
    }
    
    /**
     * Returns the <tt>Properties</tt> instance that stores all settings.
     *
     * @return the <tt>Properties</tt> instance for storing settings
     */
	public Properties getProperties() {
        return FACTORY.getProperties();
	}
    
    /**
     * Returns the <tt>File</tt> instance that stores all properties
     */
    public File getPropertiesFile() {
        return PROPS_FILE;
    }
    
    /**
     * Returns the <tt>SettingsFactory</tt> instance that stores the properties.
     */
    public SettingsFactory getFactory() {
        return FACTORY;
    }
    
    /**
     * reload settings from both the property and configuration files
     */
    public void reload() {
        FACTORY.reload();
        fireSettingsEvent(Type.RELOAD);
    }
    
    /**
     * Save property settings to the property file
     */
    public void save() {
        if (getShouldSave()) {
            FACTORY.save();
            fireSettingsEvent(Type.SAVE);
        }
    }
    
    /** Revert all settings to their default value     */
    public void revertToDefault() {
        FACTORY.revertToDefault();
        fireSettingsEvent(Type.REVERT_TO_DEFAULT);
    }
    
    /** Used to find any setting based on the key in the appropriate props file     */
    public Setting getSetting(String key) {
        synchronized(FACTORY) {
            for(Setting currSetting : FACTORY) {
                if(currSetting.getKey().equals(key))
                    return currSetting;
            }
        }
        return null; //unable the find the setting we are looking for
    }
    
    public String toString() {
        return FACTORY.toString();
    }
}
