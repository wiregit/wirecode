package org.limewire.setting;

import java.util.Properties;


/**
 * Contains a byte value key-value pair.
 * <p>
 * Create a <code>ByteSetting</code> object with a {@link SettingsFactory}.
 */
public final class ByteSetting extends AbstractNumberSetting<Byte> {
    
    private byte value;

	/**
	 * Creates a new <tt>SettingBool</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultByte the default value to use for the setting
	 */
	ByteSetting(Properties defaultProps, Properties props, String key, 
                                                             byte defaultByte) {
		super(defaultProps, props, key, String.valueOf(defaultByte), 
                                                             false, null, null);
	}

	ByteSetting(Properties defaultProps, Properties props, String key, 
                byte defaultByte, byte min, byte max) {
		super(defaultProps, props, key, String.valueOf(defaultByte), 
              true, new Byte(min), new Byte(max) );
	}
        
	/**
	 * Assessor for the value of this setting.
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
    protected Comparable<Byte> convertToComparable(String value) {
        return new Byte(value);
    }
}
