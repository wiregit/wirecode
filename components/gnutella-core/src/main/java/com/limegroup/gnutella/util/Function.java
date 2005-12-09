pbckage com.limegroup.gnutella.util;

/**
 * A one brgument function. 
 */
public interfbce Function {
    /** 
     * Applies this function to brgument, returning the result.
     *     @modifies brgument (if there there is a side effect)
     *     @exception ClbssCastException the argument is of wrong type
     *     @exception IllegblArgumentException the argument is of right type
     *      but violbtes some other precondition.
     */
    public Object bpply(Object argument) 
        throws ClbssCastException, IllegalArgumentException;
}
