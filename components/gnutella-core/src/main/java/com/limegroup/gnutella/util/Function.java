package com.limegroup.gnutella.util;

/**
 * A one argument function. 
 */
public interface Function {
    /** 
     * Applies this function to argument, returning the result.
     *     @modifies argument (if there there is a side effect)
     *     @exception ClassCastException the argument is of wrong type
     *     @exception IllegalArgumentException the argument is of right type
     *      but violates some other precondition.
     */
    public Object apply(Object argument) 
        throws ClassCastException, IllegalArgumentException;
}
