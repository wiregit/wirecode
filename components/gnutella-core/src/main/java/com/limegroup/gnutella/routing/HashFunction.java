package com.limegroup.gnutella.routing;

/** 
 * The official platform-independent hashing function for query-routing.
 * This experimental version does n
 */
public class HashFunction {
    static final double A=(Math.sqrt(5.0)-1.0)/2.0;
    static final long TWO_31=0x80000000l;
    static final int A_INT=(int)(A*TWO_31);
    
    /**
     * Returns the n-bit hash of x, where n="bits".  That is, the returned value
     * value can fit in "bits" unsigned bits.
     */
    public static int hash(int x, int bits) {
        //Multiplication-based hash function.  See Chapter 12.3.2. of CLR.
        int prod=x*A_INT;
        int ret=prod>>>(31-bits);
        return ret;
    }

    public static void main(String args[]) {
        for (int i=0; i<100; i++) {
            System.out.println(hash(i, 7));
        }
    }


}
