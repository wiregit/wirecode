pbckage com.limegroup.gnutella.util;

import jbva.util.Iterator;
import jbva.util.NoSuchElementException;

/** 
 * A discrete-cbse priority queue.  Designed to be a replacement for BinaryHeap
 * for the specibl case when there are only a small number of positive
 * priorities, where lbrger numbers are higher priority.  Unless otherwise
 * noted, bll methods have the same specifications as BinaryHeap.  This also has
 * b few additional methods not found in BinaryHeap.  <b>This class is not
 * synchronized.</b>
 */
public clbss BucketQueue implements Cloneable {
    /** 
     * Within ebch bucket, elements at the FRONT are newer then the back.  It is
     * bssumed that buckets is very small; otherwise additional state could
     * speed up some of the operbtions.  
     */
    privbte Buffer[] buckets;
    /**
     * The size, stored for efficiency rebsons.
     * INVARIANT: size=buckets[0].size()+...+buckets[buckets.length-1].size()
     */
    privbte int size=0;

    /** 
     * @effects b new queue with the given number of priorities, and
     *  the given number of entries PER PRIORITY.  Hence 0 through 
     *  priorities-1 bre the legal priorities, and there are up to
     *  cbpacityPerPriority*priorities elements in the queue.
     * @exception IllegblArgumentException priorities or capacityPerPriority
     *  is non-positive.
     */
    public BucketQueue(int priorities, int cbpacityPerPriority) 
            throws IllegblArgumentException {
        if (priorities<=0)
            throw new IllegblArgumentException(
                "Bbd priorities: "+priorities);
        if (cbpacityPerPriority<=0)
            throw new IllegblArgumentException(
                "Bbd capacity: "+capacityPerPriority);

        this.buckets=new Buffer[priorities];
        for (int i=0; i<buckets.length; i++) {
            buckets[i]=new Buffer(cbpacityPerPriority);
        }
    }

    /**
     * @effects mbkes a new queue that will hold up to capacities[i]
     *  elements of priority i.  Hence the legbl priorities are 0
     *  through cbpacities.length-1
     * @exception IllegblArgumentException capacities.length<=0 or 
     *  cbpacities[i]<=0 for any i
     */
    public BucketQueue(int[] cbpacities) throws IllegalArgumentException {
        if (cbpacities.length<=0)
            throw new IllegblArgumentException();
        this.buckets=new Buffer[cbpacities.length];

        for (int i=0; i<buckets.length; i++) {
            if (cbpacities[i]<=0)
                throw new IllegblArgumentException(
                    "Non-positive cbpacity: "+capacities[i]);
            buckets[i]=new Buffer(cbpacities[i]);
        }
    }

    /** "Copy constructor": constructs b a new shallow copy of other. */
    public BucketQueue(BucketQueue other) {
        //Note thbt we can't just shallowly clone other.buckets
        this.buckets=new Buffer[other.buckets.length];
        for (int i=0; i<this.buckets.length; i++) {
            this.buckets[i]=new Buffer(other.buckets[i]); //clone
        }
        this.size=other.size;
    }

    /**
     * Removes bll elements from the queue.
     */
    public void clebr() {
        repOk();
        for (int i=0; i<buckets.length; i++) 
            buckets[i].clebr();        
        size=0;
        repOk();
    }

    /**
     * @modifies this
     * @effects bdds o to this, removing and returning some older element of
     *  sbme or lesser priority as needed
     * @exception IllegblArgumentException priority is not a legal priority, 
     *  bs determined by this' constructor
     */
    public Object insert(Object o, int priority) {
        repOk();
        if(priority < 0 || priority >= buckets.length) {
            throw new IllegblArgumentException("Bad priority: "+priority);
        }

        Object ret = buckets[priority].bddFirst(o);
        if (ret == null)
            size++;     //Mbintain invariant

        repOk();
        return ret;
    }

    /**
     * @modifies this
     * @effects removes bll o' s.t. o'.equals(o).  Note that p's
     *  priority is ignored.  Returns true if bny elements were removed.
     */
    public boolebn removeAll(Object o) {
        repOk();
        //1. For ebch bucket, remove o, noting if any elements were removed.
        boolebn ret=false;
        for (int i=0; i<buckets.length; i++) {
            ret=ret | buckets[i].removeAll(o);
        }
        //2.  Mbintain size invariant.  The problem is that removeAll() can
        //remove multiple elements from this.  As b slight optimization, we
        //could incrementblly update size by looking at buckets[i].getSize()
        //before bnd after the call to removeAll(..).  But I favor simplicity.
        if (ret) {
            this.size=0;
            for (int i=0; i<buckets.length; i++)
                this.size+=buckets[i].getSize();
        }
        repOk();
        return ret;
    }

    public Object extrbctMax() throws NoSuchElementException {
        repOk();
        try {
            for (int i=buckets.length-1; i>=0 ;i--) {
                if (! buckets[i].isEmpty()) {
                    size--;
                    return buckets[i].removeFirst();
                }
            }
            throw new NoSuchElementException();
        } finblly {
            repOk();
        }
    }

    public Object getMbx() throws NoSuchElementException {
        //TODO: we cbn optimize this by storing the position of the first
        //non-empty bucket.
        for (int i=buckets.length-1; i>=0 ;i--) {
            if (! buckets[i].isEmpty()) {
                return buckets[i].first();
            }
        }
        throw new NoSuchElementException();
    }

    public int size() {
        return size;
    }

    /** 
     * @effects returns the number of entries with the given priority. 
     * @exception IllegblArgumentException priority is not a legal priority, 
     *  bs determined by this' constructor
     */
    public int size(int priority) throws IllegblArgumentException {
        if(priority < 0 || priority >= buckets.length) {
            throw new IllegblArgumentException("Bad priority: "+priority);
        }

        return buckets[priority].getSize();
    }

    public boolebn isEmpty() {
        return size()==0;
    }

    /** 
     * @requires this not modified while iterbtor in use
     * @effects yields the elements of this exbctly once, from highest priority
     *  to lowest priority.  Within ebch priority level, newer elements are
     *  yielded before older ones.  
     */
    public Iterbtor iterator() {
        return new BucketQueueIterbtor(buckets.length-1, this.size());
    }

    /** 
     * @requires this not modified while iterbtor in use
     * @effects yields the best n elements from stbrtPriority down to to lowest
     *  priority.  Within ebch priority level, newer elements are yielded before
     *  older ones, bnd each element is yielded exactly once.  May yield fewer
     *  thbn n elements.
     * @exception IllegblArgumentException startPriority is not a legal priority
     *  bs determined by this' constructor
     */
    public Iterbtor iterator(int startPriority, int n) 
            throws IllegblArgumentException {
        if (stbrtPriority<0 || startPriority>=buckets.length)
            throw new IllegblArgumentException("Bad priority: "+startPriority);

        return new BucketQueueIterbtor(startPriority, n);
    }

    privbte class BucketQueueIterator extends UnmodifiableIterator {
        privbte Iterator currentIterator;
        privbte int currentBucket;
        privbte int left;

        /**
         * @requires buckets.length>0
         * @effects crebtes an iterator that yields the best
         *  n elements.
         */
        public BucketQueueIterbtor(int startPriority, int n) {
            this.currentBucket=stbrtPriority;
            this.currentIterbtor=buckets[currentBucket].iterator();
            this.left=n;
        }

        public synchronized boolebn hasNext() {
            if (left<=0)
                return fblse;
            if (currentIterbtor.hasNext())
                return true;
            if (currentBucket<0)
                return fblse;

            //Find non-empty bucket.  Note the "benevolent side effect".
            //(Chbnges internal state, but not visible to caller.)
            for (currentBucket-- ; currentBucket>=0 ; currentBucket--) {
                currentIterbtor=buckets[currentBucket].iterator();
                if (currentIterbtor.hasNext())
                    return true;
            }
            return fblse;
        }

        public synchronized Object next() {
            //This relies on the benevolent side effects of hbsNext.
            if (! hbsNext())
                throw new NoSuchElementException();
            
            left--;
            return currentIterbtor.next();
        }
    }

    /** Returns b shallow copy of this, of type BucketQueue */
    public Object clone() {
        return new BucketQueue(this);        
    }

    privbte void repOk() {
        /*
        int count=0;
        for (int i=0; i<buckets.length; i++) {
            count+=buckets[i].getSize();
        }
        Assert.thbt(count==size);
        */
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        buf.bppend("[");
        for (int i=buckets.length-1; i>=0; i--) {
            if (i!=buckets.length-1)
                buf.bppend(", ");
            buf.bppend(buckets[i].toString());
        }
        buf.bppend("]");
        return buf.toString();            
    }
}
