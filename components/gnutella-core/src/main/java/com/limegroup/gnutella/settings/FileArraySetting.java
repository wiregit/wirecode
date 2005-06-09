package com.limegroup.gnutella.settings;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
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
	 * key and default value.
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
	public synchronized void setValue(File[] value) {
		super.setValue(decode(value));
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param Adds file to the array.
	 */
	public synchronized void add(File file) {
	    if (file == null)
	        return;
	    
        File[] newValue = new File[value.length+1];
		System.arraycopy(value, 0, newValue, 0, value.length);
		newValue[value.length] = file;
		setValue(newValue);
	}
    
	/**
	 * Mutator for this setting.
	 *
	 * @param Remove file from the array, if it exists.
	 * @return false when the array does not contain the file or when the
	 * file is <code>null</code> 
	 */
	public synchronized boolean remove(File file) {
	    if (file == null)
	        return false;
	    
		int index = indexOf(file);
		if (index == -1) {
			return false;
		}
	    
        File[] newValue = new File[value.length-1];
        
        //  copy first half, up to first occurrence's index
        System.arraycopy(value, 0, newValue, 0, index);
        //  copy second half, for the length of the rest of the array
		System.arraycopy(value, index+1, newValue, index, value.length - index - 1);
		
		setValue(newValue);
		return true;
	}
    
	/**
	 * Returns true if the given file is contained in this array.
	 */
	public synchronized boolean contains(File file) {
	    return indexOf(file) >= 0;
	}
	
	/**
	 * Returns the index of the given file in this array, -1 if file is not found.
	 */
	public synchronized int indexOf(File file) {
	    if (file == null)
	        return -1;
	    
        List list = Arrays.asList(value);
        Iterator it = list.iterator();
        for (int i = 0; it.hasNext(); i++) {
            try {
                if ((FileUtils.getCanonicalFile((File)it.next())).equals(FileUtils.getCanonicalFile(file)))
                    return i;
            } catch(IOException ioe) {
                continue;
            }
        }

	    return -1;
	}
	
	/**
	 * Returns the length of the array.
	 */
	public synchronized int length() {
	    return value.length;
	}
	
    /** Load value from property string value
     * @param sValue property string value
     *
     */
    protected synchronized void loadValue(String sValue) {
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
            if (i < src.length-1)
                buffer.append(';');
        }
            
        return buffer.toString();
    }

	/**
	 * Removes non-existent members from this.
	 */
	public synchronized void clean() {
		List list = new LinkedList();
		File file = null;
		for (int i = 0; i < value.length; i++) {
			file = value[i];
			if (file == null)
				continue;
			if (!file.exists())
				continue;
			list.add(file);
		}
		setValue((File[])list.toArray(new File[0]));
	}
}
