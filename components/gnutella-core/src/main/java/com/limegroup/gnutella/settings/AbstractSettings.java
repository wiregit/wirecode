package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.*;
import java.util.Properties;
import java.io.*;


/**
 * This class is the superclass for all LimeWire "general" settings classes.
 * General settings are all stored in limewire.props in the appropriate
 * platform-specific directory.  This class allows settings to be conveniently
 * grouped into subclasses by setting type.  For example, a "DownloadSettings"
 * subclass could contain any settings specifically for downloads.
 */
abstract class AbstractSettings {
   

    /**
	 * Default name for the properties file.
	 */
    private static final String PROPS_NAME = "limewire.props";
    
    /**
     * properties file
     */
    private static final File PROPS_FILE;
    
    /**
     * Default name for configuration file.
     */
    private static final String CONFIG_NAME = "config.props";
    
	/**
	 * Constant for the <tt>SettingsFactory</tt> that subclasses can use
	 * to create new settings which will be stored in the properties file.
     * This should be reserved for settings taht are user maintained or 
     * otherwise record a state that should be carried forward from one 
     * session to the next.
	 */
	protected static final SettingsFactory FACTORY;
    
    /**
     * Constant for the <tt>SettingsFactory</tt> that subclasses can use
     * to create new settings which are not user maintained are read from
     * the LimeWire configuration file instead of the property file.
     */
    protected static final SettingsFactory CFG_FACTORY;
    
    static {
        File settingsDir = CommonUtils.getUserSettingsDir();
        PROPS_FILE = new File(settingsDir, PROPS_NAME);
        FACTORY = new SettingsFactory(PROPS_FILE, "LimeWire properties file");
        CFG_FACTORY = new SettingsFactory(new File(settingsDir, CONFIG_NAME));
    }

    /**
     * Accessor for the <tt>Properties</tt> instance that stores all settings.
     *
     * @return the <tt>Properties</tt> instance for storing settings
     */
	public static Properties getProperties() {
        return FACTORY.getProperties();
	}
    
    /**
     * Accessor for the <tt>File</tt> instance taht stores all properties
     */
    public static File getPropertiesFile() {
        return PROPS_FILE;
    }
    
    /**
     * reload settings from both the property and configuration files
     */
    public static void reload() {
        FACTORY.reload();
        CFG_FACTORY.reload();
    }
    
    /**
     * Save property settings to the property file
     */
    public static void save() {
        FACTORY.save();
    }
}
