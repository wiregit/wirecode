package org.limewire.setting;

import java.util.Properties;


/**
 * Class for a setting that's an array of chars.
 */
public final class CharArraySetting extends Setting {
    
    /**
     * Cached value.
     */
    private char[] value;


	/**
	 * Creates a new <tt>SettingBool</tt> instance with the specified
	 * key and defualt value.
	 *
     * @param defaultProps the default properties
     * @param props the set properties
	 * @param key the constant key to use for the setting
	 * @param defaultValue the default value to use for the setting
	 */
    CharArraySetting(Properties defaultProps, Properties props, 
                               String key, char[] defaultValue) {
        super(defaultProps, props, key, new String(defaultValue));
    }

	CharArraySetting(Properties defaultProps, Properties props, String key, 
                                                          String defaultValue) {
		super(defaultProps, props, key, defaultValue);
	}
   
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public char[] getValue() {
		return value;
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(char[] value) {
		super.setValue(new String(value));
	}
     
    /**
     * Load value from property string value
     * @param sValue property string value
     */
    protected void loadValue(String sValue) {
        value = sValue.trim().toCharArray();
    }

}
