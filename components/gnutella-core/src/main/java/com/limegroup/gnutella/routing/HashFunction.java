package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.ByteOrder;

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
    //private static final int A_INT=(int)(A*TWO_31); //=1327217884
    private static final int A_INT=0x4F1BBCDC;
    
    /*
     * Implementation note: we special case the algorith depending on 
     * whether the hash table size is a power of two.
     */
    
//      /**
//       * Returns the n-<b>bit</b> hash of x, where n="bits".  That is, the
//       * returned value value can fit in "bits" unsigned bits.
//       *     @requires n is a power of two.
//       */
    private static int hashFast(int x, byte bits) {
        //Multiplication-based hash function.  See Chapter 12.3.2. of CLR.
        long prod= (long)x * (long)A_INT;
        long ret= prod << 32;
        ret = ret >>> (32 + (32 - bits));
        return (int)ret;
    }

//    /**
//     * Returns the hash of x.  The returned value is greater than 0 and
//     * less than n.
//     */
//    private static int hashSlow(int x, int n) {
//        //Multiplication-based hash function.  See Chapter 12.3.2. of CLR.
//        double prod=A*x;
//        double scale=prod-Math.floor(prod);
//        return (int)Math.floor(((float)n) * scale);
//    }

    /**
     * Returns the hash of x.  The returned value is greater than 0 and
     * less than n.
     *     @requires 1<=bits<=32 
     */    
    public static int hash(String x, byte bits) {
        //TODO2: can you do this without allocations?

        //Get the bytes of x, padding with zeroes so length is a multiple of 4.
        byte[] bytes=x.getBytes();        
        byte[] bytes4=null;
        if (bytes.length%4==0)
            bytes4=bytes;
        else {
            bytes4=new byte[bytes.length+(4-(bytes.length%4))];
            System.arraycopy(bytes, 0, bytes4, 0, bytes.length);
        }
        //XOR every 4 bytes together.
        int xor=0;
        for (int i=0; i<bytes4.length; i+=4)
            xor=xor^ByteOrder.leb2int(bytes4, i);
        //And fit number to n.
        return hashFast(xor, bits);
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
        System.out.println("Informal scaling tests:");
        System.out.println(hash("Hello", (byte)16));
        System.out.println(hash("Hallo", (byte)16));
        System.out.println(hash("Hellu", (byte)16));
        System.out.println();
        System.out.println(hash("Hello", (byte)16));
        System.out.println(hash("Hallo", (byte)16));
        System.out.println(hash("Hellu", (byte)16));
        System.out.println();                           
        System.out.println(hash("Hello", (byte)16));
        System.out.println(hash("Hallo", (byte)16));
        System.out.println(hash("Hellu", (byte)16));
        System.out.println();                           

        System.out.println("Testing keywords:");
        test("Music");
        test("Real");
        test("Nixon");
        test("");
        test("A");
        test("AB");
        test("abc");
    }

    private static void test(String s) {
        System.out.println(s+": "+hash(s, (byte)16));
        s=s.toLowerCase();
        System.out.println(s+": "+hash(s, (byte)16));
    }


}
