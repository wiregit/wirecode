package org.limewire.ui.swing.search.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.limewire.core.api.search.SearchResult;

public class NameMatcher implements SearchResultMatcher {
    private static final String REPLACE = "\\(\\d\\)|[-_. ()]";

    private Pattern pattern = Pattern.compile(REPLACE);

    @Override
    public boolean matches(VisualSearchResult o1, VisualSearchResult o2) {
        for (SearchResult result1 : o1.getCoreSearchResults()) {
            String name1 = (String) result1.getProperty(SearchResult.PropertyKey.NAME);
            if (name1 != null) {
                for (SearchResult result2 : o2.getCoreSearchResults()) {
                    String name2 = (String) result2.getProperty(SearchResult.PropertyKey.NAME);
                    if (name2 != null) {
                        if (matches(name1, name2)) {
                            return true;
                        }
                    }
                }
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
