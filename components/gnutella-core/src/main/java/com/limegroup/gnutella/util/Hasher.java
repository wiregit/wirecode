package com.limegroup.gnutella.util;

/**
 * an interface used by HasherSet to override the natural
 * hashCode and equals methods of objects
 */

public interface Hasher {

    /**
     * @return custom hash code for the given object
     */
    public int hash(Object o);

    /**
     * @return whether two objects are equal based on custom criteria
     */
    public boolean areEqual(Object a, Object b);

}
