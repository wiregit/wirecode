package com.limegroup.gnutella.util;

/** 
 * An IllegalArgumentException that carries suggested values for
 * the parameters.  Example use:
 * 
 * <pre>
 * try {
 *   //Try standard parameters.
 *   String a="parameter";
 *   int b=1;
 *   someMethod(a,b);
 * } catch (IllegalArgumentException2 e) {
 *   //If that doesn't work, try suggested parameters.
 *   String a=(String)(e.getSuggestions()[0]);
 *   int b=((Integer)(e.getSuggestions()[1])).intValue();
 *   someMethod(a,b);
 * }
 * </pre>
 */
public class IllegalArgumentException2 extends IllegalArgumentException {
    private Object[] suggestions;

    /** 
     * @param suggestions a list of suggested parameter values for the
     *  client to use in the method, with primitives turned into
     *  objects.  
     */
    public IllegalArgumentException2(Object[] suggestions) {
        super();
        this.suggestions=suggestions;
    }

    /*
     * @param msg a descriptive message
     * @param suggestions a list of suggested parameters for the client
     *   to use in the method.
     */
    public IllegalArgumentException2(String message, Object[] suggestions) {
        super(message);
        this.suggestions=suggestions;
    }

    /**
     * Returns the array of suggested parameter values passed to this'
     * constructor.  The client should not mutate this array. 
     */
    public Object[] getSuggestions() {
        return suggestions;
    }

}
