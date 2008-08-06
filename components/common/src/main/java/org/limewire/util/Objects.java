package org.limewire.util;

/** A utility class designed to easily perform simple Object checks. */
public class Objects {
    
    private Objects() {}
    
    /** Throws an exception with the given message if <code>t</code> is null. */
    public static <T> T nonNull(T t, String msg) {
        if(t == null)
            throw new NullPointerException("null: " + msg);
        return t;
    }

    /**
     * @param o1
     * @param o2
     * @return true if both objects are null OR if <code>o1.equals(o2)</code>
     */
    public static boolean equalOrNull(Object o1, Object o2) {
        if(o1 == null && o2 == null) {
            return true;
        }
        if(o1 == null || o2 == null) {
            return false;
        }
        return o1.equals(o2);
    }

}
