package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.util.zip.*;
import java.util.Properties;
import java.io.*;
import java.awt.*;
import com.sun.java.util.collections.*;

/**
 * Class for handling all LimeWire settings that are stored to disk.  To
 * add a new setting, simply add a new public static member to the list
 * of settings.  Each setting constructor takes the name of the key and 
 * the default value, and all settings are typed.  Choose the correct 
 * <tt>Setting</tt> subclass for your setting type.  It is also important
 * to choose a unique string key for your setting name -- otherwise there
 * will be conflicts.
 */
public final class SettingsFactory {
    
    /** 
	 * <tt>File</tt> object from which settings are loaded and saved 
	 */    
    private File SETTINGS_FILE;
    
    private String HEADING;

    /**
     * <tt>Properties</tt> instance for the defualt values.
     */
    protected final Properties DEFAULT_PROPS = new Properties();

    /**
     * The <tt>Properties</tt> instance containing all settings.
     */
	protected final Properties PROPS = new Properties(DEFAULT_PROPS);
    
    /* List of all settings associated with this factory 
     * LOCKING: must hold this monitor
     */
    private ArrayList /* of Settings */ settings = new ArrayList(10);

	
	/**
	 * Creates a new <tt>SettingsFactory</tt> instance with the specified file
	 * to read from and write to.
	 *
	 * @param settingsFile the file to read from and to write to
	 */
	SettingsFactory(File settingsFile) {
        this(settingsFile, null);
    }
    
	/**
	 * Creates a new <tt>SettingsFactory</tt> instance with the specified file
	 * to read from and write to.
	 *
	 * @param settingsFile the file to read from and to write to
     * @param heading heading to use when writing property file
	 */
	SettingsFactory(File settingsFile, String heading) {
        SETTINGS_FILE = settingsFile;
        HEADING = heading;
		reload();
	}

	/**
	 * Reloads the settings with the specified settings file from disk.
	 *
	 * @param settingsStream the <tt>InputStream</tt> to load
	 */
	public synchronized void reload() {
		// If the props file doesn't exist, the init sequence will prompt
		// the user for the required values, so return.  If this is not 
		// loading limewire.props, but rather something like themes.txt,
		// we also return, as attempting to load an invalid file will
		// not do any good.
		if(!SETTINGS_FILE.isFile()) return;
        try {
            PROPS.load(new FileInputStream(SETTINGS_FILE));
        } catch(IOException ex) {
			RouterService.getCallback().error(ActivityCallback.INTERNAL_ERROR, ex);
            // the default properties will be used -- this is fine and expected
        }		
        
        // Reload all setting values
        Iterator ii = settings.iterator(); 
        while (ii.hasNext()) {
            Setting set = (Setting)ii.next();
            set.reload();
        }
	}
    
    /**
     * Save setting information to property file
     */
    public void save() {
        try {
            PROPS.store(new FileOutputStream(SETTINGS_FILE), HEADING);
        } catch (IOException ex) {
			RouterService.getCallback().error(ActivityCallback.INTERNAL_ERROR, ex);
        }
    }
    
    /**
     * Return settings properties
     */
    Properties getProperties() {
        return PROPS;
    }

	/**
	 * Creates a new <tt>StringSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized StringSetting createStringSetting(String key, String defaultValue) {
		StringSetting result = 
			new StringSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
	}

	/**
	 * Creates a new <tt>BooleanSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized BooleanSetting createBooleanSetting(String key, boolean defaultValue) {
		BooleanSetting result = 
			new BooleanSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
	}

	/**
	 * Creates a new <tt>IntSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized IntSetting createIntSetting(String key, int defaultValue) {
		IntSetting result = 
                new IntSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
	}


	/**
	 * Creates a new <tt>ByteSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized ByteSetting createByteSetting(String key, byte defaultValue) {
		ByteSetting result = 
                new ByteSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
	}


	/**
	 * Creates a new <tt>LongSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized LongSetting createLongSetting(String key, long defaultValue) {
		 LongSetting result = 
                new LongSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
         settings.add(result);
         result.reload();
         return result;
	}


	/**
	 * Creates a new <tt>FileSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized FileSetting createFileSetting(String key, File defaultValue) {
		File parent = new File(defaultValue.getParent());
		if(!parent.isDirectory()) parent.mkdirs();

		FileSetting result = 
                new FileSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
	}

	/**
	 * Creates a new <tt>ColorSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized ColorSetting createColorSetting(String key, Color defaultValue) {
		ColorSetting result = 
            ColorSetting.createColorSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
	}
}
