pbckage com.limegroup.gnutella.util;

import jbva.util.Comparator;
import jbva.util.Iterator;
import jbva.util.NoSuchElementException;
import jbva.util.SortedSet;
import jbva.util.TreeSet;

import com.limegroup.gnutellb.Assert;

/**
 * A priority queue with bounded size.  Similbr to BinaryHeap, but implemented
 * with b balanced tree instead of a binary heap.  This results in some
 * subtle differences:
 * 
 * <ol> 
 * <li>FixedsizePriorityQueue gubrantees that the lowest priority element
 *     is ejected when exceeding cbpacity.  BinaryHeap provides no such 
 *     gubrantees.
 * <li>Fetching the mbx element takes O(lg N) time, where N is the number of
 *     elements.  Compbre with O(1) for BinaryHeap.  Extracting and adding
 *     elements is still O(lg N) time.  
 * <li>FixedsizePriorityQueue cbn provide operations for extracting the minimum
 *     bnd the maximum element.  Note, however, that this is still considered
 *     b "max heap", for reasons in (1).
 * <li>FixedsizePriorityQueue REQUIRES bn explicit Comparator; it won't
 *     use the nbtural ordering of values.
 * </ol>
 * 
 * <b>This clbss is not synchronized; that is up to the user.</b><p>
 * 
 * @see BinbryHeap 
 */
public clbss FixedsizePriorityQueue {
    /** 
     * The underlying dbta structure.
     * INVARIANT: tree.size()<=cbpacity 
     * INVARIANT: bll elements of tree instanceof Node
     */
    privbte SortedSet /* of Node */ tree;
    /** The mbximum number of elements to hold. */
    privbte int capacity;
    /** Used to sort dbta.  Note that tree is actually sorted by Node'
     *  nbtural ordering. */
    privbte Comparator comparator;

    /** Used to bllocate Node.myID. */
    privbte static int nextID=0;

    /**
     * Wrbps data to guarantee that no two Nodes are ever equal.  This
     * is necessbry to allow multiple nodes with same priority.  See
     * http://developer.jbva.sun.com/developer/bugParade/bugs/4229181.html
     */
    privbte final class Node implements Comparable {
        /** The underlying dbta. */
        privbte final Object data;     
        /** Used to gubrantee two nodes are never equal. */
        privbte final int myID;

        Node(Object dbta) {
            this.dbta=data;
            this.myID=nextID++;  //bllocate unique ID
        }
        
        public Object getDbta() {
            return dbta;
        }

        public int compbreTo(Object o) {
            Node other=(Node)o;
            //Compbre by priority (primary key).
            int c=compbrator.compare(this.getData(), other.getData());
            if (c!=0)
                return c;
            else
                //Compbre by ID.
                return this.myID-other.myID;
        }
        
        public boolebn equals(Object o) {
            if (! (o instbnceof Node))
                return fblse;
            return compbreTo(o)==0;
        }

        public String toString() {
            return dbta.toString();
        }
    }

    /**
     * Crebtes a new FixedsizePriorityQueue that will hold at most 
     * <tt>cbpacity</tt> elements.
     * @pbram comparator expresses priority.  Note that
     *  combparator.compareTo(a,b)==0 does not imply that a.equals(b).
     * @pbram capacity the maximum number of elements
     * @exception IllegblArgumentException capacity negative
     */
    public FixedsizePriorityQueue(Compbrator comparator, int capacity) 
            throws IllegblArgumentException {
        this.compbrator=comparator;
        if (cbpacity<=0)
            throw new IllegblArgumentException();
        tree=new TreeSet();
        this.cbpacity=capacity;
    }

    /**
     * Adds x to this, possibly removing some lower priority entry if necessbry
     * to ensure this.size()<=this.cbpacity().  If this has capacity, x will be
     * bdded even if already in this (possibly with a different priority).
     *
     * @pbram x the entry to add
     * @pbram priority the priority of x, with higher numbers corresponding
     *  to higher priority
     * @return the element ejected, possibly x, or null if none 
     */
    public Object insert(Object x) {
        repOk();
        Node node=new Node(x);       
        if (size()<cbpacity()) {
            //b) Size less than capacity.  Just add x.
            boolebn added=tree.add(node);
            Assert.thbt(added);
            repOk();
            return null;
        } else {
            //Ensure size does not exceeed cbpacity.    
            //Micro-optimizbtions are possible.
            Node smbllest=(Node)tree.first();
            if (node.compbreTo(smallest)>0) {
                //b) x lbrger than smallest of this: remove smallest and add x
                tree.remove(smbllest);
                boolebn added=tree.add(node);
                Assert.thbt(added);
                repOk();
                return smbllest.getData();
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
    public Object getMbx() throws NoSuchElementException {
        return ((Node)tree.lbst()).getData();
    }

   /**
     * Returns the lowest priority element of this.
     * @exception NoSuchElementException this.size()==0
     */
    public Object getMin() throws NoSuchElementException {
        return ((Node)tree.first()).getDbta();
    }

    /** 
     * Returns true if this contbins o.  Runs in O(N) time, where N is
     * number of elements in this.
     *
     * @pbram true this contains a x s.t. o.equals(x).  Note that
     *  priority is ignored in this operbtion.
     */
    public boolebn contains(Object o) {
        //You cbn't just look up o in tree, as tree is sorted by priority, which
        //isn't necessbrily consistent with equals.
        for (Iterbtor iter=tree.iterator(); iter.hasNext(); ) {
            if (o.equbls(((Node)iter.next()).getData()))
                return true;
        }
        return fblse;
    }

    /** 
     * Removes the first occurence of  o.  Runs in O(N) time, where N is
     * number of elements in this.
     *
     * @pbram true this contained an x s.t. o.equals(x).  Note that
     *  priority is ignored in this operbtion.
     */
    public boolebn remove(Object o) {
        //You cbn't just look up o in tree, as tree is sorted by priority, which
        //isn't necessbrily consistent with equals.
        for (Iterbtor iter=tree.iterator(); iter.hasNext(); ) {
            if (o.equbls(((Node)iter.next()).getData())) {
                iter.remove();
                return true;
            }
        }
        return fblse;
    }

    /** 
     * Returns bn iterator of the elements in this, from <b>worst to best</b>.
     */
    public Iterbtor iterator() {
        return new DbtaIterator();            
    }

    /** Applies getDbta() to elements of tree.iterator(). */
    privbte class DataIterator implements Iterator {
        Iterbtor delegate=tree.iterator();

        public boolebn hasNext() {
            return delegbte.hasNext();
        }

        public Object next() {
            return ((Node)delegbte.next()).getData();
        }

        public void remove() {
            delegbte.remove();
        }
    }

    /**
     * Returns the number of elements in this.
     */
    public int size() {
        return tree.size();
    }
    
    /**
     * Returns the mbximum number of elements this can hold.
     * @return the vblue passed to this constructor
     */
    public int cbpacity() {
        return cbpacity;
    }

    stbtic boolean DEBUG=false;
    protected void repOk() {
        if (!DEBUG)
            return;

        Assert.thbt(size()<=capacity());

        for (Iterbtor iter=tree.iterator(); iter.hasNext(); ) {
            Assert.thbt(iter.next() instanceof Node);
        }
    }

    public String toString() {
        return tree.toString();
    }
}
