package com.limegroup.gnutella.util;

import com.sun.java.util.collections.*;

/**
 * A priority queue with bounded size.  Similar to BinaryHeap, but implemented
 * with a balanced tree instead of a binary heap.  This results in some
 * subtle differences:
 * 
 * <ol> 
 * <li>FixedsizePriorityQueue guarantees that the lowest priority element
 *     is ejected when exceeding capacity.  BinaryHeap provides no such 
 *     guarantees.
 * <li>Fetching the max element takes O(lg N) time, where N is the number of
 *     elements.  Compare with O(1) for BinaryHeap.  Extracting and adding
 *     elements is still O(lg N) time.  
 * <li>FixedsizePriorityQueue can provide operations for extracting the minimum
 *     and the maximum element.  Note, however, that this is still considered
 *     a "max heap", for reasons in (1).
 * </ol>
 *
 * Priorities are expressed by comparing objects, which is assumed to express a
 * total order.  The highest priority entry of this is defined to be the unique
 * object x in this for which x.compareTo(y)>0 for all other y in this.  Like
 * TreeSet all objects must implement
 * <tt>com.sun.java.util.collections.Comparable</tt> or an instance of
 * <tt>com.sun.java.util.collections.Comparator</tt> must be passed to the
 * constructor of this.  Note that these classes did not exist in Java 1.1.8.<p>
 * 
 * <b>This class is not synchronized; that is up to the user.</b><p>
 *
 * This interface of this class is similar to BinaryHeap, but many operations
 * are not yet implemented.<p>
 * 
 * @see BinaryHeap 
 */
public class FixedsizePriorityQueue {
    /** The underlying data structure.
     *  INVARIANT: tree.size()<=capacity */
    private SortedSet tree;
    /** The maximum number of elements to hold. */
    private int capacity;

    /**
     * Creates a new FixedsizePriorityQueue that will hold at most 
     * <tt>capacity</tt> elements, sorted with <tt>comparator</tt>.
     * @param capacity the maximum number of elements
     * @param comparator the Comparator to use when sorting elements
     * @exception IllegalArgumentException capacity negative
     */
    public FixedsizePriorityQueue(int capacity, Comparator comparator)
            throws IllegalArgumentException {
        if (capacity<=0)
            throw new IllegalArgumentException();
        tree=new TreeSet(comparator);
        this.capacity=capacity;
    }

    /**
     * Creates a new FixedsizePriorityQueue that will hold at most 
     * <tt>capacity</tt> elements.
     * @param capacity the maximum number of elements
     * @exception IllegalArgumentException capacity negative
     */
    public FixedsizePriorityQueue(int capacity) 
            throws IllegalArgumentException {
        if (capacity<=0)
            throw new IllegalArgumentException();
        tree=new TreeSet();
        this.capacity=capacity;
    }

    /**
     * Ensures x in this, possibly removing some lower priority entry if
     * necessary to ensure this.size()<=this.capacity().  This is not
     * modified if x already in this.
     *
     * @param x the entry to add
     * @return the element eject, possibly x, or null if none 
     */
    public Object insert(Object x) {
        tree.add(x);

        //Maintain size.  You could probably micro-optimize this if first()
        //returned a pointer to the actual tree node, not just the node's value.
        if (size()>capacity()) {
            Object smallest=tree.first();
            tree.remove(smallest);
            return smallest;
        } else {
            return null;
        }
    }
    
    /**
     * Returns the highest priority element of this.
     * @exception NoSuchElementException this.size()==0
     */
    public Object getMax() throws NoSuchElementException {
        return tree.last();
    }

   /**
     * Returns the lowest priority element of this.
     * @exception NoSuchElementException this.size()==0
     */
    public Object getMin() throws NoSuchElementException {
        return tree.first();
    }

    /** 
     * Returns an iterator of the elements in this, from <b>worst to best</b>.
     */
    public Iterator iterator() {
        return tree.iterator();
    }

    /**
     * Returns the number of elements in this.
     */
    public int size() {
        return tree.size();
    }
    
    /**
     * Returns the maximum number of elements this can hold.
     * @return the value passed to this constructor
     */
    public int capacity() {
        return capacity;
    }

    public String toString() {
        return tree.toString();
    }
}
