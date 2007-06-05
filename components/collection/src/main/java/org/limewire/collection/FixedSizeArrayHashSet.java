package org.limewire.collection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;


/**
 * A fixed size hashset that provides indexed access.  The replacement
 * policy is FIFO and the iteration order is from newest to oldest.
 * 
 * Adding an already existing element will postpone the ejection of that
 * element. 
 * 
 * It does not support the null element.
 */
public class FixedSizeArrayHashSet<E> extends HashSet<E> implements RandomAccessCollection<E> {

    private transient FixedSizeArrayHashMap<E,Object> map;

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();
    
    
    /**
     * creates a FixedSizeArrayHashSet with the specified maximum capacity.
     */
    public FixedSizeArrayHashSet(int maxCapacity) {
        map = new FixedSizeArrayHashMap<E, Object>(maxCapacity);
    }

    /**
     * creates a FixedSizeArrayHashSet with maximum capacity the size of the
     * provided collection and adds all the elements of that collection.
     */
    public FixedSizeArrayHashSet(Collection<? extends E> c) {
        this(c.size());
        addAll(c);
    }
    
    /**
     * creates a FixedSizeArrayHashSet with the provided maximum capacity and
     * adds elements from the provided collection.  If the capacity is less than
     * the size of the collection, elements will get ejected with FIFO policy.
     */
    public FixedSizeArrayHashSet(int maxCapacity, Collection<? extends E> c) {
        this(maxCapacity);
        addAll(c);
    }

    public FixedSizeArrayHashSet(int maxCapacity, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        map = new FixedSizeArrayHashMap<E, Object>(maxCapacity);
    }

    public FixedSizeArrayHashSet(int maxCapacity, int initialCapacity) {
        super(initialCapacity);
        map = new FixedSizeArrayHashMap<E, Object>(maxCapacity);
    }


    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(E o) {
        return map.put(o, PRESENT)==null;
    }

    @Override
    public boolean remove(Object o) {
    return map.remove(o)==PRESENT;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object clone() {
       FixedSizeArrayHashSet<E> newSet = (FixedSizeArrayHashSet<E>) super.clone();
       newSet.map = (FixedSizeArrayHashMap<E, Object>) map.clone();
       return newSet;
    }
    
    public E get(int i) {
        return map.getKeyAt(i);
    }
}
