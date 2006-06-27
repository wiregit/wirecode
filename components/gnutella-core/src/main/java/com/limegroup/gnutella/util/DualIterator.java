package com.limegroup.gnutella.util;

import java.util.Iterator;

/**
 * An iterator that iterates over two other iterators, in order.
 */
public class DualIterator<T> implements Iterator<T> {
    
    /**
     * The primary iterator.
     */
    private final Iterator<T> i1;
    
    /**
     * The secondary iterator.
     */
    private final Iterator<T> i2;
    
    /**
     * Whether or not we have reached the secondary iterator.
     */
    private boolean onOne;
    
    /**
     * Constructs a new DualIterator backed by two iterators.
     */
    public DualIterator(Iterator<T> a, Iterator<T> b) {
        i1 = a; i2 = b;
        onOne = true;
    }
    
    /**
     * Determines if there are any elements left in either iterator.
     */
    public boolean hasNext() {
        return i1.hasNext() || i2.hasNext();
    }
    
    /**
     * Retrieves the next element from the current backing iterator.
     */
    public T next() {
        if(i1.hasNext())
            return i1.next();
        else {
            onOne = false;
            return i2.next();
        }
    }
    
    /**
     * Removes the element from the current backing iterator.
     */
    public void remove() {
        if(onOne)
            i1.remove();
        else
            i2.remove();
    }
}
