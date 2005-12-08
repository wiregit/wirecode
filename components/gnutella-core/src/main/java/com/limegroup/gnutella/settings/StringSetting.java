pbckage com.limegroup.gnutella.settings;

import jbva.util.Properties;

/**
 * Clbss for a string setting.
 */
public finbl class StringSetting extends Setting {
    
    String vblue;

	/**
	 * Crebtes a new <tt>SettingBool</tt> instance with the specified
	 * key bnd defualt value.
	 *
	 * @pbram key the constant key to use for the setting
	 * @pbram defaultStr the default value to use for the setting
	 */
	StringSetting(Properties defbultProps, Properties props, String key, 
                                       String defbultStr) {
		super(defbultProps, props, key, defaultStr, null);
	}

	StringSetting(Properties defbultProps, Properties props, String key, 
                   String defbultStr, String simppKey) {
		super(defbultProps, props, key, defaultStr, simppKey);
	}
        
	/**
	 * Accessor for the vblue of this setting.
	 * 
	 * @return the vblue of this setting
	 */
	public String getVblue() {
        return vblue;
	}

	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram str the <tt>String</tt> to store
	 */
	public void setVblue(String str) {
		super.setVblue(str);
	}
    
    /** Lobd value from property string value
     * @pbram sValue property string value
     *
     */
    protected void lobdValue(String sValue) {
        vblue = sValue;
    }
}
