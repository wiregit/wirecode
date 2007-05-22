package org.limewire.collection;

/**
 * A one argument function. 
 */
public interface Function<K, V> {
    /** 
     * Applies this function to argument, returning the result.
     */
    public V apply(K argument);
}
