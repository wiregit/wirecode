package com.limegroup.gnutella.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
* A set version of FixedsizeForgetfulHashMap.  
* This forgets values using a FIFO replacement policy, much
* like a cache.  Unlike ForgetfulHashMap, it has better-defined replacement
* policy.  Specifically, it allows values to be renewed when re-added.
* All of this is done in constant time.<p> 
*/
public class FixedsizeForgetfulHashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable {

    /**
     * Backing map which the set delegates.
     */
    private transient FixedsizeForgetfulHashMap<E,Object> map;

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    /**
     * Constructs a new, empty set.
     */
    public FixedsizeForgetfulHashSet(int size) {
        map = new FixedsizeForgetfulHashMap<E,Object>(size);
    }
    
    /**
     * Tests if the set is full
     * 
     * @return true, if the set is full (ie if adding any other entry will
     * lead to removal of some other entry to maintain the fixed-size property
     * of the set). Returns false, otherwise
     */
    public boolean isFull() {
        return map.isFull();
    }
    
    /**
     * Removes the least recently used entry from the set
     * @return The least recently used value from the set.
     * @modifies this
     */
    public E removeLRUEntry() {
        if(isEmpty())
            return null;
        
        Iterator<E> i = iterator();
        E value = i.next();
        i.remove();
        return value;
    }

    /**
     * Returns an iterator over the elements in this set.  The elements
     * are returned in no particular order.
     *
     * @return an Iterator over the elements in this set.
     * @see ConcurrentModificationException
     */
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    /**
     * Returns the number of elements in this set (its cardinality).
     *
     * @return the number of elements in this set (its cardinality).
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns <tt>true</tt> if this set contains no elements.
     *
     * @return <tt>true</tt> if this set contains no elements.
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if this set contains the specified element.
     *
     * @param o element whose presence in this set is to be tested.
     * @return <tt>true</tt> if this set contains the specified element.
     */
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * Adds the specified element to this set if it is not already
     * present.
     *
     * @param o element to be added to this set.
     * @return <tt>true</tt> if the set did not already contain the specified
     * element.
     */
    public boolean add(E o) {
        return map.put(o, PRESENT)==null;
    }

    /**
     * Removes the specified element from this set if it is present.
     *
     * @param o object to be removed from this set, if present.
     * @return <tt>true</tt> if the set contained the specified element.
     */
    public boolean remove(Object o) {
        return map.remove(o)==PRESENT;
    }

    /**
     * Removes all of the elements from this set.
     */
    public void clear() {
        map.clear();
    }

    /**
     * Returns a shallow copy of this <tt>FixedsizeForgetfulHashSet</tt> instance: the elements
     * themselves are not cloned.
     *
     * @return a shallow copy of this set.
     */
    @SuppressWarnings("unchecked")
    public FixedsizeForgetfulHashSet<E> clone() {
        try { 
            FixedsizeForgetfulHashSet<E> newSet = (FixedsizeForgetfulHashSet<E>)super.clone();
            newSet.map = map.clone();
            return newSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

}
