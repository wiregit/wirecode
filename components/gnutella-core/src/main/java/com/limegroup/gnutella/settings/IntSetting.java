package com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for an int setting.
 */
public final class IntSetting extends AbstractNumberSetting {
    
    private int value;

	/**
	 * Creates a new <tt>IntSetting</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultInt the default value to use for the setting
	 */
	IntSetting(Properties defaultProps, Properties props, String key, 
                                                              int defaultInt) {
        super(defaultProps, props, key, String.valueOf(defaultInt), 
                                                            null, null, null);
	}

    /**
     * Constructor for Settable setting which specifies a simpp-key and max and
     * min permissible values.
     */
	IntSetting(Properties defaultProps, Properties props, String key, 
          int defaultInt, String simppKey, int maxSimppVal, int minSimppVal) {
		super(defaultProps, props, key, String.valueOf(defaultInt), simppKey,
                            new Integer(maxSimppVal), new Integer(minSimppVal));
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
    
    protected boolean isInRange(String value) {
        int max = ((Integer)MAX_VALUE).intValue();
        int min = ((Integer)MIN_VALUE).intValue();
        int val = Integer.parseInt(value);
        return (val <= max && val >= min);
    }
}
