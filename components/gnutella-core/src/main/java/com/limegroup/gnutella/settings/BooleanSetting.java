package com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for a boolean setting.
 */
public final class BooleanSetting extends Setting {
    
    /** Curernve value of settings */
    private boolean value;

	/**
	 * Creates a new <tt>BooleanSetting</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultBool the default value to use for the setting
	 */
	BooleanSetting(Properties defaultProps, Properties props, String key, 
				   boolean defaultBool) {
		super(defaultProps, props, key, String.valueOf(defaultBool));
	}
        
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public boolean getValue() {
		return value;
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param bool the <tt>boolean</tt> to store
	 */
	public void setValue(boolean bool) {
        super.setValue(String.valueOf(bool));
	}
    
    /**
     * Load value from property string value
     * @param sValue property string value
     */
    protected void loadValue(String sValue) {
        value = Boolean.valueOf(sValue).booleanValue();
    }
}
