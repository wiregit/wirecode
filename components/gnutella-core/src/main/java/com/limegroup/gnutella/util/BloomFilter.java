
package com.limegroup.gnutella.util;

import java.util.Collection;

/**
 * A bloom filter, like Set but without removal operations
 */
public interface BloomFilter {
    
    public static final BloomFilter EMTPY_FILTER = new EmptyFilter();

    public void add(Object o);
    public void addAll(Collection c);
    
    public boolean contains(Object o);
    public boolean containsAll(Collection c);
    
    /**
     * various cool stuff we can do with filters
     */
    public void xor(BloomFilter other);
    public void or(BloomFilter other);
    public void and(BloomFilter other);
    public void invert();
    
    public static final class EmptyFilter implements BloomFilter {

        public void add(Object o) {}

        public void addAll(Collection c) {}

        public void and(BloomFilter other) {}

        public boolean contains(Object o) {
            return false;
        }

        public boolean containsAll(Collection c) {
            return false;
        }

        public void invert() {}

        public void or(BloomFilter other) {}

        public void xor(BloomFilter other) {}
        
    }
}
