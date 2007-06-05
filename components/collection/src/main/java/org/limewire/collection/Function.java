package org.limewire.collection;

/**
 * A one argument function. 
 */
public interface Function<I, O> {
    /** 
     * Applies this function to argument, returning the result.
     */
    public O apply(I argument);
}
