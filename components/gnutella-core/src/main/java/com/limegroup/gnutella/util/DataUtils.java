package com.limegroup.gnutella.util;

import com.sun.java.util.collections.*;


/**
 * Utility class that supplies commonly used data sets that each
 * class should not have to create on its own.  These data sets
 * are immutable objects, so any class and any thread may access them
 * whenever they like.
 */
public final class DataUtils {
    
    /**
     * Ensure that this class cannot be constructed.
     */
    private DataUtils() {}
    
    /**
     * Constant empty byte array for any class to use -- immutable.
     */
    public static byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * Utility immutable emtpy set (not supported in 1.1.8 version of 
     * Collections class in collections).
     */
    public static final Set EMPTY_SET = 
        Collections.unmodifiableSet(new HashSet());
}
