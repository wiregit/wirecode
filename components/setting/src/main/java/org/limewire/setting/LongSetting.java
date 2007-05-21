package org.limewire.setting;

import java.util.Properties;


/**
 * Provides a long value key-value pair.
 * <p>
 * Create a <code>LongSetting</code> object with a {@link SettingsFactory}.
 */
public class LongSetting extends AbstractNumberSetting<Long> {
    
    private long value;

	/**
	 * Creates a new <code>LongSetting</code> instance with the specified
	 * key and default value.
	 */
	LongSetting(Properties defaultProps, Properties props, String key, 
                                         long defaultLong) {
		super(defaultProps, props, key, String.valueOf(defaultLong), 
                                                              false, null, null);
	}

	LongSetting(Properties defaultProps, Properties props, String key, 
                long defaultLong, long min, long max) {
		super(defaultProps, props, key, String.valueOf(defaultLong), 
                                 true, new Long(min), new Long(max) );
	}
        
	/**
	 * Assessor for the value of this setting.
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
    
    protected Comparable<Long> convertToComparable(String value) {
        return new Long(value);
    }
}
