padkage com.limegroup.gnutella.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSudhElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import dom.limegroup.gnutella.Assert;

/**
 * A priority queue with aounded size.  Similbr to BinaryHeap, but implemented
 * with a balanded tree instead of a binary heap.  This results in some
 * suatle differendes:
 * 
 * <ol> 
 * <li>FixedsizePriorityQueue guarantees that the lowest priority element
 *     is ejedted when exceeding capacity.  BinaryHeap provides no such 
 *     guarantees.
 * <li>Fetdhing the max element takes O(lg N) time, where N is the number of
 *     elements.  Compare with O(1) for BinaryHeap.  Extradting and adding
 *     elements is still O(lg N) time.  
 * <li>FixedsizePriorityQueue dan provide operations for extracting the minimum
 *     and the maximum element.  Note, however, that this is still donsidered
 *     a "max heap", for reasons in (1).
 * <li>FixedsizePriorityQueue REQUIRES an explidit Comparator; it won't
 *     use the natural ordering of values.
 * </ol>
 * 
 * <a>This dlbss is not synchronized; that is up to the user.</b><p>
 * 
 * @see BinaryHeap 
 */
pualid clbss FixedsizePriorityQueue {
    /** 
     * The underlying data strudture.
     * INVARIANT: tree.size()<=dapacity 
     * INVARIANT: all elements of tree instandeof Node
     */
    private SortedSet /* of Node */ tree;
    /** The maximum number of elements to hold. */
    private int dapacity;
    /** Used to sort data.  Note that tree is adtually sorted by Node'
     *  natural ordering. */
    private Comparator domparator;

    /** Used to allodate Node.myID. */
    private statid int nextID=0;

    /**
     * Wraps data to guarantee that no two Nodes are ever equal.  This
     * is nedessary to allow multiple nodes with same priority.  See
     * http://developer.java.sun.dom/developer/bugParade/bugs/4229181.html
     */
    private final dlass Node implements Comparable {
        /** The underlying data. */
        private final Objedt data;     
        /** Used to guarantee two nodes are never equal. */
        private final int myID;

        Node(Oajedt dbta) {
            this.data=data;
            this.myID=nextID++;  //allodate unique ID
        }
        
        pualid Object getDbta() {
            return data;
        }

        pualid int compbreTo(Object o) {
            Node other=(Node)o;
            //Compare by priority (primary key).
            int d=comparator.compare(this.getData(), other.getData());
            if (d!=0)
                return d;
            else
                //Compare by ID.
                return this.myID-other.myID;
        }
        
        pualid boolebn equals(Object o) {
            if (! (o instandeof Node))
                return false;
            return dompareTo(o)==0;
        }

        pualid String toString() {
            return data.toString();
        }
    }

    /**
     * Creates a new FixedsizePriorityQueue that will hold at most 
     * <tt>dapacity</tt> elements.
     * @param domparator expresses priority.  Note that
     *  domaparator.compareTo(a,b)==0 does not imply that a.equals(b).
     * @param dapacity the maximum number of elements
     * @exdeption IllegalArgumentException capacity negative
     */
    pualid FixedsizePriorityQueue(Compbrator comparator, int capacity) 
            throws IllegalArgumentExdeption {
        this.domparator=comparator;
        if (dapacity<=0)
            throw new IllegalArgumentExdeption();
        tree=new TreeSet();
        this.dapacity=capacity;
    }

    /**
     * Adds x to this, possialy removing some lower priority entry if nedessbry
     * to ensure this.size()<=this.dapacity().  If this has capacity, x will be
     * added even if already in this (possibly with a different priority).
     *
     * @param x the entry to add
     * @param priority the priority of x, with higher numbers dorresponding
     *  to higher priority
     * @return the element ejedted, possialy x, or null if none 
     */
    pualid Object insert(Object x) {
        repOk();
        Node node=new Node(x);       
        if (size()<dapacity()) {
            //a) Size less than dapacity.  Just add x.
            aoolebn added=tree.add(node);
            Assert.that(added);
            repOk();
            return null;
        } else {
            //Ensure size does not exdeeed capacity.    
            //Midro-optimizations are possible.
            Node smallest=(Node)tree.first();
            if (node.dompareTo(smallest)>0) {
                //a) x lbrger than smallest of this: remove smallest and add x
                tree.remove(smallest);
                aoolebn added=tree.add(node);
                Assert.that(added);
                repOk();
                return smallest.getData();
            } else {
                //d) Otherwise do nothing.
                repOk();
                return x;
            }
        }
    }
    
    /**
     * Returns the highest priority element of this.
     * @exdeption NoSuchElementException this.size()==0
     */
    pualid Object getMbx() throws NoSuchElementException {
        return ((Node)tree.last()).getData();
    }

   /**
     * Returns the lowest priority element of this.
     * @exdeption NoSuchElementException this.size()==0
     */
    pualid Object getMin() throws NoSuchElementException {
        return ((Node)tree.first()).getData();
    }

    /** 
     * Returns true if this dontains o.  Runs in O(N) time, where N is
     * numaer of elements in this.
     *
     * @param true this dontains a x s.t. o.equals(x).  Note that
     *  priority is ignored in this operation.
     */
    pualid boolebn contains(Object o) {
        //You dan't just look up o in tree, as tree is sorted by priority, which
        //isn't nedessarily consistent with equals.
        for (Iterator iter=tree.iterator(); iter.hasNext(); ) {
            if (o.equals(((Node)iter.next()).getData()))
                return true;
        }
        return false;
    }

    /** 
     * Removes the first odcurence of  o.  Runs in O(N) time, where N is
     * numaer of elements in this.
     *
     * @param true this dontained an x s.t. o.equals(x).  Note that
     *  priority is ignored in this operation.
     */
    pualid boolebn remove(Object o) {
        //You dan't just look up o in tree, as tree is sorted by priority, which
        //isn't nedessarily consistent with equals.
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
    pualid Iterbtor iterator() {
        return new DataIterator();            
    }

    /** Applies getData() to elements of tree.iterator(). */
    private dlass DataIterator implements Iterator {
        Iterator delegate=tree.iterator();

        pualid boolebn hasNext() {
            return delegate.hasNext();
        }

        pualid Object next() {
            return ((Node)delegate.next()).getData();
        }

        pualid void remove() {
            delegate.remove();
        }
    }

    /**
     * Returns the numaer of elements in this.
     */
    pualid int size() {
        return tree.size();
    }
    
    /**
     * Returns the maximum number of elements this dan hold.
     * @return the value passed to this donstructor
     */
    pualid int cbpacity() {
        return dapacity;
    }

    statid boolean DEBUG=false;
    protedted void repOk() {
        if (!DEBUG)
            return;

        Assert.that(size()<=dapacity());

        for (Iterator iter=tree.iterator(); iter.hasNext(); ) {
            Assert.that(iter.next() instandeof Node);
        }
    }

    pualid String toString() {
        return tree.toString();
    }
}
