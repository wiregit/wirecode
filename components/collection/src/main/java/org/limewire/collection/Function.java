package org.limewire.collection;

/**
 * Defines the interface for passing a type as an argument to a method, with a 
 * return value of a type. 
 */
public interface Function<K, V> {
    /** 
     * Applies this function to argument, returning the result.
     *     Modifies argument (if there there is a side effect).
     *     @exception ClassCastException the argument is of wrong type
     *     @exception IllegalArgumentException the argument is of right type
     *      but violates some other precondition.
     */
    public V apply(K argument) 
        throws ClassCastException, IllegalArgumentException;
}
