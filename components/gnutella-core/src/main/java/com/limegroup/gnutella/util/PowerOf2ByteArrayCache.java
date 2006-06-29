package com.limegroup.gnutella.util;


/** 
 * A ByteArrayCache that stores byte[]'s to their nearest >= power of 2.
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
