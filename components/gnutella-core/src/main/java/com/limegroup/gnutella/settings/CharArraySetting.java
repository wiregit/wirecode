package com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for a setting that's an array of chars.
 */
public final class CharArraySetting extends Setting {
    
    /**
     * Cached value.
     */
    private char[] value;

    /**
     * Constant for the separator char to use.
     */
    private static final String SEP = ";";


	/**
	 * Creates a new <tt>SettingBool</tt> instance with the specified
	 * key and defualt value.
	 *
     * @param defaultProps the default properties
     * @param props the set properties
	 * @param key the constant key to use for the setting
	 * @param defaultValue the default value to use for the setting
	 */
    static CharArraySetting 
        createCharArraySetting(Properties defaultProps, Properties props, 
                               String key, 
                               char[] defaultValue) {

//         StringBuffer sb = new StringBuffer();
//         for(int i=0; i<defaultValue.length; i++) {
//             sb.append(defaultValue[i]);
//             sb.append(';');
//         }
        return new CharArraySetting(defaultProps, props, key, 
                                    new String(defaultValue));
    }

	CharArraySetting(Properties defaultProps, Properties props, String key, 
                     String defaultValue) {
		super(defaultProps, props, key, defaultValue);
	}

    /**
     * Formats the character array as a string.
     *
     * @param chars the characters to convert to a string
     */
//     private static String formatCharArray(char[] chars) {
//         StringBuffer sb = new StringBuffer();
//         for(int i=0; i<chars.length; i++) {
//             sb.append(chars[i]);
//             if(i != chars.length-1)
//                 sb.append(SEP);
//         }
//     }
        
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public char[] getValue() {
		return value;
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(char[] value) {
		super.setValue(new String(value));
	}
     
    /**
     * Load value from property string value
     * @param sValue property string value
     */
    protected void loadValue(String sValue) {
//         StringTokenizer st = new StringTokenizer(SEP);
//         char[] chars = new char[st.countTokens()];
//         int i = 0;
//         while(st.hasMoreTokens()) {
//             chars[i] = st.nextToken().getBytes("UTF-8")[0];
//             i++;
//         }

        value = sValue.toCharArray();
        // don't worry about 
//         byte[] bytes = sValue.getBytes(Locale.US);
//         char[] chars = new char[bytes.length];
//         System.arraycopy(bytes, 0, chars, 0, chars.length);
//         value = chars;
    }
}
