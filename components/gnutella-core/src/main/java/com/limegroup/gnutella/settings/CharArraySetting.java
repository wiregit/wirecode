package com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for a setting that's an array of chars.
 */
public final class CharArraySetting extends Setting {
    
    /**
     * Cached value.
     */
    private char[] value;

    public static final char[] DUMMY_CHAR_ARRAY = new char[0];


	/**
	 * Creates a new <tt>SettingBool</tt> instance with the specified
	 * key and defualt value.
	 *
     * @param defaultProps the default properties
     * @param props the set properties
	 * @param key the constant key to use for the setting
	 * @param defaultValue the default value to use for the setting
	 */
    static CharArraySetting 
        createCharArraySetting(Properties defaultProps, Properties props, 
                               String key, char[] defaultValue) {
        return new CharArraySetting(defaultProps, props, key, 
                                           new String(defaultValue));
    }

	CharArraySetting(Properties defaultProps, Properties props, String key, 
                                                          String defaultValue) {
		super(defaultProps, props, key, defaultValue, null, null, null);
	}
     
	CharArraySetting(Properties defaultProps, Properties props, String key, 
                 char[] defaultValue, String simppKey, char[] max, char[] min) {
		super(defaultProps, props, key, new String(defaultValue), 
                                                           simppKey, max, min);
        if(max != DUMMY_CHAR_ARRAY || min != DUMMY_CHAR_ARRAY)
            throw new IllegalArgumentException("illegal max or min in setting");
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

    protected boolean isInRange(String value) {
        //cannot handle ranges for char arrays. Just return true
        return true;
    }
}
