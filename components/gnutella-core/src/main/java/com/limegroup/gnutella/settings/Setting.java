package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.util.Properties;
import java.io.*;


/**
 * Private abstract class for an individual setting.  Subclasses of this
 * class provide typing for settings.
 */
abstract class Setting {


	protected final Properties DEFAULT_PROPS;

	protected final Properties PROPS;

	/**
	 * The constant key for this property, specified upon construction.
	 */
	protected final String KEY;

	/**
	 * Constructs a new setting with the specified key and default
	 * value.  Private access ensures that only this class can construct
	 * new <tt>Setting</tt>s.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the defaultValue for the setting
	 * @throws <tt>IllegalArgumentException</tt> if the key for this 
	 *  setting is already contained in the map of default settings
	 */
	protected Setting(Properties defaultProps, Properties props, String key, String defaultValue) {
		DEFAULT_PROPS = defaultProps;
		PROPS = props;
		KEY = key;
		if(DEFAULT_PROPS.containsKey(key)) {
			throw new IllegalArgumentException("duplicate setting key");
		}
		DEFAULT_PROPS.put(KEY, defaultValue);
	}
}


// /**
//  * Class for a boolean setting.
//  */
// public final class BooleanSetting extends Setting {

// 	/**
// 	 * Creates a new <tt>SettingBool</tt> instance with the specified
// 	 * key and defualt value.
// 	 *
// 	 * @param key the constant key to use for the setting
// 	 * @param defaultBool the default value to use for the setting
// 	 */
// 	BooleanSetting(Properties defaultProps, Properties props, String key, boolean defaultBool) {
// 		super(defaultProps, props, key, String.valueOf(defaultBool));
// 	}
        
// 	/**
// 	 * Accessor for the value of this setting.
// 	 * 
// 	 * @return the value of this setting
// 	 */
// 	public boolean getValue() {
// 		return Boolean.valueOf(PROPS.getProperty(KEY)).booleanValue();
// 	}

// 	/**
// 	 * Mutator for this setting.
// 	 *
// 	 * @param bool the <tt>boolean</tt> to store
// 	 */
// 	public void setValue(boolean bool) {
// 		PROPS.put(KEY, String.valueOf(bool));
// 	}
// }

// /**
//  * Class for a string setting.
//  */
// public final class StringSetting extends Setting {

// 	/**
// 	 * Creates a new <tt>SettingBool</tt> instance with the specified
// 	 * key and defualt value.
// 	 *
// 	 * @param key the constant key to use for the setting
// 	 * @param defaultStr the default value to use for the setting
// 	 */
// 	StringSetting(Properties defaultProps, Properties props, String key, String defaultStr) {
// 		super(defaultProps, props, key, defaultStr);
// 	}
        
// 	/**
// 	 * Accessor for the value of this setting.
// 	 * 
// 	 * @return the value of this setting
// 	 */
// 	public String getValue() {
// 		return PROPS.getProperty(KEY);
// 	}

// 	/**
// 	 * Mutator for this setting.
// 	 *
// 	 * @param str the <tt>String</tt> to store
// 	 */
// 	public void setValue(String str) {
// 		PROPS.put(KEY, str);
// 	}
// }

// /**
//  * Class for an int setting.
//  */
// public final class IntSetting extends Setting {

// 	/**
// 	 * Creates a new <tt>SettingBool</tt> instance with the specified
// 	 * key and defualt value.
// 	 *
// 	 * @param key the constant key to use for the setting
// 	 * @param defaultInt the default value to use for the setting
// 	 */
// 	IntSetting(Properties defaultProps, Properties props, String key, int defaultInt) {
// 		super(defaultProps, props, key, String.valueOf(defaultInt));
// 	}
        
// 	/**
// 	 * Accessor for the value of this setting.
// 	 * 
// 	 * @return the value of this setting
// 	 */
// 	public int getValue() {
// 		return Integer.parseInt(PROPS.getProperty(KEY));
// 	}

// 	/**
// 	 * Mutator for this setting.
// 	 *
// 	 * @param value the value to store
// 	 */
// 	public void setValue(int value) {
// 		PROPS.put(KEY, String.valueOf(value));
// 	}
// }

// /**
//  * Class for a byte setting.
//  */
// public final class ByteSetting extends Setting {

// 	/**
// 	 * Creates a new <tt>SettingBool</tt> instance with the specified
// 	 * key and defualt value.
// 	 *
// 	 * @param key the constant key to use for the setting
// 	 * @param defaultByte the default value to use for the setting
// 	 */
// 	ByteSetting(Properties defaultProps, Properties props, String key, byte defaultByte) {
// 		super(defaultProps, props, key, String.valueOf(defaultByte));
// 	}
        
// 	/**
// 	 * Accessor for the value of this setting.
// 	 * 
// 	 * @return the value of this setting
// 	 */
// 	public byte getValue() {
// 		return Byte.parseByte(PROPS.getProperty(KEY));
// 	}

// 	/**
// 	 * Mutator for this setting.
// 	 *
// 	 * @param value the value to store
// 	 */
// 	public void setValue(byte value) {
// 		PROPS.put(KEY, String.valueOf(value));
// 	}
// }

// /**
//  * Class for a long setting.
//  */
// public final class LongSetting extends Setting {

// 	/**
// 	 * Creates a new <tt>SettingBool</tt> instance with the specified
// 	 * key and defualt value.
// 	 *
// 	 * @param key the constant key to use for the setting
// 	 * @param defaultLong the default value to use for the setting
// 	 */
// 	LongSetting(Properties defaultProps, Properties props, String key, long defaultLong) {
// 		super(defaultProps, props, key, String.valueOf(defaultLong));
// 	}
        
// 	/**
// 	 * Accessor for the value of this setting.
// 	 * 
// 	 * @return the value of this setting
// 	 */
// 	public long getValue() {
// 		return Long.parseLong(PROPS.getProperty(KEY));
// 	}

// 	/**
// 	 * Mutator for this setting.
// 	 *
// 	 * @param value the value to store
// 	 */
// 	public void setValue(long value) {
// 		PROPS.put(KEY, String.valueOf(value));
// 	}
// }


// /**
//  * Class for a file setting.
//  */
// public final class FileSetting extends Setting {

// 	/**
// 	 * Creates a new <tt>SettingBool</tt> instance with the specified
// 	 * key and defualt value.
// 	 *
// 	 * @param key the constant key to use for the setting
// 	 * @param defaultFile the default value to use for the setting
// 	 */
// 	FileSetting(Properties defaultProps, Properties props, String key, File defaultFile) {
// 		super(defaultProps, props, key, defaultFile.getAbsolutePath());
// 	}
        
// 	/**
// 	 * Accessor for the value of this setting.
// 	 * 
// 	 * @return the value of this setting
// 	 */
// 	public File getValue() {
// 		return new File(PROPS.getProperty(KEY));
// 	}

// 	/**
// 	 * Mutator for this setting.
// 	 *
// 	 * @param value the value to store
// 	 */
// 	public void setValue(File value) {
// 		PROPS.put(KEY, value.getAbsolutePath());
// 	}
// }

