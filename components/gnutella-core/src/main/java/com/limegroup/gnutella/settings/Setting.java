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
     * Value for whether or not this setting can be changed by SIMPP. Default is
     * false, so setting that want to be SIMPP enabled have to specifically say
     * so.  
     */
    private final boolean SIMPP_ENABLED;
    
	/**
	 * Constructs a new setting with the specified key and default
	 * value.  Private access ensures that only this class can construct
	 * new <tt>Setting</tt>s.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the defaultValue for the setting
     * @param simppEnabled whether or not this setting should be SIMPP settable
	 * @throws <tt>IllegalArgumentException</tt> if the key for this 
	 *  setting is already contained in the map of default settings
	 */
	protected Setting(Properties defaultProps, Properties props, String key, 
                      String defaultValue, boolean simppEnabled) {
		DEFAULT_PROPS = defaultProps;
		PROPS = props;
		KEY = key;
        SIMPP_ENABLED = simppEnabled;
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
     */
    protected void setValue(String value) {
        PROPS.put(KEY, value);
        loadValue(value);
    }

    public boolean isSimppEnabled() {
        return SIMPP_ENABLED;
    }

    /**
     * Load value from property string value
     * @param sValue property string value
     */
    abstract protected void loadValue(String sValue);
}
