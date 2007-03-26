package org.limewire.collection;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
 * <li>FixedsizePriorityQueue REQUIRES an explicit Comparator; it won't
 *     use the natural ordering of values.
 * </ol>
 * 
 * <b>This class is not synchronized; that is up to the user.</b><p>
 * 
 * @see BinaryHeap 
 */
public class FixedsizePriorityQueue<E> implements Iterable<E> {
    /** 
     * The underlying data structure.
     * INVARIANT: tree.size()<=capacity 
     * INVARIANT: all elements of tree instanceof Node
     */
    private SortedList<E> tree;
    /** The maximum number of elements to hold. */
    private int capacity;

       /**
     * Creates a new FixedsizePriorityQueue that will hold at most 
     * <tt>capacity</tt> elements.
     * @param comparator expresses priority.  Note that
     *  comaparator.compareTo(a,b)==0 does not imply that a.equals(b).
     * @param capacity the maximum number of elements
     * @exception IllegalArgumentException capacity negative
     */
    public FixedsizePriorityQueue(Comparator<? super E> comparator, int capacity) 
            throws IllegalArgumentException {
        if (capacity<=0)
            throw new IllegalArgumentException();
        tree=new SortedList<E>(comparator);
        this.capacity=capacity;
    }

    /**
     * Adds x to this, possibly removing some lower priority entry if necessary
     * to ensure this.size()<=this.capacity().  If this has capacity, x will be
     * added even if already in this (possibly with a different priority).
     *
     * @param x the entry to add
     * @param priority the priority of x, with higher numbers corresponding
     *  to higher priority
     * @return the element ejected, possibly x, or null if none 
     */
    public E insert(E x) {
        if (size()<capacity()) {
            //a) Size less than capacity.  Just add x.
            boolean added=tree.add(x);
            assert added;
            return null;
        } else {
            //Ensure size does not exceeed capacity.    
            //Micro-optimizations are possible.
            E smallest = tree.first();
            if (tree.comparator().compare(x,smallest)>0) {
                //b) x larger than smallest of this: remove smallest and add x
                tree.remove(smallest);
                boolean added=tree.add(x);
                assert added;
                return smallest;
            } else {
                //c) Otherwise do nothing.
                return x;
            }
        }
    }
    
    /**
     * Returns the highest priority element of this.
     * @exception NoSuchElementException this.size()==0
     */
    public E getMax() throws NoSuchElementException {
        return tree.last();
    }

   /**
     * Returns the lowest priority element of this.
     * @exception NoSuchElementException this.size()==0
     */
    public E getMin() throws NoSuchElementException {
        return tree.first();
    }

    /** 
     * Returns true if this contains o.  Runs in O(N) time, where N is
     * number of elements in this.
     *
     * @param true this contains a x s.t. o.equals(x).  Note that
     *  priority is ignored in this operation.
     */
    public boolean contains(Object o) {
        return tree.contains(o);
    }

    /** 
     * Removes the first occurence of  o.  Runs in O(N) time, where N is
     * number of elements in this.
     *
     * @param true this contained an x s.t. o.equals(x).  Note that
     *  priority is ignored in this operation.
     */
    public boolean remove(Object o) {
        //You can't just look up o in tree, as tree is sorted by priority, which
        //isn't necessarily consistent with equals.
        for (Iterator<E> iter=tree.iterator(); iter.hasNext(); ) {
            if (o.equals(iter.next())) {
                iter.remove();
                return true;
            }
        }
        return false;
    }

    /** 
     * Returns an iterator of the elements in this, from <b>worst to best</b>.
     */
    public Iterator<E> iterator() {
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
