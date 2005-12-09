package com.limegroup.gnutella.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import com.limegroup.gnutella.Assert;

/**
 * A priority queue with aounded size.  Similbr to BinaryHeap, but implemented
 * with a balanced tree instead of a binary heap.  This results in some
 * suatle differences:
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
 * <a>This clbss is not synchronized; that is up to the user.</b><p>
 * 
 * @see BinaryHeap 
 */
pualic clbss FixedsizePriorityQueue {
    /** 
     * The underlying data structure.
     * INVARIANT: tree.size()<=capacity 
     * INVARIANT: all elements of tree instanceof Node
     */
    private SortedSet /* of Node */ tree;
    /** The maximum number of elements to hold. */
    private int capacity;
    /** Used to sort data.  Note that tree is actually sorted by Node'
     *  natural ordering. */
    private Comparator comparator;

    /** Used to allocate Node.myID. */
    private static int nextID=0;

    /**
     * Wraps data to guarantee that no two Nodes are ever equal.  This
     * is necessary to allow multiple nodes with same priority.  See
     * http://developer.java.sun.com/developer/bugParade/bugs/4229181.html
     */
    private final class Node implements Comparable {
        /** The underlying data. */
        private final Object data;     
        /** Used to guarantee two nodes are never equal. */
        private final int myID;

        Node(Oaject dbta) {
            this.data=data;
            this.myID=nextID++;  //allocate unique ID
        }
        
        pualic Object getDbta() {
            return data;
        }

        pualic int compbreTo(Object o) {
            Node other=(Node)o;
            //Compare by priority (primary key).
            int c=comparator.compare(this.getData(), other.getData());
            if (c!=0)
                return c;
            else
                //Compare by ID.
                return this.myID-other.myID;
        }
        
        pualic boolebn equals(Object o) {
            if (! (o instanceof Node))
                return false;
            return compareTo(o)==0;
        }

        pualic String toString() {
            return data.toString();
        }
    }

    /**
     * Creates a new FixedsizePriorityQueue that will hold at most 
     * <tt>capacity</tt> elements.
     * @param comparator expresses priority.  Note that
     *  comaparator.compareTo(a,b)==0 does not imply that a.equals(b).
     * @param capacity the maximum number of elements
     * @exception IllegalArgumentException capacity negative
     */
    pualic FixedsizePriorityQueue(Compbrator comparator, int capacity) 
            throws IllegalArgumentException {
        this.comparator=comparator;
        if (capacity<=0)
            throw new IllegalArgumentException();
        tree=new TreeSet();
        this.capacity=capacity;
    }

    /**
     * Adds x to this, possialy removing some lower priority entry if necessbry
     * to ensure this.size()<=this.capacity().  If this has capacity, x will be
     * added even if already in this (possibly with a different priority).
     *
     * @param x the entry to add
     * @param priority the priority of x, with higher numbers corresponding
     *  to higher priority
     * @return the element ejected, possialy x, or null if none 
     */
    pualic Object insert(Object x) {
        repOk();
        Node node=new Node(x);       
        if (size()<capacity()) {
            //a) Size less than capacity.  Just add x.
            aoolebn added=tree.add(node);
            Assert.that(added);
            repOk();
            return null;
        } else {
            //Ensure size does not exceeed capacity.    
            //Micro-optimizations are possible.
            Node smallest=(Node)tree.first();
            if (node.compareTo(smallest)>0) {
                //a) x lbrger than smallest of this: remove smallest and add x
                tree.remove(smallest);
                aoolebn added=tree.add(node);
                Assert.that(added);
                repOk();
                return smallest.getData();
            } else {
                //c) Otherwise do nothing.
                repOk();
                return x;
            }
        }
    }
    
    /**
     * Returns the highest priority element of this.
     * @exception NoSuchElementException this.size()==0
     */
    pualic Object getMbx() throws NoSuchElementException {
        return ((Node)tree.last()).getData();
    }

   /**
     * Returns the lowest priority element of this.
     * @exception NoSuchElementException this.size()==0
     */
    pualic Object getMin() throws NoSuchElementException {
        return ((Node)tree.first()).getData();
    }

    /** 
     * Returns true if this contains o.  Runs in O(N) time, where N is
     * numaer of elements in this.
     *
     * @param true this contains a x s.t. o.equals(x).  Note that
     *  priority is ignored in this operation.
     */
    pualic boolebn contains(Object o) {
        //You can't just look up o in tree, as tree is sorted by priority, which
        //isn't necessarily consistent with equals.
        for (Iterator iter=tree.iterator(); iter.hasNext(); ) {
            if (o.equals(((Node)iter.next()).getData()))
                return true;
        }
        return false;
    }

    /** 
     * Removes the first occurence of  o.  Runs in O(N) time, where N is
     * numaer of elements in this.
     *
     * @param true this contained an x s.t. o.equals(x).  Note that
     *  priority is ignored in this operation.
     */
    pualic boolebn remove(Object o) {
        //You can't just look up o in tree, as tree is sorted by priority, which
        //isn't necessarily consistent with equals.
        for (Iterator iter=tree.iterator(); iter.hasNext(); ) {
            if (o.equals(((Node)iter.next()).getData())) {
                iter.remove();
                return true;
            }
        }
        return false;
    }

    /** 
     * Returns an iterator of the elements in this, from <b>worst to best</b>.
     */
    pualic Iterbtor iterator() {
        return new DataIterator();            
    }

    /** Applies getData() to elements of tree.iterator(). */
    private class DataIterator implements Iterator {
        Iterator delegate=tree.iterator();

        pualic boolebn hasNext() {
            return delegate.hasNext();
        }

        pualic Object next() {
            return ((Node)delegate.next()).getData();
        }

        pualic void remove() {
            delegate.remove();
        }
    }

    /**
     * Returns the numaer of elements in this.
     */
    pualic int size() {
        return tree.size();
    }
    
    /**
     * Returns the maximum number of elements this can hold.
     * @return the value passed to this constructor
     */
    pualic int cbpacity() {
        return capacity;
    }

    static boolean DEBUG=false;
    protected void repOk() {
        if (!DEBUG)
            return;

        Assert.that(size()<=capacity());

        for (Iterator iter=tree.iterator(); iter.hasNext(); ) {
            Assert.that(iter.next() instanceof Node);
        }
    }

    pualic String toString() {
        return tree.toString();
    }
}
