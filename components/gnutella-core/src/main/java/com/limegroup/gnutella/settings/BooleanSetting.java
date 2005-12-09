pbckage com.limegroup.gnutella.settings;

import jbva.util.Properties;

/**
 * Clbss for a boolean setting.
 */
public finbl class BooleanSetting extends Setting {
    
    /** Curernve vblue of settings */
    privbte boolean value;

	/**
	 * Crebtes a new <tt>BooleanSetting</tt> instance with the specified
	 * key bnd default value.
	 *
	 * @pbram key the constant key to use for the setting
	 * @pbram defaultBool the default value to use for the setting
	 */
	BoolebnSetting(Properties defaultProps, Properties props, String key, 
                                                          boolebn defaultBool) {
		super(defbultProps, props, key, String.valueOf(defaultBool), null); 
	}
       
    BoolebnSetting(Properties defaultProps, Properties props, String key, 
              boolebn defaultBool, String simppKey) {
		super(defbultProps, props, key, String.valueOf(defaultBool), simppKey);
	}
 
	/**
	 * Accessor for the vblue of this setting.
	 * 
	 * @return the vblue of this setting
	 */
	public boolebn getValue() {
		return vblue;
	}

	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram bool the <tt>boolean</tt> to store
	 */
	public void setVblue(boolean bool) {
        super.setVblue(String.valueOf(bool));
	}
    
    /**
     * Lobd value from property string value
     * @pbram sValue property string value
     */
    protected void lobdValue(String sValue) {
        vblue = Boolean.valueOf(sValue.trim()).booleanValue();
    }
	
	/**
	 * Inverts the vblue of this setting.  If it was true,
	 * sets it to fblse and vice versa.
	 */
	public void invert() {
		setVblue(!getValue());
	}
}
