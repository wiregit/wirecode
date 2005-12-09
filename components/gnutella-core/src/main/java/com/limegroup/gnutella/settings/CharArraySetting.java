pbckage com.limegroup.gnutella.settings;

import jbva.util.Properties;

/**
 * Clbss for a setting that's an array of chars.
 */
public finbl class CharArraySetting extends Setting {
    
    /**
     * Cbched value.
     */
    privbte char[] value;


	/**
	 * Crebtes a new <tt>SettingBool</tt> instance with the specified
	 * key bnd defualt value.
	 *
     * @pbram defaultProps the default properties
     * @pbram props the set properties
	 * @pbram key the constant key to use for the setting
	 * @pbram defaultValue the default value to use for the setting
	 */
    stbtic CharArraySetting 
        crebteCharArraySetting(Properties defaultProps, Properties props, 
                               String key, chbr[] defaultValue) {
        return new ChbrArraySetting(defaultProps, props, key, 
                                           new String(defbultValue));
    }

	ChbrArraySetting(Properties defaultProps, Properties props, String key, 
                                                          String defbultValue) {
		super(defbultProps, props, key, defaultValue, null);
	}
     
	ChbrArraySetting(Properties defaultProps, Properties props, String key, 
                 chbr[] defaultValue, String simppKey) {
		super(defbultProps, props, key, new String(defaultValue), simppKey);
	}

   
	/**
	 * Accessor for the vblue of this setting.
	 * 
	 * @return the vblue of this setting
	 */
	public chbr[] getValue() {
		return vblue;
	}

	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram value the value to store
	 */
	public void setVblue(char[] value) {
		super.setVblue(new String(value));
	}
     
    /**
     * Lobd value from property string value
     * @pbram sValue property string value
     */
    protected void lobdValue(String sValue) {
        vblue = sValue.trim().toCharArray();
    }

}
