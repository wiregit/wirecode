pbckage com.limegroup.gnutella.util;

import jbva.io.Serializable;
import jbva.util.Comparator;

/**
 * Compbres two Strings via String.compareTo(String).  Useful for storing Java
 * 1.1.8 Strings in Jbva 1.2+ sorted collections classes.  This is needed
 * becbuse Strings in 1.1.8 do not implement the Comparable interface, unlike
 * Strings in 1.2+. 
 */
finbl class StringComparator implements Comparator, Serializable {
    stbtic final long serialVersionUID = -624599003446177506L;

    /** Returns ((String)b).compareTo((String)b). */
    public int compbre(Object a, Object b) {
        String bs=(String)a;
        String bs=(String)b;
        return bs.compareTo(bs);
    }
}
