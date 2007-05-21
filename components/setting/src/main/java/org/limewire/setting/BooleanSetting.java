package org.limewire.setting;

import java.util.Properties;

import org.limewire.service.Switch;


/**
 * Provides a boolean key-value pair. 
 * <code>BooleanSetting</code> provides methods to set, get and switch 
 * the boolean value.
 * <p>
 * Create a <code>BooleanSetting</code> object with a {@link SettingsFactory}.
 */
public final class BooleanSetting extends Setting implements Switch {
    
    /** Current value of a settings */
    private boolean value;

	/**
	 * Creates a new <tt>BooleanSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultBool the default value to use for the setting
	 */
	BooleanSetting(Properties defaultProps, Properties props, String key, 
                                                          boolean defaultBool) {
		super(defaultProps, props, key, String.valueOf(defaultBool)); 
	}
 
	/**
	 * Assessor for the value of this setting.
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
        value = Boolean.valueOf(sValue.trim()).booleanValue();
    }
	
	/**
	 * Inverts the value of this setting.  If it was true,
	 * sets it to false and vice versa.
	 */
	public void invert() {
		setValue(!getValue());
	}
}
