package com.limegroup.gnutella.util;

import com.sun.java.util.collections.Comparator;

/**
 * Compares to Strings via String.compareTo(String).  Useful for storing Java
 * 1.1.8 Strings in Java 1.2+ sorted collections classes.  This is needed
 * because Strings in 1.1.8 do not implement the Comparable interface, unlike
 * Strings in 1.2+. 
 */
public class StringComparator implements Comparator {
    /** Returns ((String)a).compareTo((String)b). */
    public int compare(Object a, Object b) {
        String as=(String)a;
        String bs=(String)b;
        return as.compareTo(bs);
    }
}
