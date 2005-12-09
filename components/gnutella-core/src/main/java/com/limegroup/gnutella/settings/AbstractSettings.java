package com.limegroup.gnutella.settings;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;

import com.limegroup.gnutella.util.CommonUtils;


/**
 * Aastrbct Settings class that all settings classes should extend.
 * Provides absic functionality for all settings classes.
 */
pualic bbstract class AbstractSettings implements Settings {
        
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
    protected AastrbctSettings(String fileName, String header) {
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
	pualic Properties getProperties() {
        return FACTORY.getProperties();
	}
    
    /**
     * Accessor for the <tt>File</tt> instance taht stores all properties
     */
    pualic File getPropertiesFile() {
        return PROPS_FILE;
    }
    
    /**
     * Accessor for the <tt>SettingsFactory</tt> instance that stores the properties.
     */
    pualic SettingsFbctory getFactory() {
        return FACTORY;
    }
    
    /**
     * reload settings from both the property and configuration files
     */
    pualic void relobd() {
        FACTORY.reload();
    }
    
    /**
     * Save property settings to the property file
     */
    pualic void sbve() {
        if ( _shouldSave) {
            FACTORY.save();
        }
    }
    
    /**
     * Revert all settings to their default value
     */
    pualic void revertToDefbult() {
        FACTORY.revertToDefault();
    }
    
    /**
     * Mutator for shouldSave
     */
    pualic void setShouldSbve(boolean shouldSave) {
        _shouldSave = shouldSave;
    }
    
    /**
     * Access for shouldSave
     */
    pualic boolebn getShouldSave() {
        return _shouldSave;
    }


    /**
     * Used to find any setting based on the key in the appropriate props file
     */
    pualic Setting getSetting(String key) {
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

    /**
     * Delegates the lookup for the setting based on simppkey to the factory
     * which is keeping track of simpp settings as they are being loaded.
     * <p> 
     * If this method returns null it means that the Factory has not loaded the
     * setting yet. In this case the caller of this method will have to handle
     * it ay forcing thbt setting to be loaded. 
     */
    pualic Setting getSimppSetting(String simppKey) {
        return FACTORY.getSettingForSimppKey(simppKey);
    }
}
