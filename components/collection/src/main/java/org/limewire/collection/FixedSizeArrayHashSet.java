package org.limewire.collection;

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
public class FixedSizeArrayHashSet<T> extends HashSet<T> implements RandomAccess {

    private Buffer<T> buf;
    
    /**
     * creates a FixedSizeArrayHashSet with the specified maximum capacity.
     */
    public FixedSizeArrayHashSet(int maxCapacity) {
        buf = new Buffer<T>(maxCapacity);
    }

    /**
     * creates a FixedSizeArrayHashSet with maximum capacity the size of the
     * provided collection and adds all the elements of that collection.
     */
    public FixedSizeArrayHashSet(Collection<? extends T> c) {
        this(c.size(),c);
    }
    
    /**
     * creates a FixedSizeArrayHashSet with the provided maximum capacity and
     * adds elements from the provided collection.  If the capacity is less than
     * the size of the collection, elements will get ejected with FIFO policy.
     */
    public FixedSizeArrayHashSet(int maxCapacity, Collection<? extends T> c) {
        buf = new Buffer<T>(maxCapacity);
        addAll(c);
    }

    public FixedSizeArrayHashSet(int maxCapacity, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        buf = new Buffer<T>(maxCapacity);
    }

    public FixedSizeArrayHashSet(int maxCapacity, int initialCapacity) {
        super(initialCapacity);
        buf = new Buffer<T>(maxCapacity);
    }

    @Override
    public boolean add(T e) {
        if (e == null)
            throw new IllegalArgumentException("null element not supported");
        
        boolean ret = super.add(e);
        
        if (ret) {
            // eject oldest element if size reached
           T removed = buf.add(e);
           if (removed != null)
               super.remove(removed);
        } else {
            // refresh this element
            buf.remove(e);
            buf.add(e);
        }
        
        return ret;
    }
    
    @Override
    public void clear() {
        buf.clear();
        super.clear();
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public Object clone() {
        FixedSizeArrayHashSet<T> newSet = (FixedSizeArrayHashSet<T>)super.clone();
        newSet.buf = buf.clone();
        return newSet;
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayHashSetIterator();
    }
    
    public T get(int i) {
        return buf.get(i);
    }

    @Override
    public boolean remove(Object o) {
        boolean ret = super.remove(o);
        if (ret)
            buf.remove(o);
        return ret;
    }
    
    private class ArrayHashSetIterator extends UnmodifiableIterator<T> {
        private final Iterator<T> iter = buf.iterator();
        private T current;
        public boolean hasNext() {
            return iter.hasNext();
        }
        
        public T next() {
            current = iter.next();
            return current;
        }
    }
}
