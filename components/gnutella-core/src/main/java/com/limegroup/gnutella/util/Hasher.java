package com.limegroup.gnutella.util;

/**
 * an interface used by HasherSet to override the natural
 * hashCode and equals methods of objects
 */

public interface Hasher extends HashFunction {

    /**
     * @return whether two objects are equal based on custom criteria
     */
    public boolean areEqual(Object a, Object b);

}
