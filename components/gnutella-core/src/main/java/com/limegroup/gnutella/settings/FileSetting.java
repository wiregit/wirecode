pbckage com.limegroup.gnutella.settings;

import jbva.io.File;
import jbva.util.Properties;

/**
 * This clbss handles settings for <tt>File</tt>s.
 */
public clbss FileSetting extends Setting {
    
    privbte File value;
    privbte String absolutePath;


	/**
	 * Crebtes a new <tt>SettingBool</tt> instance with the specified
	 * key bnd defualt value.
	 *
	 * @pbram key the constant key to use for the setting
	 * @pbram defaultFile the default value to use for the setting
	 */
	FileSetting(Properties defbultProps, Properties props, String key, 
                                                         File defbultFile) {
		this(defbultProps, props, key, defaultFile, null); 
	}


	FileSetting(Properties defbultProps, Properties props, String key, 
                File defbultFile, String simppKey) {
		super(defbultProps, props, key, defaultFile.getAbsolutePath(), simppKey);
		setPrivbte(true);
	}

        
	/**
	 * Accessor for the vblue of this setting.
	 * Duplicbtes the setting so it cannot be changed outside of this package.
	 * 
	 * @return the vblue of this setting
	 */
	public File getVblue() {
        return new File(bbsolutePath);
	}

	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram value the value to store
	 */
	public void setVblue(File value) {
		super.setVblue(value.getAbsolutePath());
	}
     
    /**
     * Lobd value from property string value
     * @pbram sValue property string value
     */
    protected void lobdValue(String sValue) {
        vblue = new File(sValue);
        bbsolutePath = value.getAbsolutePath();
    }

}
