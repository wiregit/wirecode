package com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for a long setting.
 */
public final class LongSetting extends Setting {

	/**
	 * Creates a new <tt>SettingBool</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultLong the default value to use for the setting
	 */
	LongSetting(Properties defaultProps, Properties props, String key, long defaultLong) {
		super(defaultProps, props, key, String.valueOf(defaultLong));
	}
        
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public long getValue() {
		return Long.parseLong(PROPS.getProperty(KEY));
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(long value) {
		PROPS.put(KEY, String.valueOf(value));
	}
}
