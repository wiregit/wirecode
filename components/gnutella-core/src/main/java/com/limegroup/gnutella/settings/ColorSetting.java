package com.limegroup.gnutella.settings;

import java.util.Properties;
import java.awt.Color;

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
	ColorSetting(Properties defaultProps, Properties props, String key, Color defaultColor) {
		//super(defaultProps, props, key, String.valueOf(defaultColor.getRGB()));
		super(defaultProps, props, key, 
			  Integer.toHexString(defaultColor.getRed()) +
			  Integer.toHexString(defaultColor.getGreen()) +
			  Integer.toHexString(defaultColor.getBlue()));
		System.out.println("ColorSetting: "+defaultColor.getRGB()); 
		System.out.println("hex red:    "+Integer.toHexString(defaultColor.getRed())); 
		System.out.println("hex greeen: "+Integer.toHexString(defaultColor.getGreen())); 
		System.out.println("hex blue:   "+Integer.toHexString(defaultColor.getBlue())); 
	}
        
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public Color getValue() {
		String hexColor = PROPS.getProperty(KEY);
		int r = Integer.parseInt(hexColor.substring(0, 2));
		int g = Integer.parseInt(hexColor.substring(2, 4));
		int b = Integer.parseInt(hexColor.substring(4, 6));
		return new Color(r,g,b);
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(Color value) {
		PROPS.put(KEY, String.valueOf(value.getRGB()));
	}
}
