package org.limewire.collection;

import java.util.Collection;

/**
 * A fixed size Last-In-First-Out set. When the maximum capacity
 * is reached, the last element is removed from the set and the new 
 * one is inserted at the head.
 */
public class FixedSizeLIFOSet<E> extends LIFOSet<E> {

    int maxSize;
    
    public FixedSizeLIFOSet(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    protected FixedSizeLIFOSet(int maxSize, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(E o) {
        boolean added = super.add(o);
        if(added && size() > maxSize) {
            removeEldest();
            assert (size() == maxSize);
        }
        return added;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = super.addAll(c);
        if(modified) {
            while(size() > maxSize) {
                removeEldest();
            }
        }
        assert (size() == maxSize);
        return modified;
    }
    
}
