package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * A variant of <tt>FixedSizeArrayHashSet</tt> that allows iterations over
 * its elements in random order. 
 */
public class RandomOrderHashSet extends FixedSizeArrayHashSet {

    public RandomOrderHashSet(Collection c) {
        super(c);
    }
    
    public RandomOrderHashSet(int capacity, Collection c) {
        super(capacity, c);
    }

    public RandomOrderHashSet(int maxSize, int initialCapacity, float loadFactor) {
        super(maxSize, initialCapacity, loadFactor);
    }

    public RandomOrderHashSet(int initialCapacity) {
        super(initialCapacity);
    }

    public Iterator iterator() {
        return new RandomIterator();
    }
    
    private class RandomIterator extends UnmodifiableIterator {
        private final Iterator sequence = new RandomSequence(size()).iterator();
        
        public boolean hasNext() {
            return sequence.hasNext();
        }
        
        public Object next() {
            return get(((Integer)sequence.next()).intValue());
        }
    }
}
