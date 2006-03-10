package com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for a byte setting.
 */
public final class ByteSetting extends AbstractNumberSetting {
    
    private byte value;

	/**
	 * Creates a new <tt>SettingBool</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultByte the default value to use for the setting
	 */
	ByteSetting(Properties defaultProps, Properties props, String key, 
                                                             byte defaultByte) {
		super(defaultProps, props, key, String.valueOf(defaultByte), 
                                                             null, null, null);
	}


	ByteSetting(Properties defaultProps, Properties props, String key, 
                byte defaultByte, String simppKey, byte min, byte max) {
		super(defaultProps, props, key, String.valueOf(defaultByte), 
              simppKey, new Byte(min), new Byte(max) );
	}
        
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public byte getValue() {
		return value;
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(byte value) {
		super.setValue(String.valueOf(value));
	}
     
    /**
     * Load value from property string value
     * @param sValue property string value
     */
    protected void loadValue(String sValue) {
        try {
            value = Byte.parseByte(sValue.trim());
        } catch(NumberFormatException nfe) {
            revertToDefault();
        }
    }
    
    protected Comparable convertToComparable(String value) {
        return new Byte(value);
    }
}
