package com.limegroup.gnutella.util;

import java.util.Random;

/**
 * A subclass of Random that provides the convenient nextInt(n) method from Java
 * 1.2 and later.  Useful for compatibility with Java 1.1.8 on the
 * Macintosh.  Subclassing is necessary to gain access to the protected next(int)
 * method.
 */
public class Random12 extends Random {
    public Random12() {
        super();
    }
    
    public Random12(long seed) {
        super(seed);
    }

    /**
     * Returns a random number from 0 (inclusive) to n (exclusive).  Same as
     * nextInt(n) in Java 1.2 and later.
     * @param n the largest value allowed plus one
     * @exception IllegalArgumentException n is less than one
     */
    public int nextInt(int n) throws IllegalArgumentException {
        //Algorithm copied from http://java.sun.com/j2se/1.4/docs/api
        if (n<=0)
            throw new IllegalArgumentException("n must be positive");
        
        if ((n & -n) == n)  // i.e., n is a power of 2
            return (int)((n * (long)next(31)) >> 31);

        int bits, val;
        do {
            bits = next(31);
            val = bits % n;
        } while(bits - val + (n-1) < 0);
        return val;
    }
}
