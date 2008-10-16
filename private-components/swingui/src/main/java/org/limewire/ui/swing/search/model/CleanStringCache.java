package org.limewire.ui.swing.search.model;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CleanStringCache {
    private static final String DEFAULT_REPLACE = "\\(\\d\\)|[-_.' ()]";

    private final Pattern pattern;
    
    private final Map<String, String> cache;
    
    public CleanStringCache() {
        this.pattern = Pattern.compile(DEFAULT_REPLACE);
        this.cache = new WeakHashMap<String, String>();
    }

    public String getCleanString(String name) {
        String cleanedString = cache.get(name);
        if(cleanedString == null) {
            cleanedString = cleanString(name);
            cache.put(name, cleanedString);
        }
        return cleanedString;
    }
    
    /**
     * Removes all symbols and spaces in the string. 
     * Also removes any leaf elements on the name. file1.txt
     * file1(1).txt, file1(2).txt
     */
    public String cleanString(String string) {
        Matcher matcher = pattern.matcher(string);
        string = matcher.replaceAll("");
        return string;
    }

    public boolean matches(String string1, String string2) {
       return getCleanString(string1).equalsIgnoreCase(getCleanString(string2));
    }
}
