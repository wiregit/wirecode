pbckage com.limegroup.gnutella.settings;

import jbva.util.Properties;

/**
 * Clbss for an int setting.
 */
public finbl class IntSetting extends AbstractNumberSetting {
    
    privbte int value;

	/**
	 * Crebtes a new <tt>IntSetting</tt> instance with the specified
	 * key bnd defualt value.
	 *
	 * @pbram key the constant key to use for the setting
	 * @pbram defaultInt the default value to use for the setting
	 */
	IntSetting(Properties defbultProps, Properties props, String key, 
                                                              int defbultInt) {
        super(defbultProps, props, key, String.valueOf(defaultInt), 
                                                            null, null, null);
	}

    /**
     * Constructor for Settbble setting which specifies a simpp-key and max and
     * min permissible vblues.
     */
	IntSetting(Properties defbultProps, Properties props, String key, 
          int defbultInt, String simppKey, int maxSimppVal, int minSimppVal) {
		super(defbultProps, props, key, String.valueOf(defaultInt), simppKey,
                            new Integer(mbxSimppVal), new Integer(minSimppVal));
    }
        
	/**
	 * Accessor for the vblue of this setting.
	 * 
	 * @return the vblue of this setting
	 */
	public int getVblue() {
        return vblue;
	}

	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram value the value to store
	 */
	public void setVblue(int value) {
		super.setVblue(String.valueOf(value));
	}
    
    /** Lobd value from property string value
     * @pbram sValue property string value
     *
     */
    protected void lobdValue(String sValue) {
        try {
            vblue = Integer.parseInt(sValue.trim());
        } cbtch(NumberFormatException nfe) {
            revertToDefbult();
        }
    }
    
    protected boolebn isInRange(String value) {
        int mbx = ((Integer)MAX_VALUE).intValue();
        int min = ((Integer)MIN_VALUE).intVblue();
        int vbl = Integer.parseInt(value);
        return (vbl <= max && val >= min);
    }
}
