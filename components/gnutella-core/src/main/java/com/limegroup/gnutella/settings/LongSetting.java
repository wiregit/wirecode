package com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for a long setting.
 */
public final class LongSetting extends AbstractNumberSetting {
    
    private long value;

	/**
	 * Creates a new <tt>LongSetting</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultLong the default value to use for the setting
	 */
	LongSetting(Properties defaultProps, Properties props, String key, 
                                         long defaultLong) {
		super(defaultProps, props, key, String.valueOf(defaultLong), 
                                                              null, null, null);
	}


	LongSetting(Properties defaultProps, Properties props, String key, 
                long defaultLong, String simppSetting, long max, long min) {
		super(defaultProps, props, key, String.valueOf(defaultLong), 
                                 simppSetting, new Long(max), new Long(min) );
	}
        
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public long getValue() {
        return value;
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(long value) {
		super.setValue(String.valueOf(value));
	}
    
    /** Load value from property string value
     * @param sValue property string value
     *
     */
    protected void loadValue(String sValue) {
        try {
            value = Long.parseLong(sValue.trim());
        } catch(NumberFormatException nfe) {
            revertToDefault();
        }
    }

    protected boolean isInRange(String value) {
        long max = ((Long)MAX_VALUE).longValue();
        long min = ((Long)MIN_VALUE).longValue();
        long val = Long.parseLong(value);
        return (val <= max && val >= min);
    }
}
