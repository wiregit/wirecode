package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** 
 * A discrete-case priority queue.  Designed to be a replacement for BinaryHeap
 * for the special case when there are only a small number of positive
 * priorities, where larger numbers are higher priority.  Unless otherwise
 * noted, all methods have the same specifications as BinaryHeap.  This also has
 * a few additional methods not found in BinaryHeap.  <b>This class is not
 * synchronized.</b>
 */
public class BucketQueue implements Cloneable {
    /** 
     * Within each bucket, elements at the FRONT are newer then the back.  It is
     * assumed that buckets is very small; otherwise additional state could
     * speed up some of the operations.  
     */
    private Buffer[] buckets;
    /**
     * The size, stored for efficiency reasons.
     * INVARIANT: size=buckets[0].size()+...+buckets[buckets.length-1].size()
     */
    private int size=0;

    /** 
     * @effects a new queue with the given number of priorities, and
     *  the given number of entries PER PRIORITY.  Hence 0 through 
     *  priorities-1 are the legal priorities, and there are up to
     *  capacityPerPriority*priorities elements in the queue.
     * @exception IllegalArgumentException priorities or capacityPerPriority
     *  is non-positive.
     */
    public BucketQueue(int priorities, int capacityPerPriority) 
            throws IllegalArgumentException {
        if (priorities<=0)
            throw new IllegalArgumentException(
                "Bad priorities: "+priorities);
        if (capacityPerPriority<=0)
            throw new IllegalArgumentException(
                "Bad capacity: "+capacityPerPriority);

        this.buckets=new Buffer[priorities];
        for (int i=0; i<buckets.length; i++) {
            buckets[i]=new Buffer(capacityPerPriority);
        }
    }

    /**
     * @effects makes a new queue that will hold up to capacities[i]
     *  elements of priority i.  Hence the legal priorities are 0
     *  through capacities.length-1
     * @exception IllegalArgumentException capacities.length<=0 or 
     *  capacities[i]<=0 for any i
     */
    public BucketQueue(int[] capacities) throws IllegalArgumentException {
        if (capacities.length<=0)
            throw new IllegalArgumentException();
        this.buckets=new Buffer[capacities.length];

        for (int i=0; i<buckets.length; i++) {
            if (capacities[i]<=0)
                throw new IllegalArgumentException(
                    "Non-positive capacity: "+capacities[i]);
            buckets[i]=new Buffer(capacities[i]);
        }
    }

    /** "Copy constructor": constructs a a new shallow copy of other. */
    public BucketQueue(BucketQueue other) {
        //Note that we can't just shallowly clone other.buckets
        this.buckets=new Buffer[other.buckets.length];
        for (int i=0; i<this.buckets.length; i++) {
            this.buckets[i]=new Buffer(other.buckets[i]); //clone
        }
        this.size=other.size;
    }

    /**
     * Removes all elements from the queue.
     */
    public void clear() {
        repOk();
        for (int i=0; i<buckets.length; i++) 
            buckets[i].clear();        
        size=0;
        repOk();
    }

    /**
     * @modifies this
     * @effects adds o to this, removing and returning some older element of
     *  same or lesser priority as needed
     * @exception IllegalArgumentException priority is not a legal priority, 
     *  as determined by this' constructor
     */
    public Object insert(Object o, int priority) {
        repOk();
        if(priority < 0 || priority >= buckets.length) {
            throw new IllegalArgumentException("Bad priority: "+priority);
        }

        Object ret = buckets[priority].addFirst(o);
        if (ret == null)
            size++;     //Maintain invariant

        repOk();
        return ret;
    }

    /**
     * @modifies this
     * @effects removes all o' s.t. o'.equals(o).  Note that p's
     *  priority is ignored.  Returns true if any elements were removed.
     */
    public boolean removeAll(Object o) {
        repOk();
        //1. For each bucket, remove o, noting if any elements were removed.
        boolean ret=false;
        for (int i=0; i<buckets.length; i++) {
            ret=ret | buckets[i].removeAll(o);
        }
        //2.  Maintain size invariant.  The problem is that removeAll() can
        //remove multiple elements from this.  As a slight optimization, we
        //could incrementally update size by looking at buckets[i].getSize()
        //before and after the call to removeAll(..).  But I favor simplicity.
        if (ret) {
            this.size=0;
            for (int i=0; i<buckets.length; i++)
                this.size+=buckets[i].getSize();
        }
        repOk();
        return ret;
    }

    public Object extractMax() throws NoSuchElementException {
        repOk();
        try {
            for (int i=buckets.length-1; i>=0 ;i--) {
                if (! buckets[i].isEmpty()) {
                    size--;
                    return buckets[i].removeFirst();
                }
            }
            throw new NoSuchElementException();
        } finally {
            repOk();
        }
    }

    public Object getMax() throws NoSuchElementException {
        //TODO: we can optimize this by storing the position of the first
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
     * @exception IllegalArgumentException priority is not a legal priority, 
     *  as determined by this' constructor
     */
    public int size(int priority) throws IllegalArgumentException {
        if(priority < 0 || priority >= buckets.length) {
            throw new IllegalArgumentException("Bad priority: "+priority);
        }

        return buckets[priority].getSize();
    }

    public boolean isEmpty() {
        return size()==0;
    }

    /** 
     * @requires this not modified while iterator in use
     * @effects yields the elements of this exactly once, from highest priority
     *  to lowest priority.  Within each priority level, newer elements are
     *  yielded before older ones.  
     */
    public Iterator iterator() {
        return new BucketQueueIterator(buckets.length-1, this.size());
    }

    /** 
     * @requires this not modified while iterator in use
     * @effects yields the best n elements from startPriority down to to lowest
     *  priority.  Within each priority level, newer elements are yielded before
     *  older ones, and each element is yielded exactly once.  May yield fewer
     *  than n elements.
     * @exception IllegalArgumentException startPriority is not a legal priority
     *  as determined by this' constructor
     */
    public Iterator iterator(int startPriority, int n) 
            throws IllegalArgumentException {
        if (startPriority<0 || startPriority>=buckets.length)
            throw new IllegalArgumentException("Bad priority: "+startPriority);

        return new BucketQueueIterator(startPriority, n);
    }

    private class BucketQueueIterator extends UnmodifiableIterator {
        private Iterator currentIterator;
        private int currentBucket;
        private int left;

        /**
         * @requires buckets.length>0
         * @effects creates an iterator that yields the best
         *  n elements.
         */
        public BucketQueueIterator(int startPriority, int n) {
            this.currentBucket=startPriority;
            this.currentIterator=buckets[currentBucket].iterator();
            this.left=n;
        }

        public synchronized boolean hasNext() {
            if (left<=0)
                return false;
            if (currentIterator.hasNext())
                return true;
            if (currentBucket<0)
                return false;

            //Find non-empty bucket.  Note the "benevolent side effect".
            //(Changes internal state, but not visible to caller.)
            for (currentBucket-- ; currentBucket>=0 ; currentBucket--) {
                currentIterator=buckets[currentBucket].iterator();
                if (currentIterator.hasNext())
                    return true;
            }
            return false;
        }

        public synchronized Object next() {
            //This relies on the benevolent side effects of hasNext.
            if (! hasNext())
                throw new NoSuchElementException();
            
            left--;
            return currentIterator.next();
        }
    }

    /** Returns a shallow copy of this, of type BucketQueue */
    public Object clone() {
        return new BucketQueue(this);        
    }

    private void repOk() {
        /*
        int count=0;
        for (int i=0; i<buckets.length; i++) {
            count+=buckets[i].getSize();
        }
        Assert.that(count==size);
        */
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        buf.append("[");
        for (int i=buckets.length-1; i>=0; i--) {
            if (i!=buckets.length-1)
                buf.append(", ");
            buf.append(buckets[i].toString());
        }
        buf.append("]");
        return buf.toString();            
    }
}
