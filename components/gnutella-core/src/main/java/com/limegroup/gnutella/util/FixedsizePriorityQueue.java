
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ExtendedEndpoint;

/**
 * A FixedsizePriorityQueue is a list of objects in priority order that discards the worst object when it overflows.
 * In LimeWire, only HostCatcher uses FixedsizePriorityQueue, and keeps ExtendedEndpoint objects in it.
 * 
 * When you make a FixedsizePriorityQueue, you specify how many objects it will be able to hold.
 * You also specify a method that can determine which of two objects is the best.
 * 
 * The FixedsizePriorityQueue will keep the objects sorted in priority order with the worst first and the best last.
 * When you add a new one, the FixedsizePriorityQueue will put it in the correct spot.
 * If the list was full, the worst one gets discarded.
 * 
 * This class is not synchronized.
 * 
 * Similar to BinaryHeap, but implemented with a balanced tree instead of a binary heap.
 * This results in some subtle differences:
 * 
 * FixedsizePriorityQueue guarantees that the lowest priority element is ejected when exceeding capacity.
 * BinaryHeap provides no such guarantees.
 * 
 * Fetching the max element takes O(lg N) time, where N is the number of elements.
 * Compare with O(1) for BinaryHeap.
 * Extracting and adding elements is still O(lg N) time.
 * 
 * FixedsizePriorityQueue can provide operations for extracting the minimum and the maximum element.
 * Note, however, that this is still considered a max heap, for reasons in the first listed item.
 * 
 * FixedsizePriorityQueue REQUIRES an explicit Comparator.
 * It won't use the natural ordering of values.
 */
public class FixedsizePriorityQueue {

    /**
     * The data structure this FixedsizePriorityQueue holds its elements in.
     * The constructor makes tree a new TreeSet object, and then this member variable looks at it as though it were just a SortedSet.
     * 
     * A java.util.SortedSet is a list of elements that stays sorted.
     * As you iterate through its elements, they will be in ascending element order.
     * Their order is defined by the comparator we'll provide when we create the set.
     * We'll provide a comparator that prioritizes them, so the best elements will be first.
     * 
     * Calling tree.size() will always return a number up to capacity.
     * All the elements inside tree are objects of type Node.
     * 
     * In the tree, the elements are ordered smallest to largest.
     * When we're using priority, their order is worst to best.
     */
    private SortedSet tree; // This is actually a TreeSet, and we're going to put Node objects in it

    /** The maximum number of elements to hold. */
    private int capacity;

    /**
     * The method we'll call to compare two objects in our list.
     * This method will compare their priority, and tell us which to keep and which to throw out.
     * When the list is full, we'll keep only the best items.
     * 
     * FixedsizePriorityQueue is actually only used by HostCatcher in LimeWire.
     * The objects we're keeping it it are ExtendedEndpoint objects.
     * The comparator is returned by ExtendedEndpoint.priorityComparator().
     * The call comparator.compare(a, b) goes into a method in a nested class:
     * ExtendedEndpoint.PriorityComparator.compare(a, b).
     */
    private Comparator comparator;

    /** The first Node we make will have a myID of 0, the next one 1, and so on. */
    private static int nextID = 0;

    /**
     * Wraps the objects we put in the list, making sure compareTo(o) never returns 0.
     * 
     * We don't put the given objects in the tree TreeSet list directly.
     * Instead, we wrap them in Node objects, and put the Node objects in the TreeSet.
     * When the Node constructor makes a new Node object, it gives it the next ID number.
     * Each Node object contains a unique ID number.
     * 
     * This makes sure no to Nodes are ever equal.
     * This is necessary to allow multiple nodes to have the same priority.
     * This is a solution to the problem described here:
     * http://developer.java.sun.com/developer/bugParade/bugs/4229181.html
     * 
     * This class implements the Comparable interface, which requires the compareTo(o) method.
     */
    private final class Node implements Comparable {

        /** The object we'll keep inside this Node object. */
        private final Object data;

        /**
         * Each Node has a unique ID.
         * This makes sure that no two Node objects are the same.
         * Even if you add the same object twice, their ID numbers will be different.
         */
        private final int myID;

        /**
         * Make a new Node with the given object and the next ID number.
         * 
         * @param data The object we'll put inside it
         */
        Node(Object data) {

            // Save the object
            this.data = data;

            // Get the next ID number, and save it in this new Node object
            this.myID = nextID++; // Each Node object will have a different number
        }

        /**
         * Return the object stored inside this Node.
         * You provided this object to the Node constructor when you made it.
         * 
         * @return The object stored in this Node
         */
        public Object getData() {

            // Return the saved object
            return data;
        }

        /**
         * Compare this object to a given one to see which we should keep.
         * Never returns 0, always chooses this one or the other.
         * 
         * @param o The object to compare this one to
         * @return  A negative number if the given object is the best.
         *          A positive number if this one is the best.
         */
        public int compareTo(Object o) {

            /*
             * FixedsizePriorityQueue is actually only used by HostCatcher in LimeWire.
             * The objects we're keeping it it are ExtendedEndpoint objects.
             * The comparator is ExtendedEndpoint.priorityComparator().
             * The call comparator.compare(a, b) goes into a method in a nested class:
             * ExtendedEndpoint.PriorityComparator.compare(a, b).
             */

            // Compare by priority
            Node other = (Node)o;
            int c = comparator.compare(this.getData(), other.getData());
            if (c != 0) return c; // A positive number if this one is the best

            // Same priority, compare by ID number to break the tie
            return this.myID - other.myID;
        }

        /**
         * Determine if a given Node object is the same as this one.
         * Always returns false, no two Node objects are the same because they all have unique IDs.
         * 
         * @param o An object to compare this one to
         * @return  False, no two Node objects are the same
         */
        public boolean equals(Object o) {

            // If the given object isn't a node, it's not the same as this one
            if (!(o instanceof Node)) return false;

            // Returns false, compareTo() never returns 0
            return compareTo(o) == 0;
        }

        /**
         * Express this Node object as text.
         * 
         * @return A String
         */
        public String toString() {

            // Have the stored object do this
            return data.toString();
        }
    }

    /**
     * Make a new FixedsizePriorityQueue that will hold capacity elements at most.
     * 
     * @param comparator We'll call comparator.compare(a, b) to see which we should keep.
     *                   If compare(a, b) returns 0, that means they're equally good.
     *                   It does not mean they are the same.
     * @param capacity   The number of elements this new FixedsizePriorityQueue will hold.
     *                   If it's full and we add another, it will throw out the lowest priority element.
     */
    public FixedsizePriorityQueue(Comparator comparator, int capacity) throws IllegalArgumentException {

        // Save a reference to the given comparator object so we can use it later
        this.comparator = comparator;

        // Make sure the given capacity isn't 0 or negative
        if (capacity <= 0) throw new IllegalArgumentException();

        // Make a new empty TreeSet, a list of elements that it will keep sorted
        tree = new TreeSet();

        // Save the given capacity in this object, we'll enforce it ourselves
        this.capacity = capacity;
    }

    /**
     * Add an object to this FixedsizePriorityQueue in priority order, pushing the lowest priority one out if full.
     * Duplicates are allowed.
     * If you add the same object twice, you will get 2 of them in the list.
     * 
     * @param x The object to insert
     * @return  The low-priority object we ejected if the list was full.
     *          If the list isn't full, returns null.
     */
    public Object insert(Object x) {

        // Make sure the tree hasn't grown beyond its capacity, and there are only Node objects in it
        repOk();

        // Wrap the given object in a new Node, which will make sure it's unique in the list
        Node node = new Node(x); // Two objects might be the same, but wrapped in Nodes, they're all unique

        // The tree has room for another element
        if (size() < capacity()) {

            // Add the wrapped object to the TreeSet
            boolean added = tree.add(node);
            Assert.that(added); // Make sure add() returned true, meaning it added it
            repOk();

            // No element ejected
            return null;
            
        // The tree is full
        } else {

            // Get the first element in the tree, which is the smallest
            Node smallest = (Node)tree.first(); // You can also think of it as the worst

            // The new node is bigger or better than the first smallest worst one, it makes it into the list
            if (node.compareTo(smallest) > 0) {

                // Remove the first, smallest, worst item from the list
                tree.remove(smallest);
                
                // Add the given one
                boolean added = tree.add(node); // It will get added in sorted order
                Assert.that(added);
                repOk();
                
                // Return the item we removed
                return smallest.getData(); // Get the object we wrapped in a Node

            // The new node is worst that the worst one in the list
            } else {

                // Return it, this is sort of like it knocking itself out of the list
                return x;
            }
        }
    }

    /**
     * Get the highest priority element stored in this list.
     * 
     * @exception NoSuchElementException If the list is empty
     */
    public Object getMax() throws NoSuchElementException {

        // The last element is the greatest, biggest and best
        return ((Node)tree.last()).getData();
    }

    /**
     * Get the lowest priority element stored in this list.
     * 
     * @exception NoSuchElementException If the list is empty
     */
    public Object getMin() throws NoSuchElementException {

        // The first element is the least, smallest and worst
        return ((Node)tree.first()).getData();
    }

    /**
     * Determine if this list contains an object or not.
     * This operation doesn't use priority at all.
     * 
     * This may be slow.
     * It runs in O(N) time, where N is the number of elements in the list.
     * 
     * @return True if this list contains o, false if it's not here
     */
    public boolean contains(Object o) {

        /*
         * You can't just look up o in tree.
         * Tree is sorted by priority, which isn't consistant with equals.
         */

        // Loop through all the elements in the list
        for (Iterator iter = tree.iterator(); iter.hasNext(); ) {

            // Found it
            if (o.equals(((Node)iter.next()).getData())) return true;
        }

        // Not found
        return false;
    }

    /**
     * Removes the first occurance of a given object.
     * Since this FixedsizePriorityQueue can contain duplicates, finding and removeing one does not mean you've removed them all.
     * This operation doesn't use priority at all.
     * 
     * This may be slow.
     * It runs in O(N) time, where N is the number of elements in the list.
     * 
     * @return True if we found it and remove it, false if not found
     */
    public boolean remove(Object o) {

        /*
         * You can't just look up o in tree.
         * Tree is sorted by priority, which isn't consistant with equals.
         */

        // Loop through all the elements in the list
        for (Iterator iter=tree.iterator(); iter.hasNext(); ) {

            // Found it
            if (o.equals(((Node)iter.next()).getData())) {

                // Remove it and return true
                iter.remove();
                return true;
            }
        }

        // Not found, return false
        return false;
    }

    /**
     * Returns an iterator of the elements in this, from <b>worst to best</b>.
     */
    public Iterator iterator() {

        return new DataIterator();            
    }

    /**
     * A DataIterator object keeps a iterator, and lets you loop down the items in the tree list.
     * 
     * The list in a FixedsizePriorityQueue doesn't hold the given objects directly.
     * Instead, it wraps each one in a unique Node object, and stores the Node objects.
     * Iterating through them, it would be inconvenient to have to unwrap them.
     * This nested DataIterator class does the unwrapping for you.
     */
    private class DataIterator implements Iterator {

        /** Get an Iterator from the tree that will remember which element it's on. */
        Iterator delegate = tree.iterator();

        /**
         * Determine if calling next() will return an element or throw an exception.
         * 
         * @return True if the iteration has more elements
         */
        public boolean hasNext() {

            // Have the tree iterator do this
            return delegate.hasNext();
        }

        /**
         * Get the element at the iterator's current position, and move the iterator forward to the next one.
         * 
         * @return The object we wrapped in a Node before storing in the list
         */
        public Object next() {

            // Get the object from the list, look at it as a Node, and get the object wrapped inside it
            return ((Node)delegate.next()).getData();
        }

        /**
         * Remove the last element the iterator returned.
         */
        public void remove() {

            // Have the tree iterator do this
            delegate.remove();
        }
    }

    /**
     * The number of elements in this object.
     * Calls tree.size(), tree is the TreeSet that lists the elements.
     * 
     * @return tree.size()
     * Returns the number of elements in this.
     */
    public int size() {

        // Ask the TreeSet for the number of elements it's holding
        return tree.size();
    }

    /**
     * The maximum number of elements this object can hold.
     * You set this value when you made this FixedsizePriorityQueue.
     * The add() method makes sure this object doesn't go over this capacity.
     * 
     * @return The capacity
     */
    public int capacity() {

        // Return the capacity we're keeping ourselves within
        return capacity;
    }

    /** False, don't do the extra debugging checks in repOk() */
    static boolean DEBUG = false;

    /** Make sure the tree hasn't grown beyond its capacity, and only Node objects are in it. */
    protected void repOk() {

        // Debugging is off, leave now
        if (!DEBUG) return;

        // Make sure the tree hasn't grown larger than its capacity
        Assert.that(size() <= capacity());

        // Loop through all the items in the tree, making sure each is of type Node
        for (Iterator iter = tree.iterator(); iter.hasNext(); ) {

            // The call iter.next() gets the current item and moves the iterator to the next one
            Assert.that(iter.next() instanceof Node);
        }
    }

    /**
     * Express this object as text.
     * Calls tree.toString(), which calls TreeSet.toString().
     * 
     * @return The String from the TreeSet this object uses to hold its elements
     */
    public String toString() {

        // Have the TreeSet provide the String
        return tree.toString();
    }
}
