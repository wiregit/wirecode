package com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for an int setting.
 */
public final class IntSetting extends Setting {

	/**
	 * Creates a new <tt>SettingBool</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultInt the default value to use for the setting
	 */
	IntSetting(Properties defaultProps, Properties props, String key, int defaultInt) {
		super(defaultProps, props, key, String.valueOf(defaultInt));
	}
        
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public int getValue() {
		return Integer.parseInt(PROPS.getProperty(KEY));
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(int value) {
		PROPS.put(KEY, String.valueOf(value));
	}
}
