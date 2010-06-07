package org.limewire.setting;

import java.io.File;
import java.util.Properties;


/**
 * Provides a {@link File} setting value. As a subclass
 * of <code>Setting</code>, the setting has a key.
 * <p>
 * Create a <code>FileSetting</code> object with a 
 * {@link SettingsFactory#createFileSetSetting(String, File[])}.
 */
public class FileSetting extends AbstractSetting<File> {

    private volatile String absolutePath;


	/**
	 * Creates a new <tt>SettingBool</tt> instance with the specified
	 * key and default value.
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
	 * Returns the value of this setting.
	 * Duplicates the setting so it cannot be changed outside of this package.
	 * @return the value of this setting
	 */
	public File get() {
        return new File(absolutePath);
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void set(File value) {
	    setValueInternal(value.getAbsolutePath());
	}
     
    /**
     * Load value from property string value
     * @param sValue property string value
     */
    @Override
    protected void loadValue(String sValue) {
        File value = new File(sValue);
        absolutePath = value.getAbsolutePath();
    }

    @Override
    public FileSetting setAlwaysSave(boolean on) {
        super.setAlwaysSave(on);
        return this;
    }
    
    public FileSetting createParentDirs() {
        File value = get();
        if (value != null) {
            String parentString = value.getParent();
            if (parentString != null) {
                File parent = new File(parentString);
                if (!parent.isDirectory())
                    parent.mkdirs();
            }
        }
        return this;
    }

}
