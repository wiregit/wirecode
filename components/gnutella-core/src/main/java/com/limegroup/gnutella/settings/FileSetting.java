package com.limegroup.gnutella.settings;

import java.util.Properties;
import java.io.*;

/**
 * This class handles settings for <tt>File</tt>s.
 */
public final class FileSetting extends Setting {
    
    private File value;
    private String absolutePath;

    public static final File DUMMY_FILE = new File(".");

	/**
	 * Creates a new <tt>SettingBool</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultFile the default value to use for the setting
	 */
	FileSetting(Properties defaultProps, Properties props, String key, 
                                                         File defaultFile) {
		super(defaultProps, props, key, defaultFile.getAbsolutePath(), 
                                                            null, null, null);
	}


	FileSetting(Properties defaultProps, Properties props, String key, 
                File defaultFile, String simppKey, File max, File min) {
		super(defaultProps, props, key, defaultFile.getAbsolutePath(), 
                                                          simppKey, max, min);
        if(max != DUMMY_FILE || min != DUMMY_FILE)
            throw new IllegalArgumentException("FileSetting bad max or min");
	}

        
	/**
	 * Accessor for the value of this setting.
	 * Duplicates the setting so it cannot be changed outside of this package.
	 * 
	 * @return the value of this setting
	 */
	public File getValue() {
        return new File(absolutePath);
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(File value) {
		super.setValue(value.getAbsolutePath());
	}
     
    /**
     * Load value from property string value
     * @param sValue property string value
     */
    protected void loadValue(String sValue) {
        value = new File(sValue);
        absolutePath = value.getAbsolutePath();
    }

    protected boolean isInRange(String value) {
        //max and min make no sense for Files, just return true
        return true;
    }

}
