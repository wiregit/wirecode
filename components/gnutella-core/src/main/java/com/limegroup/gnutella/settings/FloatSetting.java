package com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for an float setting.
 */
public final class FloatSetting extends Setting {
    
    private float value;

	/**
	 * Creates a new <tt>FloatSetting</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultFloat the default value to use for the setting
	 */
	FloatSetting(Properties defaultProps, Properties props, String key, 
                                        float defaultFloat, String simppKey) {
		super(defaultProps, props, key, String.valueOf(defaultFloat), simppKey);
	}
        
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public float getValue() {
        return value;
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(float value) {
		super.setValue(String.valueOf(value));
	}
    
    /** Load value from property string value
     * @param sValue property string value
     *
     */
    protected void loadValue(String sValue) {
        try {
            value = Float.valueOf(sValue.trim()).floatValue();
        } catch(NumberFormatException nfe) {
            revertToDefault();
        }
    }
}
