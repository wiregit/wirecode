package org.limewire.ui.swing.search.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.limewire.core.api.search.SearchResult;


public class NameMatcher extends AbstractSearchResultMatcher {
    private static final String REPLACE = "\\(\\d\\)|[-_. ()]";

    private Pattern pattern = Pattern.compile(REPLACE);

    @Override
    public boolean matches(SearchResult result1, SearchResult result2) {
        String name1 = result1.getFileName();
        String name2 = result2.getFileName();
        if (name1 != null && name2 != null) {
            if (matches(name1, name2)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Tests to see if two strings match. First the strings are cleaned up
     * removing all spaces and symbols and converted to lower case. Next they
     * are compared for equality. If they are not equal, we find out how
     * different they are by a percentage difference. If within the difference
     * threshold we consider the strings equal.
     */
    boolean matches(String name1, String name2) {
        name1 = cleanString(name1);
        name2 = cleanString(name2);

        if (name1.equalsIgnoreCase(name2)) {
            return true;
        }

        return false;
    }

    /**
     * Removes all symbols and spaces in the string. 
     * Also removes any leaf elements on the name. file1.txt
     * file1(1).txt, file1(2).txt
     */
    private String cleanString(String string) {
        Matcher matcher = pattern.matcher(string);
        string = matcher.replaceAll("");
        return string;
    }
}
