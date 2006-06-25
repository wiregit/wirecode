package com.limegroup.gnutella.settings;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

public class StringSetSetting extends Setting {

    private Set<String> value;
    
    public StringSetSetting(Properties defaultProps, Properties props,
            String key, String defaultValue) {
        super(defaultProps, props, key, defaultValue, null);
    }
    
    /**
     * Accessor for the value of this setting.
     * 
     * @return the value of this setting
     */
    public synchronized Set<String> getValue() {
        return value;
    }
    
    /**
     * Gets the value as an array.
     */
    public synchronized String[] getValueAsArray() {
        return value.toArray(new String[value.size()]);
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
    private static final Set<String> encode(String src) {
        if (src == null || src.length()==0)
            return new HashSet<String>();
        
        StringTokenizer tokenizer = new StringTokenizer(src, ";");
        int size = tokenizer.countTokens();
        Set<String> set = new HashSet<String>();
        for(int i = 0; i < size; i++)
            set.add(tokenizer.nextToken());
        return set;
    }
    
    /**
     * Separates each field of the array by a semicolon
     */
    private static final String decode(Set src) {
        if (src == null || src.isEmpty())
            return "";
        
        StringBuffer buffer = new StringBuffer();
        for(Iterator i = src.iterator(); i.hasNext(); ) {
            buffer.append(i.next());
            if (i.hasNext())
                buffer.append(';');
        }
        return buffer.toString();
    }
    
    /**
     * Mutator for this setting.
     *
     * @param value the value to store
     */
    public synchronized void setValue(Set<String> value) {
        super.setValue(decode(value));
    }
    
    public synchronized boolean add(String s) {
        if (value.add(s)) {
            setValue(decode(value));
            return true;
        }
        return false;
    }

    public synchronized boolean remove(String s) {
        if (value.remove(s)) {
            setValue(decode(value));
            return true;
        }
        return false;
    }
    
    public synchronized boolean contains(String s) {
        return value.contains(s);
    }

}
