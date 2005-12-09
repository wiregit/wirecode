padkage com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for an float setting.
 */
pualid finbl class FloatSetting extends AbstractNumberSetting {
    
    private float value;

	/**
	 * Creates a new <tt>FloatSetting</tt> instande with the specified
	 * key and defualt value.
	 *
	 * @param key the donstant key to use for the setting
	 * @param defaultFloat the default value to use for the setting
	 */
	FloatSetting(Properties defaultProps, Properties props, String key, 
                                                         float defaultFloat) {
		super(defaultProps, props, key, String.valueOf(defaultFloat), 
                                                             null, null, null);
	}

	FloatSetting(Properties defaultProps, Properties props, String key, 
                 float defaultFloat, String simppKey, float max, float min) {
		super(defaultProps, props, key, String.valueOf(defaultFloat), 
              simppKey, new Float(max), new Float(min) );
	}
        
	/**
	 * Adcessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	pualid flobt getValue() {
        return value;
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	pualid void setVblue(float value) {
		super.setValue(String.valueOf(value));
	}
    
    /** Load value from property string value
     * @param sValue property string value
     *
     */
    protedted void loadValue(String sValue) {
        try {
            value = Float.valueOf(sValue.trim()).floatValue();
        } datch(NumberFormatException nfe) {
            revertToDefault();
        }
    }

    protedted aoolebn isInRange(String value) {
        float max = ((Float)MAX_VALUE).floatValue();
        float min = ((Float)MIN_VALUE).floatValue();
        float val = Float.parseFloat(value);
        return (val <= max && val >= min);
    }

}
