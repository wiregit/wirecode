package com.limegroup.gnutella.settings;

import java.util.Properties;

import com.limegroup.gnutella.util.StringUtils;
/**
 * Class for a notification filter setting
 *
 */
public class NotificationFilterSetting extends Setting {

    private String[][] value;

	/**
	 * Creates a new <tt>NotificationFilterSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultInt the default value to use for the setting
	 */
    NotificationFilterSetting(Properties defaultProps, Properties props, String key, 
                                                       String[][] defaultValue) {
		super(defaultProps, props, key, decode(defaultValue), null);
	}

    NotificationFilterSetting(Properties defaultProps, Properties props, String key, 
                       String[][] defaultValue, String simppKey) {
		super(defaultProps, props, key, decode(defaultValue), simppKey);
	}


        
	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public String[][] getValue() {
        return value;
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(String[][] value) {
		super.setValue(decode(value));
	}
    
    /** Load value from property string value
     * @param sValue property string value
     *
     */
    protected void loadValue(String sValue) {
		value = encode(sValue);
    }
    
    /**
     * Splits the string into an Array of an Array of String
     */
    private static final String[][] encode(String src) {
        
        if (src == null || src.length()==0) {
            return (new String[0][0]);
        }
        String[] filters= StringUtils.split(src, "|");
        String[][] res = new String[filters.length][];
        for (int i = 0; i < filters.length; i++) {
        	String[] str = StringUtils.split(filters[i], ";");
//        	res[i] = new String[str.length];
        	res[i] = str;
		} 
        return res;
    }
    
    /**
     * Separates each field of the internal array by a semicolon
     * and the outer array by a pipe
     */
    private static final String decode(String[][] src) {
        
        if (src == null || src.length==0) {
            return "";
        }
        
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < src.length; i++) {
        	for(int j = 0; j < src[i].length; j++) {
                buffer.append(src[i][j]);
                if (j < src[i].length-1) { buffer.append(';'); }
            }
        	if (i < src.length-1)buffer.append('|');
		}
            
        return buffer.toString();
    }

}
