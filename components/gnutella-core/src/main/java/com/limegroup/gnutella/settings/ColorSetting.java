package com.limegroup.gnutella.settings;

import java.util.Properties;
import java.awt.Color;
import java.text.NumberFormat;


/**
 * Class for an <tt>Color</tt> setting.
 */
public final class ColorSetting extends Setting {

	/**
	 * Creates a new <tt>ColorSetting</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultColor the default value to use for the setting
	 */
	static ColorSetting createColorSetting(Properties defaultProps, Properties props, 
										   String key, Color defaultColor) {	  
		String red   = Integer.toHexString(defaultColor.getRed());
		String green = Integer.toHexString(defaultColor.getGreen());
		String blue  = Integer.toHexString(defaultColor.getBlue());	
		if(red.length() == 1)   red   = "0" + red;
		if(green.length() == 1) green = "0" + green;
		if(blue.length() == 1)  blue  = "0" + blue;
		return new ColorSetting(defaultProps, props, key, red+green+blue);
	}

	
	private ColorSetting(Properties defaultProps, Properties props, 
						 String key, String value) {
		super(defaultProps, props, key, value);
	}

        
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public Color getValue() {
		String hexColor = PROPS.getProperty(KEY);
		int r = Integer.parseInt(hexColor.substring(0, 2), 16);
		int g = Integer.parseInt(hexColor.substring(2, 4), 16);
		int b = Integer.parseInt(hexColor.substring(4, 6), 16);
		return new Color(r,g,b);
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(Color value) {
		String red   = Integer.toHexString(value.getRed());
		String green = Integer.toHexString(value.getGreen());
		String blue  = Integer.toHexString(value.getBlue());	
		if(red.length() == 1)   red   = "0" + red;
		if(green.length() == 1) green = "0" + green;
		if(blue.length() == 1)  blue  = "0" + blue;
		PROPS.put(KEY, red+green+blue);
	}
}
