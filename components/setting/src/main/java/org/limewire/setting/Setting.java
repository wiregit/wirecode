package org.limewire.setting;

import java.util.Properties;


/**
 * Private abstract class for an individual setting.  Subclasses of this
 * class provide typing for settings.
 */
public abstract class Setting {


	/**
	 * Protected default <tt>Properties</tt> instance for subclasses.
	 */
	protected final Properties DEFAULT_PROPS;

	/**
	 * Protected <tt>Properties</tt> instance containing properties for any
	 * subclasses.
	 */
	protected final Properties PROPS;

	/**
	 * The constant key for this property, specified upon construction.
	 */
	protected final String KEY;

	/**
	 * Constant for the default value for this <tt>Setting</tt>.
	 */
	protected final String DEFAULT_VALUE;
    
	/**
	 * Value for whether or not this setting should always save.
	 */
	private boolean _alwaysSave = false;
	
	/**
	 * Setting for whether or not this setting is private and should
	 * not be reported in bug reports.
	 */
	private boolean _isPrivate = false;

    
	/**
	 * Constructs a new setting with the specified key and default
	 * value.  Private access ensures that only this class can construct
	 * new <tt>Setting</tt>s.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the defaultValue for the setting
     * @param remoteKey the string used to identify a remote setting. This must
     * have a valid value for remoteable settings and must be null for
     * non-remoteable settings.
	 * @throws <tt>IllegalArgumentException</tt> if the key for this 
	 *  setting is already contained in the map of default settings
	 */
	protected Setting(Properties defaultProps, Properties props, String key, 
                String defaultValue) {
		DEFAULT_PROPS = defaultProps;
		PROPS = props;
		KEY = key;
		DEFAULT_VALUE = defaultValue;
		if(DEFAULT_PROPS.containsKey(key)) 
			throw new IllegalArgumentException("duplicate setting key: " + key);
		DEFAULT_PROPS.put(KEY, defaultValue);
        loadValue(defaultValue);
	}
    
    /**
     * Reload value from properties object
     */
    public void reload() {
        String value = PROPS.getProperty(KEY);
        if (value == null) value = DEFAULT_VALUE;
        loadValue(value);
    }

	/**
	 * Revert to the default value.
	 * It is critically important that the DEFAULT_VALUE is valid,
	 * otherwise an infinite loop will be encountered when revertToDefault
	 * is called, as invalid values call revertToDefault.
	 * Because default values are hard-coded into the program, this is okay.
	 */
	public void revertToDefault() {
        setValue(DEFAULT_VALUE);
	}
	
	/**
	 * Determines whether or not this value should always be saved to disk.
	 */
    public boolean shouldAlwaysSave() {
        return _alwaysSave;
    }
    
    /**
     * Sets whether or not this setting should always save, even if
     * it is default.
     * Returns this so it can be used during assignment.
     */
    public Setting setAlwaysSave(boolean save) {
        _alwaysSave = save;
        return this;
    }
    
    /**
     * Sets whether or not this setting should be reported in bug reports.
     */
    public Setting setPrivate(boolean priv) {
        _isPrivate = priv;
        return this;
    }
    
    /**
     * Determines whether or not a setting is private.
     */
    public boolean isPrivate() {
        return _isPrivate;
    }
	
    /**
     * Determines whether or not the current value is the default value.
     */
    public boolean isDefault() {
        String value = PROPS.getProperty(KEY);
        if (value == null)
            return false;
        return value.equals(DEFAULT_PROPS.getProperty(KEY));
    }
    
    /**
     * Get the key for this setting.
     */
    public String getKey() {
        return KEY;
    }
    
    /**
     * Returns the value as stored in the properties file.
     */
    public String getValueAsString() {
        return PROPS.getProperty(KEY);
    }
    
    /**
     * Set new property value
     * @param value new property value
     * 
     * NOTE: This is protected so that only this package
     * can update all kinds of settings using a String value.
     * StringSetting updates the access to public.
     */
    protected void setValue(String value) {
        PROPS.put(KEY, value);
        loadValue(value);
    }

    /**
     * Load value from property string value
     * @param sValue property string value
     */
    abstract protected void loadValue(String sValue);    

}
