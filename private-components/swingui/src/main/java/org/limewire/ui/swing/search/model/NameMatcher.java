package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.SearchResult;

public class NameMatcher implements SearchResultMatcher {

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
    private boolean matches(String name1, String name2) {
        name1 = cleanString(name1);
        name2 = cleanString(name2);

        if (name1.equalsIgnoreCase(name2)) {
            return true;
        }
//         else if (getPercentageDifferance(name1, name2) < getDifferanceTolerance(name1, name2)) {
//            return true;
//        }
        return false;
    }

    /**
     * Returns a tolerance for differances between the strings based upon the
     * length of the smaller string.
     */
//    private double getDifferanceTolerance(String string1, String string2) {
//        double tolerance = 0;
//        int minStringLength = Math.min(string1.length(), string2.length());
//
//        if (minStringLength <= 5) {
//            tolerance = .20;
//        } else if (minStringLength <= 20) {
//            tolerance = .10;
//        } else if (tolerance <= 100) {
//            tolerance = .05;
//        } else {
//            tolerance = .01;
//        }
//
//        return tolerance;
//    }

    /**
     * Calculates the percentage differance between two strings as a function of
     * the edit distance over the length of the smaller string.
     */
//    private double getPercentageDifferance(String string1, String string2) {
//        int editDistance = getLevenshteinDistance(string1, string2);
//        int minStringLength = Math.min(string1.length(), string2.length());
//        return editDistance / (double) minStringLength;
//    }

    /**
     * This is the same implentation as the commons lang library.
     * 
     * http://www.merriampark.com/ldjava.htm
     * 
     * TODO - could include jakarta commons lang library instead.
     */
//    private int getLevenshteinDistance(String s, String t) {
//        int n = s.length(); // length of s
//        int m = t.length(); // length of t
//
//        if (n == 0) {
//            return m;
//        } else if (m == 0) {
//            return n;
//        }
//
//        int p[] = new int[n + 1]; // 'previous' cost array, horizontally
//        int d[] = new int[n + 1]; // cost array, horizontally
//        int _d[]; // placeholder to assist in swapping p and d
//
//        // indexes into strings s and t
//        int i; // iterates through s
//        int j; // iterates through t
//
//        char t_j; // jth character of t
//
//        int cost; // cost
//
//        for (i = 0; i <= n; i++) {
//            p[i] = i;
//        }
//
//        for (j = 1; j <= m; j++) {
//            t_j = t.charAt(j - 1);
//            d[0] = j;
//
//            for (i = 1; i <= n; i++) {
//                cost = s.charAt(i - 1) == t_j ? 0 : 1;
//                // minimum of cell to the left+1, to the top+1, diagonally left
//                // and up +cost
//                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
//            }
//
//            // copy current distance counts to 'previous row' distance counts
//            _d = p;
//            p = d;
//            d = _d;
//        }
//
//        // our last action in the above loop was to switch d and p, so p now
//        // actually has the most recent cost counts
//        return p[n];
//
//    }

    /**
     * Removes all symbols and spaces in the string. Converts string to
     * lowercase.
     */
    private String cleanString(String string) {
        string = string.replaceAll("\\(\\d\\)", "");//replace leaf file names eg. file1.txt file1(1).txt, file1(2).txt
        string = string.replaceAll("[-_. ()]", "").toLowerCase();
       
        return string;
    }
}
