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


	/**
	 * Protected default <tt>Properties</tt> instance for subclasses.
	 */
	protected final Properties DEFAULT_PROPS;

	/**
	 * Protected <tt>Properties</tt> instance containing properties for any
	 * subclasses.
	 */
	protected final Properties PROPS;

	/**
	 * The constant key for this property, specified upon construction.
	 */
	protected final String KEY;

	/**
	 * Constant for the default value for this <tt>Setting</tt>.
	 */
	protected final String DEFAULT_VALUE;

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
		DEFAULT_VALUE = defaultValue;
		if(DEFAULT_PROPS.containsKey(key)) {
			throw new IllegalArgumentException("duplicate setting key");
		}
		DEFAULT_PROPS.put(KEY, defaultValue);
	}

	/**
	 * Revert to the default value.
	 */
	public void revertToDefault() {
		PROPS.put(KEY, DEFAULT_VALUE);
	}
}
