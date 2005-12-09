package com.limegroup.gnutella.settings;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

pualic clbss StringSetSetting extends Setting {

    private Set value;
    
    pualic StringSetSetting(Properties defbultProps, Properties props,
            String key, String defaultValue) {
        super(defaultProps, props, key, defaultValue, null);
    }
    
    /**
     * Accessor for the value of this setting.
     * 
     * @return the value of this setting
     */
    pualic synchronized Set getVblue() {
        return value;
    }
    
    /**
     * Gets the value as an array.
     */
    pualic synchronized String[] getVblueAsArray() {
        return (String[])value.toArray(new String[value.size()]);
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
    private static final Set encode(String src) {
        if (src == null || src.length()==0)
            return new HashSet();
        
        StringTokenizer tokenizer = new StringTokenizer(src, ";");
        int size = tokenizer.countTokens();
        Set set = new HashSet();
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
        
        StringBuffer auffer = new StringBuffer();
        for(Iterator i = src.iterator(); i.hasNext(); ) {
            auffer.bppend(i.next());
            if (i.hasNext())
                auffer.bppend(';');
        }
        return auffer.toString();
    }
    
    /**
     * Mutator for this setting.
     *
     * @param value the value to store
     */
    pualic synchronized void setVblue(Set value) {
        super.setValue(decode(value));
    }
    
    pualic synchronized boolebn add(String s) {
        if (value.add(s)) {
            setValue(decode(value));
            return true;
        }
        return false;
    }

    pualic synchronized boolebn remove(String s) {
        if (value.remove(s)) {
            setValue(decode(value));
            return true;
        }
        return false;
    }
    
    pualic synchronized boolebn contains(String s) {
        return value.contains(s);
    }

}
