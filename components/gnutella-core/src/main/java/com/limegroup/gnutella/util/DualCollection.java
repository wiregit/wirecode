package com.limegroup.gnutella.util;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * A collection that can be expired to swap out older entries.
 *
 * Newer elements are added into one internal collection.  When 'expire'
 * is called, these entries are moved to a secondary collection.  When 'expire'
 * is called again, the secondary collection is erased.  All methods act
 * upon both internal collections, refreshing the newest collection with each
 * 'add' method.
 */
public class DualCollection extends AbstractCollection {
    
    /**
     * The most recent collection.
     */
    private Collection _c1;
    
    /**
     * The secondary collection.
     */
    private Collection _c2;
    
    /**
     * Constructs a new DualCollection using the collection type returned
     * by createCollection().
     */
    public DualCollection() {
        _c1 = createCollection();
        _c2 = createCollection();
    }
    
    /**
     * Constructs a DualCollection that is initially backed by these two collections.
     *
     * This is useful not only for expiring, but also for wrapping two collections
     * into a single one without requiring that either collection is changed.
     * No allocations (other than the creation of this class) are done.
     */
    public DualCollection(Collection a, Collection b) {
        _c1 = a;
        _c2 = b;
    }
    
    /**
     * Swaps the primary collection to the secondary collection and creates
     * a new primary collection.
     * The former secondary collection is left to be GC'd.
     */
    public void expire() {
        _c2 = _c1;
        _c1 = createCollection();
    }
    
    /**
     * Constructs a new collection.  By default, this returns an ArrayList.
     */
    protected Collection createCollection() {
        return new ArrayList();
    }
    
    /**
     * Adds to the primary collection.  If it existed in the secondary collection,
     * it is removed.
     */
    public boolean add(Object o) {
        boolean there = _c1.add(o);
        there  = _c2.remove(o) || there;
        return there;
    }

    /**
     * Adds all elements to the primary collection.  If any existed in the secondary
     * collection, they are removed.
     */
    public boolean addAll(Collection c) {
        boolean there = _c1.addAll(c);
        there = _c2.removeAll(c) || there;
        return there;
    }
    
    /**
     * Clears both the primary & secondary collection.
     */
    public void clear() {
        _c1.clear(); _c2.clear();
    }
    
    /**
     * Determines if the object exists in either the primary or secondary collection.
     */
    public boolean contains(Object o) {
        return _c1.contains(o) || _c2.contains(o);
    }
    
    /**
     * Iterates through each element to determine if they're contained.
     */
    public boolean containsAll(Collection c) {
        return super.containsAll(c);
    }
    
    /**
     * Determines if both the primary & secondary collections are empty.
     */
    public boolean isEmpty() {
        return super.isEmpty();
    }
        
    /**
     * Determines if this collection equals another collection.
     */
    public boolean equals(Object o) {
        return super.equals(o);
    }
    
    /**
     * Generates the hashcode for this collection.
     */
    public int hashCode() {
        return super.hashCode();
    }
    
    /**
     * Returns a DualIterator that iterates over the primary & then secondary collection.
     */
    public Iterator iterator() {
        return new DualIterator(_c1.iterator(), _c2.iterator());
    }
 
    /**
     * Removes first from the primary collection and then (if it didn't exist)
     * from the secondary collection.
     */
    public boolean remove(Object o) {
        return _c1.remove(o) || _c2.remove(o);
    }
    
    /**
     * Removes all elements from the primary & secondary collection.
     */
    public boolean removeAll(Collection c) {
        boolean removed = _c1.removeAll(c);
        removed = _c2.removeAll(c) || removed;
        return removed;
    }
    
    /**
     * Retains only those elements that are in the given collection.
     */
    public boolean retainAll(Collection c)  {
        boolean retained = _c1.retainAll(c);
        retained = _c2.retainAll(c) || retained;
        return retained;
    }
    
    /**
     * Returns the size of the primary & secondary collection.
     *
     * Note that this class ensures that there will never be the same
     * element in both the primary & secondary collection, so this
     * method works just by adding the two sizes.
     */
    public int size() {
        return _c1.size() + _c2.size();
    }
 
    /**
     * Returns the objects in the primary & secondary collection as an array.
     */
    public Object[] toArray() {
        return super.toArray();
	}
        
    /**
     * Returns the objects in the primary & secondary collection as an array,
     * attempting to fit the elements within an array of type 'a'.
     */
    public Object[] toArray(Object[] a) {
        return super.toArray(a);
    } 
}