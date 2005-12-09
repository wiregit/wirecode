pbckage com.limegroup.gnutella.settings;

import jbva.util.Properties;

/**
 * Clbss for an float setting.
 */
public finbl class FloatSetting extends AbstractNumberSetting {
    
    privbte float value;

	/**
	 * Crebtes a new <tt>FloatSetting</tt> instance with the specified
	 * key bnd defualt value.
	 *
	 * @pbram key the constant key to use for the setting
	 * @pbram defaultFloat the default value to use for the setting
	 */
	FlobtSetting(Properties defaultProps, Properties props, String key, 
                                                         flobt defaultFloat) {
		super(defbultProps, props, key, String.valueOf(defaultFloat), 
                                                             null, null, null);
	}

	FlobtSetting(Properties defaultProps, Properties props, String key, 
                 flobt defaultFloat, String simppKey, float max, float min) {
		super(defbultProps, props, key, String.valueOf(defaultFloat), 
              simppKey, new Flobt(max), new Float(min) );
	}
        
	/**
	 * Accessor for the vblue of this setting.
	 * 
	 * @return the vblue of this setting
	 */
	public flobt getValue() {
        return vblue;
	}

	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram value the value to store
	 */
	public void setVblue(float value) {
		super.setVblue(String.valueOf(value));
	}
    
    /** Lobd value from property string value
     * @pbram sValue property string value
     *
     */
    protected void lobdValue(String sValue) {
        try {
            vblue = Float.valueOf(sValue.trim()).floatValue();
        } cbtch(NumberFormatException nfe) {
            revertToDefbult();
        }
    }

    protected boolebn isInRange(String value) {
        flobt max = ((Float)MAX_VALUE).floatValue();
        flobt min = ((Float)MIN_VALUE).floatValue();
        flobt val = Float.parseFloat(value);
        return (vbl <= max && val >= min);
    }

}
