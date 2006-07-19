package com.limegroup.gnutella.settings;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * A setting which has a Set of files.
 */
 
public class FileSetSetting extends Setting {
    
    private Set<File> value;

	/**
	 * Creates a new <tt>FileSetSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the constant key to use for the setting
	 * @param defaultInt the default value to use for the setting
	 */
	FileSetSetting(Properties defaultProps, Properties props, String key, File[] defaultValue) {
		this(defaultProps, props, key, defaultValue, null);
	}
        
	FileSetSetting(Properties defaultProps, Properties props, String key, 
                     File[] defaultValue, String simppKey) {
		super(defaultProps, props, key, decode(new HashSet<File>(Arrays.asList(defaultValue))), simppKey);
		setPrivate(true);
    }


	/**
	 * Accessor for the value of this setting.
	 * 
	 * @return the value of this setting
	 */
	public Set<File> getValue() {
        return value;
	}
	
	/**
	 * Gets the value as an array.
	 */
	public synchronized File[] getValueAsArray() {
	    return value.toArray(new File[value.size()]);
    }

	/**
	 * Mutator for this setting.
	 *
	 * @param value the value to store
	 */
	public void setValue(Set<? extends File> value) {
		super.setValue(decode(value));
	}

	/**
	 * Mutator for this setting.
	 *
	 * @param Adds file to the array.
	 */
	public synchronized void add(File file) {
	    value.add(file);
	    setValue(value);
	}
    
	/**
	 * Mutator for this setting.
	 *
	 * @param Remove file from the array, if it exists.
	 * @return false when the array does not contain the file or when the
	 * file is <code>null</code> 
	 */
	public synchronized boolean remove(File file) {
	    if(value.remove(file)) {
	        setValue(value);
	        return true;
	    } else {
	        return false;
	    }
	}
    
	/**
	 * Returns true if the given file is contained in this array.
	 */
	public synchronized boolean contains(File file) {
	    return value.contains(file);
	}
	
	/**
	 * Returns the length of the array.
	 */
	public synchronized int length() {
	    return value.size();
	}
	
    /** Load value from property string value
     * @param sValue property string value
     *
     */
    protected synchronized void loadValue(String sValue) {
		value = encode(sValue);
    }
    
    /**
     * Splits the string into a Set
     */
    private static final Set<File> encode(String src) {
        if (src == null || src.length()==0)
            return new HashSet<File>();
        
        StringTokenizer tokenizer = new StringTokenizer(src, ";");
        int size = tokenizer.countTokens();
        Set<File> set = new HashSet<File>();
        for(int i = 0; i < size; i++)
            set.add(new File(tokenizer.nextToken()));
        return set;
    }
    
    /**
     * Separates each field of the array by a semicolon
     */
    private static final String decode(Set<? extends File> src) {
        if (src == null || src.isEmpty())
            return "";
        
        StringBuffer buffer = new StringBuffer();
        for(Iterator<? extends File> i = src.iterator(); i.hasNext(); ) {
            buffer.append(i.next().getAbsolutePath());
            if (i.hasNext())
                buffer.append(';');
        }
            
        return buffer.toString();
    }

	/**
	 * Removes non-existent members.
	 */
	public synchronized void clean() {
	    for(Iterator<File> i = value.iterator(); i.hasNext(); ) {
	        File next = i.next();
	        if(!next.exists())
	            i.remove();
        }
        
	    setValue(value);
    }
}