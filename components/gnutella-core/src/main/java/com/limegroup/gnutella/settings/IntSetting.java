package com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for an int setting.
 */
public final class IntSetting extends Setting {
    
    private int value;

	/**
	 * Creates a new <tt>IntSetting</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultInt the default value to use for the setting
	 */
	IntSetting(Properties defaultProps, Properties props, String key, 
                                           int defaultInt, String simppKey) {
		super(defaultProps, props, key, String.valueOf(defaultInt), simppKey);
	}
        
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public int getValue() {
        return value;
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(int value) {
		super.setValue(String.valueOf(value));
	}
    
    /** Load value from property string value
     * @param sValue property string value
     *
     */
    protected void loadValue(String sValue) {
        try {
            value = Integer.parseInt(sValue.trim());
        } catch(NumberFormatException nfe) {
            revertToDefault();
        }
    }
    
}
