package com.limegroup.gnutella.settings;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import com.limegroup.gnutella.util.FileUtils;

/**
 * Class for an Array of Files setting.
 */
 
public class FileArraySetting extends Setting {
    
    private File[] value;

	/**
	 * Creates a new <tt>FileArraySetting</tt> instance with the specified
	 * key and defualt value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultInt the default value to use for the setting
	 */
	FileArraySetting(Properties defaultProps, Properties props, String key, 
                                                         File[] defaultValue) {
		this(defaultProps, props, key, defaultValue, null);
	}
        
	FileArraySetting(Properties defaultProps, Properties props, String key, 
                     File[] defaultValue, String simppKey) {
		super(defaultProps, props, key, decode(defaultValue), simppKey);
		setPrivate(true);
    }


	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public File[] getValue() {
        return value;
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(File[] value) {
		super.setValue(decode(value));
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param Adds file to the array.
	 */
	public void add(File file) {
        File[] newValue = new File[value.length+1];
		System.arraycopy(value, 0, newValue, 0, value.length);
		newValue[value.length] = file;
		setValue(newValue);
	}
    
	/**
	 * Returns true if the given file is contained in this array.
	 */
	public boolean contains(File file) {
	    if(file == null) return false;
        List list = Arrays.asList(value);
        for(Iterator it = list.iterator(); it.hasNext(); ) {
            try {
                if(file.equals(FileUtils.getCanonicalFile((File)it.next()))) {
                    return true;
                }
            } catch(IOException ioe) {
                continue;
            }
        }
	    return false;
	}
	
    /** Load value from property string value
     * @param sValue property string value
     *
     */
    protected void loadValue(String sValue) {
		value = encode(sValue);
    }
    
    /**
     * Splits the string into an Array
     */
    private static final File[] encode(String src) {
        
        if (src == null || src.length()==0) {
            return (new File[0]);
        }
        
        StringTokenizer tokenizer = new StringTokenizer(src, ";");
        File[] dirs = new File[tokenizer.countTokens()];
        for(int i = 0; i < dirs.length; i++) {
            dirs[i] = new File(tokenizer.nextToken());
        }
        
        return dirs;
    }
    
    /**
     * Separates each field of the array by a semicolon
     */
    private static final String decode(File[] src) {
        
        if (src == null || src.length==0) {
            return "";
        }
        
        StringBuffer buffer = new StringBuffer();
        
        for(int i = 0; i < src.length; i++) {
            buffer.append(src[i].getAbsolutePath());
            if (i < src.length-1) { buffer.append(';'); }
        }
            
        return buffer.toString();
    }

}
