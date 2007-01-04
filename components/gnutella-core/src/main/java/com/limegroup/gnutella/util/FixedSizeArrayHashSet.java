package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.RandomAccess;

/**
 * A fixed size hashset that provides indexed access.  The replacement
 * policy is FIFO and the iteration order is from newest to oldest.
 * 
 * Adding an already existing element will postpone the ejection of that
 * element. 
 * 
 * It does not support the null element.
 */
public class FixedSizeArrayHashSet extends HashSet implements RandomAccess {

    private Buffer buf;
    
    /**
     * creates a FixedSizeArrayHashSet with the specified maximum capacity.
     */
    public FixedSizeArrayHashSet(int maxCapacity) {
        buf = new Buffer(maxCapacity);
    }

    /**
     * creates a FixedSizeArrayHashSet with maximum capacity the size of the
     * provided collection and adds all the elements of that collection.
     */
    public FixedSizeArrayHashSet(Collection c) {
        this(c.size(),c);
    }
    
    /**
     * creates a FixedSizeArrayHashSet with the provided maximum capacity and
     * adds elements from the provided collection.  If the capacity is less than
     * the size of the collection, elements will get ejected with FIFO policy.
     */
    public FixedSizeArrayHashSet(int maxCapacity, Collection c) {
        buf = new Buffer(maxCapacity);
        addAll(c);
    }

    public FixedSizeArrayHashSet(int maxCapacity, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        buf = new Buffer(maxCapacity);
    }

    public FixedSizeArrayHashSet(int maxCapacity, int initialCapacity) {
        super(initialCapacity);
        buf = new Buffer(maxCapacity);
    }

    public boolean add(Object e) {
        if (e == null)
            throw new IllegalArgumentException("null element not supported");
        
        boolean ret = super.add(e);
        
        if (ret) {
            // eject oldest element if size reached
           Object removed = buf.add(e);
           if (removed != null)
               super.remove(removed);
        } else {
            // refresh this element
            buf.remove(e);
            buf.add(e);
        }
        
        return ret;
    }
    
    public void clear() {
        buf.clear();
        super.clear();
    }

    public Object clone() {
        FixedSizeArrayHashSet newSet = (FixedSizeArrayHashSet)super.clone();
        newSet.buf = (Buffer)buf.clone();
        return newSet;
    }

    public Iterator iterator() {
        return new ArrayHashSetIterator();
    }
    
    public Object get(int i) {
        return buf.get(i);
    }

    public boolean remove(Object o) {
        boolean ret = super.remove(o);
        if (ret)
            buf.remove(o);
        return ret;
    }
    
    private class ArrayHashSetIterator extends UnmodifiableIterator {
        private final Iterator iter = buf.iterator();
        private Object current;
        public boolean hasNext() {
            return iter.hasNext();
        }
        
        public Object next() {
            current = iter.next();
            return current;
        }
    }
}
