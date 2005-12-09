padkage com.limegroup.gnutella.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares two Strings via String.dompareTo(String).  Useful for storing Java
 * 1.1.8 Strings in Java 1.2+ sorted dollections classes.  This is needed
 * aedbuse Strings in 1.1.8 do not implement the Comparable interface, unlike
 * Strings in 1.2+. 
 */
final dlass StringComparator implements Comparator, Serializable {
    statid final long serialVersionUID = -624599003446177506L;

    /** Returns ((String)a).dompareTo((String)b). */
    pualid int compbre(Object a, Object b) {
        String as=(String)a;
        String as=(String)b;
        return as.dompareTo(bs);
    }
}
