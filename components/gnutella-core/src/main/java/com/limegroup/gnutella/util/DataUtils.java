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
     * An empty byte array length 1.
     */
    public static byte[] BYTE_ARRAY_ONE = new byte[1];
    
    /**
     * An empty byte array length 2.
     */
    public static byte[] BYTE_ARRAY_TWO = new byte[2];
    
    /**
     * An empty byte array length 3.
     */
    public static byte[] BYTE_ARRAY_THREE = new byte[3];
    
    static {
        BYTE_ARRAY_ONE[0] = 0;
        BYTE_ARRAY_TWO[0] = 0;
        BYTE_ARRAY_TWO[1] = 0;
        BYTE_ARRAY_THREE[0] = 0;
        BYTE_ARRAY_THREE[1] = 0;
        BYTE_ARRAY_THREE[2] = 0;
    }
    
    /**
     * Constant empty string array for any class to use -- immutable.
     */
    public static String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Utility immutable emtpy set (not supported in 1.1.8 version of 
     * Collections class in collections).
     */
    public static final Set EMPTY_SET = 
        Collections.unmodifiableSet(new HashSet(0));
        
    /**
     * An empty list.
     */
    public static final List EMPTY_LIST =
        Collections.unmodifiableList(new ArrayList(0));
        
    /**
     * An empty map.
     */
    public static final Map EMPTY_MAP =
        Collections.unmodifiableMap(new HashMap(0));
        
    /**
     * An 16-length empty byte array, for GUIDs.
     */
    public static final byte[] EMPTY_GUID = new byte[16];
    
    /**
     * The amount of milliseconds in a week.
     */
    public static final long ONE_WEEK = 7 * 24 * 60 * 60 * 1000;
    
    /**
     * Determines whether or not the the child Set contains any elements
     * that are in the parent's set.
     */
    public static boolean containsAny(Collection parent, Collection children) {
        for(Iterator i = children.iterator(); i.hasNext(); )
            if(parent.contains(i.next()))
                return true;
        return false;
    }    
    
    /**
     * Utility function to write out the toString contents
     * of a URN.
     */
    public static String listSet(Set s) {
        StringBuffer sb = new StringBuffer();
        for(Iterator i = s.iterator(); i.hasNext();)
            sb.append(i.next().toString());
        return sb.toString();
    }
}
