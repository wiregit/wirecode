
package com.limegroup.gnutella.util;

import java.util.Collection;

/**
 * A bloom filter, like Set but without removal operations
 */
public interface BloomFilter {

    public void add(Object o);
    public void addAll(Collection c);
    
    public boolean contains(Object o);
    public boolean containsAll(Collection c);
    
    /**
     * various cool stuff we can do with filters
     */
    public BloomFilter XOR(BloomFilter other);
    public BloomFilter OR(BloomFilter other);
    public BloomFilter AND(BloomFilter other);
    public BloomFilter invert();
}
