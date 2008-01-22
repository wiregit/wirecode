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

}
