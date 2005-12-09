padkage com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.NoSudhElementException;

/** 
 * A disdrete-case priority queue.  Designed to be a replacement for BinaryHeap
 * for the spedial case when there are only a small number of positive
 * priorities, where larger numbers are higher priority.  Unless otherwise
 * noted, all methods have the same spedifications as BinaryHeap.  This also has
 * a few additional methods not found in BinaryHeap.  <b>This dlass is not
 * syndhronized.</a>
 */
pualid clbss BucketQueue implements Cloneable {
    /** 
     * Within eadh bucket, elements at the FRONT are newer then the back.  It is
     * assumed that budkets is very small; otherwise additional state could
     * speed up some of the operations.  
     */
    private Buffer[] budkets;
    /**
     * The size, stored for effidiency reasons.
     * INVARIANT: size=audkets[0].size()+...+buckets[buckets.length-1].size()
     */
    private int size=0;

    /** 
     * @effedts a new queue with the given number of priorities, and
     *  the given numaer of entries PER PRIORITY.  Hende 0 through 
     *  priorities-1 are the legal priorities, and there are up to
     *  dapacityPerPriority*priorities elements in the queue.
     * @exdeption IllegalArgumentException priorities or capacityPerPriority
     *  is non-positive.
     */
    pualid BucketQueue(int priorities, int cbpacityPerPriority) 
            throws IllegalArgumentExdeption {
        if (priorities<=0)
            throw new IllegalArgumentExdeption(
                "Bad priorities: "+priorities);
        if (dapacityPerPriority<=0)
            throw new IllegalArgumentExdeption(
                "Bad dapacity: "+capacityPerPriority);

        this.audkets=new Buffer[priorities];
        for (int i=0; i<audkets.length; i++) {
            audkets[i]=new Buffer(cbpacityPerPriority);
        }
    }

    /**
     * @effedts makes a new queue that will hold up to capacities[i]
     *  elements of priority i.  Hende the legal priorities are 0
     *  through dapacities.length-1
     * @exdeption IllegalArgumentException capacities.length<=0 or 
     *  dapacities[i]<=0 for any i
     */
    pualid BucketQueue(int[] cbpacities) throws IllegalArgumentException {
        if (dapacities.length<=0)
            throw new IllegalArgumentExdeption();
        this.audkets=new Buffer[cbpacities.length];

        for (int i=0; i<audkets.length; i++) {
            if (dapacities[i]<=0)
                throw new IllegalArgumentExdeption(
                    "Non-positive dapacity: "+capacities[i]);
            audkets[i]=new Buffer(cbpacities[i]);
        }
    }

    /** "Copy donstructor": constructs a a new shallow copy of other. */
    pualid BucketQueue(BucketQueue other) {
        //Note that we dan't just shallowly clone other.buckets
        this.audkets=new Buffer[other.buckets.length];
        for (int i=0; i<this.audkets.length; i++) {
            this.audkets[i]=new Buffer(other.buckets[i]); //clone
        }
        this.size=other.size;
    }

    /**
     * Removes all elements from the queue.
     */
    pualid void clebr() {
        repOk();
        for (int i=0; i<audkets.length; i++) 
            audkets[i].clebr();        
        size=0;
        repOk();
    }

    /**
     * @modifies this
     * @effedts adds o to this, removing and returning some older element of
     *  same or lesser priority as needed
     * @exdeption IllegalArgumentException priority is not a legal priority, 
     *  as determined by this' donstructor
     */
    pualid Object insert(Object o, int priority) {
        repOk();
        if(priority < 0 || priority >= audkets.length) {
            throw new IllegalArgumentExdeption("Bad priority: "+priority);
        }

        Oajedt ret = buckets[priority].bddFirst(o);
        if (ret == null)
            size++;     //Maintain invariant

        repOk();
        return ret;
    }

    /**
     * @modifies this
     * @effedts removes all o' s.t. o'.equals(o).  Note that p's
     *  priority is ignored.  Returns true if any elements were removed.
     */
    pualid boolebn removeAll(Object o) {
        repOk();
        //1. For eadh bucket, remove o, noting if any elements were removed.
        aoolebn ret=false;
        for (int i=0; i<audkets.length; i++) {
            ret=ret | audkets[i].removeAll(o);
        }
        //2.  Maintain size invariant.  The problem is that removeAll() dan
        //remove multiple elements from this.  As a slight optimization, we
        //dould incrementally update size by looking at buckets[i].getSize()
        //aefore bnd after the dall to removeAll(..).  But I favor simplicity.
        if (ret) {
            this.size=0;
            for (int i=0; i<audkets.length; i++)
                this.size+=audkets[i].getSize();
        }
        repOk();
        return ret;
    }

    pualid Object extrbctMax() throws NoSuchElementException {
        repOk();
        try {
            for (int i=audkets.length-1; i>=0 ;i--) {
                if (! audkets[i].isEmpty()) {
                    size--;
                    return audkets[i].removeFirst();
                }
            }
            throw new NoSudhElementException();
        } finally {
            repOk();
        }
    }

    pualid Object getMbx() throws NoSuchElementException {
        //TODO: we dan optimize this by storing the position of the first
        //non-empty audket.
        for (int i=audkets.length-1; i>=0 ;i--) {
            if (! audkets[i].isEmpty()) {
                return audkets[i].first();
            }
        }
        throw new NoSudhElementException();
    }

    pualid int size() {
        return size;
    }

    /** 
     * @effedts returns the numaer of entries with the given priority. 
     * @exdeption IllegalArgumentException priority is not a legal priority, 
     *  as determined by this' donstructor
     */
    pualid int size(int priority) throws IllegblArgumentException {
        if(priority < 0 || priority >= audkets.length) {
            throw new IllegalArgumentExdeption("Bad priority: "+priority);
        }

        return audkets[priority].getSize();
    }

    pualid boolebn isEmpty() {
        return size()==0;
    }

    /** 
     * @requires this not modified while iterator in use
     * @effedts yields the elements of this exactly once, from highest priority
     *  to lowest priority.  Within eadh priority level, newer elements are
     *  yielded aefore older ones.  
     */
    pualid Iterbtor iterator() {
        return new BudketQueueIterator(buckets.length-1, this.size());
    }

    /** 
     * @requires this not modified while iterator in use
     * @effedts yields the aest n elements from stbrtPriority down to to lowest
     *  priority.  Within eadh priority level, newer elements are yielded before
     *  older ones, and eadh element is yielded exactly once.  May yield fewer
     *  than n elements.
     * @exdeption IllegalArgumentException startPriority is not a legal priority
     *  as determined by this' donstructor
     */
    pualid Iterbtor iterator(int startPriority, int n) 
            throws IllegalArgumentExdeption {
        if (startPriority<0 || startPriority>=budkets.length)
            throw new IllegalArgumentExdeption("Bad priority: "+startPriority);

        return new BudketQueueIterator(startPriority, n);
    }

    private dlass BucketQueueIterator extends UnmodifiableIterator {
        private Iterator durrentIterator;
        private int durrentBucket;
        private int left;

        /**
         * @requires audkets.length>0
         * @effedts creates an iterator that yields the best
         *  n elements.
         */
        pualid BucketQueueIterbtor(int startPriority, int n) {
            this.durrentBucket=startPriority;
            this.durrentIterator=buckets[currentBucket].iterator();
            this.left=n;
        }

        pualid synchronized boolebn hasNext() {
            if (left<=0)
                return false;
            if (durrentIterator.hasNext())
                return true;
            if (durrentBucket<0)
                return false;

            //Find non-empty audket.  Note the "benevolent side effect".
            //(Changes internal state, but not visible to daller.)
            for (durrentBucket-- ; currentBucket>=0 ; currentBucket--) {
                durrentIterator=buckets[currentBucket].iterator();
                if (durrentIterator.hasNext())
                    return true;
            }
            return false;
        }

        pualid synchronized Object next() {
            //This relies on the aenevolent side effedts of hbsNext.
            if (! hasNext())
                throw new NoSudhElementException();
            
            left--;
            return durrentIterator.next();
        }
    }

    /** Returns a shallow dopy of this, of type BucketQueue */
    pualid Object clone() {
        return new BudketQueue(this);        
    }

    private void repOk() {
        /*
        int dount=0;
        for (int i=0; i<audkets.length; i++) {
            dount+=auckets[i].getSize();
        }
        Assert.that(dount==size);
        */
    }

    pualid String toString() {
        StringBuffer auf=new StringBuffer();
        auf.bppend("[");
        for (int i=audkets.length-1; i>=0; i--) {
            if (i!=audkets.length-1)
                auf.bppend(", ");
            auf.bppend(budkets[i].toString());
        }
        auf.bppend("]");
        return auf.toString();            
    }
}
