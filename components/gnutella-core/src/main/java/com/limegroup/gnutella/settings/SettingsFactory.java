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
     * <tt>Properties</tt> instance for the defualt values.
     */
    protected final Properties DEFAULT_PROPS;

    /**
     * The <tt>Properties</tt> instance containing all settings.
     */
	protected final Properties PROPS;

	
	/**
	 * Creates a new <tt>SettingsFactory</tt> instance with the specified file
	 * to read from and write to.
	 *
	 * @param settingsFile the file to read from and to write to
	 */
	SettingsFactory(File settingsFile, Properties defaultProps, Properties props) {
		DEFAULT_PROPS = defaultProps;
		PROPS = props;		
		reload(settingsFile);
	}

	/**
	 * Factory method for creating a <tt>SettingsFactory</tt> instance for the
	 * specified properties file and with the specified default <tt>Properties</tt>
	 * instance.
	 */
	static SettingsFactory createFromFile(File file, Properties defaultProps) {
		return new SettingsFactory(file, defaultProps, new Properties(defaultProps));
	}

	/**
	 * Reloads the settings with the specified settings file from disk.
	 *
	 * @param settingsStream the <tt>InputStream</tt> to load
	 */
	public void reload(File file) {
        try {
            PROPS.load(new FileInputStream(file));
        } catch(IOException e) {
			e.printStackTrace();
            // the default properties will be used -- this is fine and expected
        }		
	}


	/**
	 * Creates a new <tt>StringSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public StringSetting createStringSetting(String key, String defaultValue) {
		return new StringSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}

	/**
	 * Creates a new <tt>BooleanSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public BooleanSetting createBooleanSetting(String key, boolean defaultValue) {
		return new BooleanSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}

	/**
	 * Creates a new <tt>IntSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public IntSetting createIntSetting(String key, int defaultValue) {
		return new IntSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}


	/**
	 * Creates a new <tt>ByteSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public ByteSetting createByteSetting(String key, byte defaultValue) {
		return new ByteSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}


	/**
	 * Creates a new <tt>LongSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public LongSetting createLongSetting(String key, long defaultValue) {
		return new LongSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}


	/**
	 * Creates a new <tt>FileSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public FileSetting createFileSetting(String key, File defaultValue) {
		File parent = new File(defaultValue.getParent());
		if(!parent.isDirectory()) parent.mkdirs();

		return new FileSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}

	/**
	 * Creates a new <tt>ColorSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public ColorSetting createColorSetting(String key, Color defaultValue) {
		return ColorSetting.createColorSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}
}
