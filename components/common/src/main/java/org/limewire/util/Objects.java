package org.limewire.util;

public class Objects {
    
    private Objects() {}
    
    public static <T> T nonNull(T t, String msg) {
        if(t == null)
            throw new NullPointerException("null: " + msg);
        return t;
    }

}
