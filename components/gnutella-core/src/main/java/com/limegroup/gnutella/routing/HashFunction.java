package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.FileManager;

/** 
 * The official platform-independent hashing function for query-routing.
 * This experimental version does not necessarily work cross-platform,
 * not is it secure in any sense.
 */
public class HashFunction {
    static final double A=(Math.sqrt(5.0)-1.0)/2.0;
    static final long TWO_31=0x80000000l;
    static final int A_INT=(int)(A*TWO_31);
    
    /**
     * Returns the n-bit hash of x, where n="bits".  That is, the returned value
     * value can fit in "bits" unsigned bits.
     */
    private static int hash(int x, int bits) {
        //Multiplication-based hash function.  See Chapter 12.3.2. of CLR.
        int prod=x*A_INT;
        int ret=prod>>>(31-bits);
        return ret;
    }

    /**
     * Returns the n-bit hash of x, where n="bits".  That is, the returned value
     * value can fit in "bits" unsigned bits.
     */    
    public static int hash(String x, int bits) {
        //TODO: This is just a temporary hack.  The real algorithm obviously shouldn't
        //be tied to the JDK.  Note that we don't just return x.hashCode()%bits;
        //that wouldn't allow resizing of tables.
        return hash(x.hashCode(), bits);
    }       

    /** 
     * Returns a list of canonicalized keywords in the given query, suitable
     * for passing to hash(String,int).
     */
    public static String[] keywords(String query) {
        //TODO1: this isn't a proper implementation.  It should really be
        //to tokenized by ALL non-alphanumeric characters.

        //TODO2: perhaps we should do an English-specific version that accounts
        //for plurals, common keywords, etc.  But that's only necessary for 
        //our own files, since the assumption is that queries have already been
        //canonicalized.
        return StringUtils.split(query.toLowerCase(), FileManager.DELIMETERS);
    }

    public static void main(String args[]) {
        for (int i=0; i<100; i++) {
            System.out.println(hash(i, 7));
        }
        System.out.println(hash("Hello Chris", 10));
        System.out.println(hash("Hallo Chris", 10));
    }


}
