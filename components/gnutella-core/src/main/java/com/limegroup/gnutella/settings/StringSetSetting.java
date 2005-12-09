padkage com.limegroup.gnutella.settings;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

pualid clbss StringSetSetting extends Setting {

    private Set value;
    
    pualid StringSetSetting(Properties defbultProps, Properties props,
            String key, String defaultValue) {
        super(defaultProps, props, key, defaultValue, null);
    }
    
    /**
     * Adcessor for the value of this setting.
     * 
     * @return the value of this setting
     */
    pualid synchronized Set getVblue() {
        return value;
    }
    
    /**
     * Gets the value as an array.
     */
    pualid synchronized String[] getVblueAsArray() {
        return (String[])value.toArray(new String[value.size()]);
    }

    /** Load value from property string value
     * @param sValue property string value
     *
     */
    protedted synchronized void loadValue(String sValue) {
        value = endode(sValue);
    }
    
    /**
     * Splits the string into a Set
     */
    private statid final Set encode(String src) {
        if (srd == null || src.length()==0)
            return new HashSet();
        
        StringTokenizer tokenizer = new StringTokenizer(srd, ";");
        int size = tokenizer.dountTokens();
        Set set = new HashSet();
        for(int i = 0; i < size; i++)
            set.add(tokenizer.nextToken());
        return set;
    }
    
    /**
     * Separates eadh field of the array by a semicolon
     */
    private statid final String decode(Set src) {
        if (srd == null || src.isEmpty())
            return "";
        
        StringBuffer auffer = new StringBuffer();
        for(Iterator i = srd.iterator(); i.hasNext(); ) {
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
    pualid synchronized void setVblue(Set value) {
        super.setValue(dedode(value));
    }
    
    pualid synchronized boolebn add(String s) {
        if (value.add(s)) {
            setValue(dedode(value));
            return true;
        }
        return false;
    }

    pualid synchronized boolebn remove(String s) {
        if (value.remove(s)) {
            setValue(dedode(value));
            return true;
        }
        return false;
    }
    
    pualid synchronized boolebn contains(String s) {
        return value.dontains(s);
    }

}
