package com.limegroup.gnutella.guess;

import com.sun.java.util.collections.*;

/**
 * Abstraction for a Query Key as detailed in the GUESS protocol spec.
 * Provides:
 * - encapsulation of (all, LW and non-LW) Query Keys
 * - generation of Query Keys (hence, it contains the LimeWire QK Algorithm)
 *
 * A Query Key is a credential necessary to perform a GUESS Query.  A Query Key
 * instance is immutable.
 */
public class QueryKey {

    /** As detailed by the GUESS spec.
     */
    public final int MIN_QK_SIZE_IN_BYTES = 4;
    /** As detailed by the GUESS spec.
     */
    public final int MAX_QK_SIZE_IN_BYTES = 16;

    /** The Query Key.  MIN_QK_SIZE_IN_BYTES <=_queryKey.length <=
     *  MAX_QK_SIZE_IN_BYTES
     */
    private byte[] _queryKey;
    
    private QueryKey(byte[] key) throws IllegalArgumentException {
        if ((key.length < MIN_QK_SIZE_IN_BYTES) ||
            (key.length > MAX_QK_SIZE_IN_BYTES)
            )
            throw new IllegalArgumentException();
        _queryKey = new byte[key.length];
        System.arraycopy(key, 0, _queryKey, 0, key.length);
    }


    public boolean equals(Object o) {
        if (!(o instanceof QueryKey))
            return false;
        QueryKey other = (QueryKey) o;
        return Arrays.equals(_queryKey, other._queryKey);
    }

    // NOT A VERY GOOD HASH FUNCTION RIGHT NOW - NO BIGGIE FOR NOW....
    // TODO: make a better hash function
    public int hashCode() {
        int retInt = 0;
        for (int i = 0; i < 4; i++) {
            int index = _queryKey[i]%_queryKey.length;
            if (index < 0)
                index *= -1;
            retInt += _queryKey[index] * 7;
        }
        return retInt;
    }


    //--------------------------------------
    //--- PUBLIC STATIC CONSTRUCTION METHODS


    /** Use this method to construct Query Keys that you get from network
     *  commerce.
     */    
    public static QueryKey getQueryKey(byte[] networkQK) 
        throws IllegalArgumentException {
        return new QueryKey(networkQK);
    }
        


    //--------------------------------------


}
