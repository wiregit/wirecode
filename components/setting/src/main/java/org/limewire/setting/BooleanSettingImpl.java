package org.limewire.setting;

import java.util.Properties;


/**
 * Provides a boolean setting value. As a subclass of 
 * <code>Setting</code>, the setting has a key.
 * <p>
 * You can create a <code>BooleanSetting</code> object with a 
 * {@link SettingsFactory#createBooleanSetting(String, boolean)}.
 */
public final class BooleanSettingImpl extends AbstractSetting<Boolean> implements BooleanSetting {
    
    /** Current value of the setting. */
    private volatile boolean value;

	/**
	 * Creates a new <tt>BooleanSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultBool the default value to use for the setting
	 */
	BooleanSettingImpl(Properties defaultProps, Properties props, String key, 
                                                          boolean defaultBool) {
		super(defaultProps, props, key, String.valueOf(defaultBool)); 
	}
	
	@Override
	public Boolean get() {
	    return getValue();
	}
	
	@Override
	public void set(Boolean newValue) {
	    setValue(newValue);
	}
 
	/* (non-Javadoc)
     * @see org.limewire.setting.BooleanSetting#getValue()
     */
	public boolean getValue() {
		return value;
	}

	/* (non-Javadoc)
     * @see org.limewire.setting.BooleanSetting#setValue(boolean)
     */
	public void setValue(boolean bool) {
	    setValueInternal(String.valueOf(bool));
	}
    
    /**
     * Load value from property string value
     * @param sValue property string value
     */
    @Override
    protected void loadValue(String sValue) {
        value = Boolean.valueOf(sValue.trim());
    }
	
	/* (non-Javadoc)
     * @see org.limewire.setting.BooleanSetting#invert()
     */
	public void invert() {
		setValue(!getValue());
	}
    
    @Override
    public Object inspect() {
        return get();
    }
}
