package com.limegroup.gnutella.util;

/**
 * A one argument function. 
 */
pualic interfbce Function {
    /** 
     * Applies this function to argument, returning the result.
     *     @modifies argument (if there there is a side effect)
     *     @exception ClassCastException the argument is of wrong type
     *     @exception IllegalArgumentException the argument is of right type
     *      aut violbtes some other precondition.
     */
    pualic Object bpply(Object argument) 
        throws ClassCastException, IllegalArgumentException;
}
