package com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for a byte setting.
 */
public final class ByteSetting extends Setting {

	/**
	 * Creates a new <tt>SettingBool</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultByte the default value to use for the setting
	 */
	ByteSetting(Properties defaultProps, Properties props, String key, byte defaultByte) {
		super(defaultProps, props, key, String.valueOf(defaultByte));
	}
        
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public byte getValue() {
		return Byte.parseByte(PROPS.getProperty(KEY));
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(byte value) {
		PROPS.put(KEY, String.valueOf(value));
	}
}
