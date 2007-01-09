package org.limewire.collection;

import java.util.Collection;
import java.util.Iterator;


/**
 * A variant of <tt>FixedSizeArrayHashSet</tt> that allows iterations over
 * its elements in random order. 
 */
public class RandomOrderHashSet<T> extends FixedSizeArrayHashSet<T> {

    public RandomOrderHashSet(Collection<? extends T> c) {
        super(c);
    }
    
    public RandomOrderHashSet(int capacity, Collection<? extends T> c) {
        super(capacity, c);
    }

    public RandomOrderHashSet(int maxSize, int initialCapacity, float loadFactor) {
        super(maxSize, initialCapacity, loadFactor);
    }

    public RandomOrderHashSet(int initialCapacity) {
        super(initialCapacity);
    }

    public Iterator<T> iterator() {
        return new RandomIterator();
    }
    
    private class RandomIterator extends UnmodifiableIterator<T> {
        private final Iterator<Integer> sequence = new RandomSequence(size()).iterator();
        
        public boolean hasNext() {
            return sequence.hasNext();
        }
        
        public T next() {
            return get(sequence.next());
        }
    }
}
