package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.FileManager;

/** 
 * The official platform-independent hashing function for query-routing.  The
 * key property is that it allows interpolation of hash tables of different
 * sizes.  More formally k*hash(x,n)<=hash(x, kn)<=k*hash(x,n)+1.<p>
 *
 * This experimental version does not necessarily work cross-platform,
 * however, nor is it secure in any sense.   See Chapter 12.3.2. of 
 * for details of multiplication-based algorithms.
 */
public class HashFunction {
    private static final double A=(Math.sqrt(5.0)-1.0)/2.0;
    private static final long TWO_31=0x80000000l;
    private static final int A_INT=(int)(A*TWO_31);
    
    /*
     * Implementation note: we special case the algorith depending on 
     * whether the hash table size is a power of two.
     */
    
//      /**
//       * Returns the n-<b>bit</b> hash of x, where n="bits".  That is, the
//       * returned value value can fit in "bits" unsigned bits.
//       *     @requires n is a power of two.
//       */
//      private static int hashFast(int x, int n) {
//          Assert.that(n%2==0, "Precondition to hashFast violated.");
//          //Multiplication-based hash function.  See Chapter 12.3.2. of CLR.
//          int prod=x*A_INT;
//          int ret=prod>>>(31-log2(n));
//          return ret;
//      }

    /**
     * Returns the hash of x.  The returned value is greater than 0 and
     * less than n.
     */
    private static int hashSlow(int x, int n) {
        //Multiplication-based hash function.  See Chapter 12.3.2. of CLR.
        double prod=A*x;
        double scale=prod-Math.floor(prod);
        return (int)Math.floor(((float)n) * scale);
    }

    /**
     * Returns the hash of x.  The returned value is greater than 0 and
     * less than n.
     *     @requires 1<=bits<=32 
     */    
    public static int hash(String x, int n) {
        //TODO: This is just a temporary hack.  The real algorithm obviously shouldn't
        //be tied to the JDK.  Note that we don't just return x.hashCode()%bits;
        //that wouldn't allow resizing of tables.
        return hashSlow(x.hashCode(), n);
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
        //TODO: we should verify the scaling property described in the header
        //above.
        System.out.println(hash("Hello", 256));
        System.out.println(hash("Hallo", 256));
        System.out.println(hash("Hellu", 256));
        System.out.println();
        System.out.println(hash("Hello", 257));
        System.out.println(hash("Hallo", 257));
        System.out.println(hash("Hellu", 257));
        System.out.println();                           
        System.out.println(hash("Hello", 512));
        System.out.println(hash("Hallo", 512));
        System.out.println(hash("Hellu", 512));
    }


}
