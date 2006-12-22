package org.limewire.setting;

import java.io.File;
import java.util.Properties;


/**
 * This class handles settings for <tt>File</tt>s.
 */
public class FileSetting extends Setting {
    
    private File value;
    private String absolutePath;


	/**
	 * Creates a new <tt>SettingBool</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultFile the default value to use for the setting
	 */
	FileSetting(Properties defaultProps, Properties props, String key, 
                                                         File defaultFile) {
        super(defaultProps, props, key, defaultFile.getAbsolutePath());
        setPrivate(true);
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
    
    public FileSetting setAlwaysSave(boolean on) {
        super.setAlwaysSave(on);
        return this;
    }

}
