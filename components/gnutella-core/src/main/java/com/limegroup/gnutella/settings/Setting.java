package com.limegroup.gnutella.settings;

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
     * The string which will be used to identify the setting the simpp message
     * is trying to set. For non-simppable setting this value must be null, for
     * simppable settings the setting must have a value which will NEVER change
     */
    private final String SIMPP_KEY;

    
	/**
	 * Constructs a new setting with the specified key and default
	 * value.  Private access ensures that only this class can construct
	 * new <tt>Setting</tt>s.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the defaultValue for the setting
     * @param simppKey the string used to identify a simpp setting. This must
     * have a valid value for simppable settings and must be null for
     * non-simppable settings.
	 * @throws <tt>IllegalArgumentException</tt> if the key for this 
	 *  setting is already contained in the map of default settings
	 */
	protected Setting(Properties defaultProps, Properties props, String key, 
                String defaultValue, String simppKey) {
		DEFAULT_PROPS = defaultProps;
		PROPS = props;
		KEY = key;
        SIMPP_KEY = simppKey;
		DEFAULT_VALUE = defaultValue;
		if(DEFAULT_PROPS.containsKey(key)) 
			throw new IllegalArgumentException("duplicate setting key");
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
     * Note: This is the method used by SimmSettingsManager to load the setting
     * with the value specified by Simpp 
     */
    protected void setValue(String value) {
        PROPS.put(KEY, value);
        loadValue(value);
    }

    public boolean isSimppEnabled() {
        return (SIMPP_KEY != null);
    }

    /**
     * Load value from property string value
     * @param sValue property string value
     */
    abstract protected void loadValue(String sValue);    

}
