package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.*;
import java.util.Properties;
import java.io.*;


/**
 * This controls all 'Do not ask this again' or
 * 'Always use this answer' questions.
 */
public class QuestionsHandler {    
    /**
     * properties file
     */
    private static final File PROPS_FILE;
    
    /**
     * Name for file that stores answers to
     * all  the 'do not ask this again' or 'always use this answer' questions
     * 
     */
    private static final String FILE_NAME = "questions.props";    
    
	/**
	 * Constant for the <tt>SettingsFactory</tt> that subclasses can use
	 * to create new settings which will be stored in the properties file.
	 */
	protected static final SettingsFactory FACTORY;
    
    /**
     * Value for whether or not settings should be saved to file.
     */
    protected static boolean _shouldSave = true;
    
    static {
        File settingsDir = CommonUtils.getUserSettingsDir();
        PROPS_FILE = new File(settingsDir, FILE_NAME);
        FACTORY = new SettingsFactory(PROPS_FILE, "LimeWire properties file");
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
    }
    
    /**
     * Save property settings to the property file
     */
    public static void save() {
        if ( _shouldSave) {
            FACTORY.save();
        }
    }
    
    /**
     * Revert all settings to their default value
     */
    public static void revertToDefault() {
        FACTORY.revertToDefault();
    }
    
    /**
     * Mutator for shouldSave
     */
    public static void setShouldSave(boolean shouldSave) {
        _shouldSave = shouldSave;
    }
    
    /**
     * Access for shouldSave
     */
    public static boolean getShouldSave() {
        return _shouldSave;
    }
    
    //////////// The actual questions ///////////////
    
    /** 
    * Setting for whether or not to allow multiple instances of LimeWire.
    */ 
    public static final BooleanSetting MONITOR_VIEW = 
        FACTORY.createBooleanSetting("MONITOR_VIEW", false);

    /**
     * Setting for whether or not to ask if you want to delete files.
     */
    public static final IntSetting SHOULD_DELETE_FILE =
        FACTORY.createIntSetting("SHOULD_DELETE_FILE", 0);
    
}
