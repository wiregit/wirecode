package com.limegroup.gnutella.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares two Strings via String.compareTo(String).  Useful for storing Java
 * 1.1.8 Strings in Java 1.2+ sorted collections classes.  This is needed
 * because Strings in 1.1.8 do not implement the Comparable interface, unlike
 * Strings in 1.2+. 
 */
final class StringComparator implements Comparator, Serializable {
    static final long serialVersionUID = -624599003446177506L;

    /** Returns ((String)a).compareTo((String)b). */
    public int compare(Object a, Object b) {
        String as=(String)a;
        String bs=(String)b;
        return as.compareTo(bs);
    }
}
