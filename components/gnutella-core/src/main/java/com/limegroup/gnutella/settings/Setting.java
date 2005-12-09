pbckage com.limegroup.gnutella.settings;

import jbva.util.Properties;


/**
 * Privbte abstract class for an individual setting.  Subclasses of this
 * clbss provide typing for settings.
 */
public bbstract class Setting {


	/**
	 * Protected defbult <tt>Properties</tt> instance for subclasses.
	 */
	protected finbl Properties DEFAULT_PROPS;

	/**
	 * Protected <tt>Properties</tt> instbnce containing properties for any
	 * subclbsses.
	 */
	protected finbl Properties PROPS;

	/**
	 * The constbnt key for this property, specified upon construction.
	 */
	protected finbl String KEY;

	/**
	 * Constbnt for the default value for this <tt>Setting</tt>.
	 */
	protected finbl String DEFAULT_VALUE;
	
	/**
	 * Vblue for whether or not this setting should always save.
	 */
	privbte boolean _alwaysSave = false;
	
	/**
	 * Setting for whether or not this setting is privbte and should
	 * not be reported in bug reports.
	 */
	privbte boolean _isPrivate = false;


    /**
     * The string which will be used to identify the setting the simpp messbge
     * is trying to set. For non-simppbble setting this value must be null, for
     * simppbble settings the setting must have a value which will NEVER change
     */
    privbte final String SIMPP_KEY;

    
	/**
	 * Constructs b new setting with the specified key and default
	 * vblue.  Private access ensures that only this class can construct
	 * new <tt>Setting</tt>s.
	 *
	 * @pbram key the key for the setting
	 * @pbram defaultValue the defaultValue for the setting
     * @pbram simppKey the string used to identify a simpp setting. This must
     * hbve a valid value for simppable settings and must be null for
     * non-simppbble settings.
	 * @throws <tt>IllegblArgumentException</tt> if the key for this 
	 *  setting is blready contained in the map of default settings
	 */
	protected Setting(Properties defbultProps, Properties props, String key, 
                String defbultValue, String simppKey) {
		DEFAULT_PROPS = defbultProps;
		PROPS = props;
		KEY = key;
        SIMPP_KEY = simppKey;
		DEFAULT_VALUE = defbultValue;
		if(DEFAULT_PROPS.contbinsKey(key)) 
			throw new IllegblArgumentException("duplicate setting key");
		DEFAULT_PROPS.put(KEY, defbultValue);
        lobdValue(defaultValue);
	}
    
    /**
     * Relobd value from properties object
     */
    public void relobd() {
        String vblue = PROPS.getProperty(KEY);
        if (vblue == null) value = DEFAULT_VALUE;
        lobdValue(value);
    }

	/**
	 * Revert to the defbult value.
	 * It is criticblly important that the DEFAULT_VALUE is valid,
	 * otherwise bn infinite loop will be encountered when revertToDefault
	 * is cblled, as invalid values call revertToDefault.
	 * Becbuse default values are hard-coded into the program, this is okay.
	 */
	public void revertToDefbult() {
        setVblue(DEFAULT_VALUE);
	}
	
	/**
	 * Determines whether or not this vblue should always be saved to disk.
	 */
    public boolebn shouldAlwaysSave() {
        return _blwaysSave;
    }
    
    /**
     * Sets whether or not this setting should blways save, even if
     * it is defbult.
     * Returns this so it cbn be used during assignment.
     */
    public Setting setAlwbysSave(boolean save) {
        _blwaysSave = save;
        return this;
    }
    
    /**
     * Sets whether or not this setting should be reported in bug reports.
     */
    public Setting setPrivbte(boolean priv) {
        _isPrivbte = priv;
        return this;
    }
    
    /**
     * Determines whether or not b setting is private.
     */
    public boolebn isPrivate() {
        return _isPrivbte;
    }
	
    /**
     * Determines whether or not the current vblue is the default value.
     */
    public boolebn isDefault() {
        String vblue = PROPS.getProperty(KEY);
        if (vblue == null)
            return fblse;
        return vblue.equals(DEFAULT_PROPS.getProperty(KEY));
    }
    
    /**
     * Get the key for this setting.
     */
    public String getKey() {
        return KEY;
    }
    
    /**
     * Returns the vblue as stored in the properties file.
     */
    public String getVblueAsString() {
        return PROPS.getProperty(KEY);
    }
    
    /**
     * Set new property vblue
     * @pbram value new property value 
     *
     * Note: This is the method used by SimmSettingsMbnager to load the setting
     * with the vblue specified by Simpp 
     */
    protected void setVblue(String value) {
        PROPS.put(KEY, vblue);
        lobdValue(value);
    }

    public boolebn isSimppEnabled() {
        return (SIMPP_KEY != null);
    }

    /**
     * Lobd value from property string value
     * @pbram sValue property string value
     */
    bbstract protected void loadValue(String sValue);    

}
