package org.limewire.collection;


/** 
 * Creates a byte array with a size that is a power of 2 (byte array size = 2^power).
 * If you try to get a byte array with a size that is not a power of two, the value 
 * is set to the next power of 2. For example, setting a value of 511 returns a byte 
 * array of size 512 because 511 is not a power of 2 (2^8 = 256 and 2^9 = 512). 
 * (If you attempt to create a byte array less than a size zero (base 2^-2), 
 * the size is set to 1.)
 * <p>
 * Additionally, <code>PowerOf2ByteArrayCache</code> stores the total cache size.
 * <pre>
    void sampleCodePowerOf2ByteArrayCache(){
        PowerOf2ByteArrayCache p2 = new PowerOf2ByteArrayCache();
        for(int i = 0; i < 17; i++){
            byte[] ba = p2.get(i);
            int n = p2.getCacheSize();
            System.out.println("i= " +i + " ba size: " + ba.length + " cache size is " + n);
        }
    }
    Output:
        i= 0 ba size: 1 cache size is 1
        i= 1 ba size: 1 cache size is 1
        i= 2 ba size: 2 cache size is 3
        i= 3 ba size: 4 cache size is 7
        i= 4 ba size: 4 cache size is 7
        i= 5 ba size: 8 cache size is 15
        i= 6 ba size: 8 cache size is 15
        i= 7 ba size: 8 cache size is 15
        i= 8 ba size: 8 cache size is 15
        i= 9 ba size: 16 cache size is 31
        i= 10 ba size: 16 cache size is 31
        i= 11 ba size: 16 cache size is 31
        i= 12 ba size: 16 cache size is 31
        i= 13 ba size: 16 cache size is 31
        i= 14 ba size: 16 cache size is 31
        i= 15 ba size: 16 cache size is 31
        i= 16 ba size: 16 cache size is 31
    
  </pre>
 */

public class PowerOf2ByteArrayCache {
    
    private final IntHashMap<byte[]> CACHE = new IntHashMap<byte[]>(20);
    private volatile int totalStored = 0;
    
    /**
     * @return a byte array of the specified size, using cached one
     * if possible.
     */
    public byte [] get(int size) {
        int exp;
        for (exp = 1 ; exp < size ; exp*=2);
        // exp is now >= size.  it will be equal
        // to size if size was a power of two.
        // otherwise, it will be the first power of 2
        // greater than size.
        // since we want to cache only powers of two,
        // we will use exp from hereon.
        
        byte[] ret = CACHE.get(exp);
        if (ret == null) {
            ret = new byte[exp];
            totalStored += exp;
            CACHE.put(exp, ret);
        }
        return ret;
    }
    
    public int getCacheSize() {
        return totalStored;
    }
    
    
    /** Erases all data in the cache. */
    public void clear() {
        CACHE.clear();
        totalStored = 0;
    }

}
