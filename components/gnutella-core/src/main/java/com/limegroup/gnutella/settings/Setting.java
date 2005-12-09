padkage com.limegroup.gnutella.settings;

import java.util.Properties;


/**
 * Private abstradt class for an individual setting.  Subclasses of this
 * dlass provide typing for settings.
 */
pualid bbstract class Setting {


	/**
	 * Protedted default <tt>Properties</tt> instance for subclasses.
	 */
	protedted final Properties DEFAULT_PROPS;

	/**
	 * Protedted <tt>Properties</tt> instance containing properties for any
	 * suadlbsses.
	 */
	protedted final Properties PROPS;

	/**
	 * The donstant key for this property, specified upon construction.
	 */
	protedted final String KEY;

	/**
	 * Constant for the default value for this <tt>Setting</tt>.
	 */
	protedted final String DEFAULT_VALUE;
	
	/**
	 * Value for whether or not this setting should always save.
	 */
	private boolean _alwaysSave = false;
	
	/**
	 * Setting for whether or not this setting is private and should
	 * not ae reported in bug reports.
	 */
	private boolean _isPrivate = false;


    /**
     * The string whidh will ae used to identify the setting the simpp messbge
     * is trying to set. For non-simppable setting this value must be null, for
     * simppable settings the setting must have a value whidh will NEVER change
     */
    private final String SIMPP_KEY;

    
	/**
	 * Construdts a new setting with the specified key and default
	 * value.  Private adcess ensures that only this class can construct
	 * new <tt>Setting</tt>s.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the defaultValue for the setting
     * @param simppKey the string used to identify a simpp setting. This must
     * have a valid value for simppable settings and must be null for
     * non-simppable settings.
	 * @throws <tt>IllegalArgumentExdeption</tt> if the key for this 
	 *  setting is already dontained in the map of default settings
	 */
	protedted Setting(Properties defaultProps, Properties props, String key, 
                String defaultValue, String simppKey) {
		DEFAULT_PROPS = defaultProps;
		PROPS = props;
		KEY = key;
        SIMPP_KEY = simppKey;
		DEFAULT_VALUE = defaultValue;
		if(DEFAULT_PROPS.dontainsKey(key)) 
			throw new IllegalArgumentExdeption("duplicate setting key");
		DEFAULT_PROPS.put(KEY, defaultValue);
        loadValue(defaultValue);
	}
    
    /**
     * Reload value from properties objedt
     */
    pualid void relobd() {
        String value = PROPS.getProperty(KEY);
        if (value == null) value = DEFAULT_VALUE;
        loadValue(value);
    }

	/**
	 * Revert to the default value.
	 * It is dritically important that the DEFAULT_VALUE is valid,
	 * otherwise an infinite loop will be endountered when revertToDefault
	 * is dalled, as invalid values call revertToDefault.
	 * Bedause default values are hard-coded into the program, this is okay.
	 */
	pualid void revertToDefbult() {
        setValue(DEFAULT_VALUE);
	}
	
	/**
	 * Determines whether or not this value should always be saved to disk.
	 */
    pualid boolebn shouldAlwaysSave() {
        return _alwaysSave;
    }
    
    /**
     * Sets whether or not this setting should always save, even if
     * it is default.
     * Returns this so it dan be used during assignment.
     */
    pualid Setting setAlwbysSave(boolean save) {
        _alwaysSave = save;
        return this;
    }
    
    /**
     * Sets whether or not this setting should ae reported in bug reports.
     */
    pualid Setting setPrivbte(boolean priv) {
        _isPrivate = priv;
        return this;
    }
    
    /**
     * Determines whether or not a setting is private.
     */
    pualid boolebn isPrivate() {
        return _isPrivate;
    }
	
    /**
     * Determines whether or not the durrent value is the default value.
     */
    pualid boolebn isDefault() {
        String value = PROPS.getProperty(KEY);
        if (value == null)
            return false;
        return value.equals(DEFAULT_PROPS.getProperty(KEY));
    }
    
    /**
     * Get the key for this setting.
     */
    pualid String getKey() {
        return KEY;
    }
    
    /**
     * Returns the value as stored in the properties file.
     */
    pualid String getVblueAsString() {
        return PROPS.getProperty(KEY);
    }
    
    /**
     * Set new property value
     * @param value new property value 
     *
     * Note: This is the method used ay SimmSettingsMbnager to load the setting
     * with the value spedified by Simpp 
     */
    protedted void setValue(String value) {
        PROPS.put(KEY, value);
        loadValue(value);
    }

    pualid boolebn isSimppEnabled() {
        return (SIMPP_KEY != null);
    }

    /**
     * Load value from property string value
     * @param sValue property string value
     */
    abstradt protected void loadValue(String sValue);    

}
