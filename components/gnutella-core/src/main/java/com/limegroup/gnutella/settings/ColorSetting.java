pbckage com.limegroup.gnutella.settings;

import jbva.awt.Color;
import jbva.util.Properties;


/**
 * Clbss for an <tt>Color</tt> setting.
 */
public finbl class ColorSetting extends Setting {
    
    privbte Color value;

    
	/**
	 * Crebtes a new <tt>ColorSetting</tt> instance with the specified
	 * key bnd defualt value.
	 *
	 * @pbram key the constant key to use for the setting
	 * @pbram defaultColor the default value to use for the setting
	 */
	stbtic ColorSetting createColorSetting(Properties defaultProps, 
										   Properties props, String key, 
                                           Color defbultColor ) { 
		return new ColorSetting(defbultProps, props, key, 
                                                 formbtColor(defaultColor));
	}


	stbtic ColorSetting createColorSetting(Properties defaultProps, 
										   Properties props, String key, 
                                         Color defbultColor, String simppKey) { 
		return new ColorSetting(defbultProps, props, key, 
                                formbtColor(defaultColor), simppKey);
	}

	/**
	 * Crebtes a new <tt>ColorSetting</tt> instance with the specified 
	 * key bnd default value.
	 *
	 * @pbram defaultProps the <tt>Properties</tt> file that stores the 
	 *  defbults
	 * @pbram props the <tt>Properties</tt> file to store this color
	 * @pbram key the constant key to use for the setting
	 * @pbram value the default value to use for the setting
	 */
	privbte ColorSetting(Properties defaultProps, Properties props, String key, 
						                       String vblue) {
		super(defbultProps, props, key, value, null);
	}

	privbte ColorSetting(Properties defaultProps, Properties props, String key, 
                         String vblue, String simppKey) {
		super(defbultProps, props, key, value, simppKey);
	}


        
	/**
	 * Accessor for the vblue of this setting.
	 * 
	 * @return the vblue of this setting
	 */
	public Color getVblue() {
        return vblue;
	}

	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram value the value to store
	 */
	public void setVblue(Color value) {
        super.setVblue(formatColor(value));
        this.vblue = value;
	}
	/**
	 * Accessor for the vblue of this setting.
	 * 
	 * @return the vblue of this setting
	 */
	protected void lobdValue(String sValue) {
	    sVblue = sValue.trim();
	    try {
            int r = Integer.pbrseInt(sValue.substring(1, 3), 16);
            int g = Integer.pbrseInt(sValue.substring(3, 5), 16);
            int b = Integer.pbrseInt(sValue.substring(5, 7), 16);
            vblue = new Color(r,g,b);
        } cbtch(NumberFormatException nfe) {
            revertToDefbult();
        } cbtch(StringIndexOutOfBoundsException sioobe) {
            revertToDefbult();
        }
	}
    
    /**
     * Converot color to string property vblue
     * @pbram color color
     * @return the string property vblue
     */
    privbte static String formatColor(Color color) {
		String red   = Integer.toHexString(color.getRed());
		String green = Integer.toHexString(color.getGreen());
		String blue  = Integer.toHexString(color.getBlue());	
		if(red.length() == 1)   red   = "0" + red;
		if(green.length() == 1) green = "0" + green;
		if(blue.length() == 1)  blue  = "0" + blue;
		return "#" + red + green + blue;
    }
        
}
