package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.*;

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
        
    /**
     * Returns the n-<b>bit</b> hash of x, where n="bits".  That is, the
     * returned value value can fit in "bits" unsigned bits, and is
     * between 0 and (2^bits)-1.
     */
    private static int hashFast(int x, byte bits) {
        //Multiplication-based hash function.  See Chapter 12.3.2. of CLR.
        long prod= (long)x * (long)A_INT;
        long ret= prod << 32;
        ret = ret >>> (32 + (32 - bits));
        return (int)ret;
    }

    /*
     * Returns the n-<b>bit</b> hash of x.toLowerCase(), where n="bits".  That
     * is, the returned value value can fit in "bits" unsigned bits, and is
     * between 0 and (2^bits)-1.
     *
     * @param x the string to hash
     * @param bits the number of bits to use in the resulting answer
     * @return the hash value
     */    
    public static int hash(String x, byte bits) {
        return hash(x, 0, x.length(), bits);
    }       

    /**
     * Returns the same value as hash(x.substring(start, end), bits),
     * but tries to avoid allocations.  Note that x is lower-cased
     * when hashing.
     *
     * @param x the string to hash
     * @param bits the number of bits to use in the resulting answer
     * @param start the start offset of the substring to hash
     * @param end just PAST the end of the substring to hash
     * @return the hash value 
     */   
    public static int hash(String x, int start, int end, byte bits) {
        //1. First turn x[start...end-1] into a number by treating all 4-byte
        //chunks as a little-endian quadword, and XOR'ing the result together.
        //We pad x with zeroes as needed. 
        //    To avoid having do deal with special cases, we do this by XOR'ing
        //a rolling value one byte at a time, taking advantage of the fact that
        //x XOR 0==x.
        int xor=0;  //the running total
        int j=0;    //the byte position in xor.  INVARIANT: j==(i-start)%4
        for (int i=start; i<end; i++) {
            //TODO: internationalization be damned?
            int b=Character.toLowerCase(x.charAt(i)) & 0xFF; 
            b=b<<(j*8);
            xor=xor^b;
            j=(j+1)%4;
        }
        //2. Now map number to range 0 - (2^bits-1).
        return hashFast(xor, bits);
    }


    /** 
     * Returns a list of canonicalized keywords in the given query, suitable
     * for passing to hash(String,int).  The returned keywords are
     * lower-cased, though that is not strictly needed as hash ignores
     * case.
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

    /** 
     * Returns the index of the keyword starting at or after the i'th position
     * of query, or -1 if no such luck.
     */
    public static int keywordStart(String query, int i) {
        //Search for the first character that is not a delimiterer TODO3: we can
        //make this O(|DELIMETERS|) times faster by converting
        //FileManager.DELIMETERS into a Set in this' static initializer.  But
        //then we have to allocate Strings here.  Can work around the problem,
        //but it's trouble.
        final String DELIMETERS=FileManager.DELIMETERS;
        for ( ; i<query.length() ; i++) {
            char c=query.charAt(i);
            //If c not in DELIMETERS, declare success.
            if (DELIMETERS.indexOf(c)<0)
                return i;
        }
        return -1;
    }   

    /** 
     * Returns the index just past the end of the keyword starting at the i'th
     * position of query, or query.length() if no such index.
     */
    public static int keywordEnd(String query, int i) {
        //Search for the first character that is a delimiterer.  
        //TODO3: see above
        final String DELIMETERS=FileManager.DELIMETERS;
        for ( ; i<query.length() ; i++) {
            char c=query.charAt(i);
            //If c in DELIMETERS, declare success.
            if (DELIMETERS.indexOf(c)>=0)
                return i;
        }
        return query.length();
    }    
        

    /**
     * @return an array of strings with the original strings and prefixes
     */
    public static String[] getPrefixes(String[] words){
        ArrayList l = new ArrayList();
        for(int i=0;i<words.length;i++){
            //add the string itself
            l.add(words[i]);
            int len = words[i].length();
            if(len>4){//if we can have prefixes add them to the list
                l.add(words[i].substring(0,len-1));
                l.add(words[i].substring(0,len-2));
            }
        }//done!
        //convert to a String[]...could not do this directly...since
        //we did not know now many of the words are longer than 4 chars
        String[] retArray = new String[l.size()];
        for(int i=0;i<l.size();i++)
            retArray[i]=(String)l.get(i);
        return retArray;
    }

    /*
    public static void main(String args[]) {
        //1. Basic hash tests.  These unit tests were generated by the reference
        //implementation of HashFunction.  Some I've checked manually.
        Assert.that(hash("", (byte)13)==0);
        Assert.that(hash("eb", (byte)13)==6791);
        Assert.that(hash("ebc", (byte)13)==7082);
        Assert.that(hash("ebck", (byte)13)==6698);
        Assert.that(hash("ebckl", (byte)13)==3179);
        Assert.that(hash("ebcklm", (byte)13)==3235);
        Assert.that(hash("ebcklme", (byte)13)==6438);
        Assert.that(hash("ebcklmen", (byte)13)==1062);
        Assert.that(hash("ebcklmenq", (byte)13)==3527);
        Assert.that(hash("", (byte)16)==0);
        Assert.that(hash("n", (byte)16)==65003);
        Assert.that(hash("nd", (byte)16)==54193);
        Assert.that(hash("ndf", (byte)16)==4953);
        Assert.that(hash("ndfl", (byte)16)==58201);
        Assert.that(hash("ndfla", (byte)16)==34830);
        Assert.that(hash("ndflal", (byte)16)==36910);
        Assert.that(hash("ndflale", (byte)16)==34586);
        Assert.that(hash("ndflalem", (byte)16)==37658);
        Assert.that(hash("ndflaleme", (byte)16)==45559);
        Assert.that(hash("ol2j34lj", (byte)10)==318);
        Assert.that(hash("asdfas23", (byte)10)==503);
        Assert.that(hash("9um3o34fd", (byte)10)==758);
        Assert.that(hash("a234d", (byte)10)==281);
        Assert.that(hash("a3f", (byte)10)==767);
        Assert.that(hash("3nja9", (byte)10)==581);
        Assert.that(hash("2459345938032343", (byte)10)==146);
        Assert.that(hash("7777a88a8a8a8", (byte)10)==342);
        Assert.that(hash("asdfjklkj3k", (byte)10)==861);
        Assert.that(hash("adfk32l", (byte)10)==1011);
        Assert.that(hash("zzzzzzzzzzz", (byte)10)==944);

        //2. Offset tests.
        Assert.that(hash("ndfl", 0, 4, (byte)16)==58201);
        Assert.that(hash("_ndfl_", 1, 1+4, (byte)16)==58201);
        Assert.that(hash("__ndfl__", 2, 2+4, (byte)16)==58201);
        Assert.that(hash("___ndfl___", 3, 3+4, (byte)16)==58201);

        //3. Case tests.
        Assert.that(hash("3nja9", (byte)10)==581);
        Assert.that(hash("3NJA9", (byte)10)==581);
        Assert.that(hash("3nJa9", (byte)10)==581);
    }
    */

}
