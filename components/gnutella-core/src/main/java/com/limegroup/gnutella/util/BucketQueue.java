package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.NoSuchElementException;
import com.sun.java.util.collections.Iterator;

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
    }

    public void clear() {
        for (int i=0; i<buckets.length; i++) 
            buckets[i].clear();        
    }

    /**
     * @modifies this
     * @effects adds o to this, removing and returning some older element of
     *  same or lesser priority as needed
     * @exception IllegalArgumentException priority is not a legal priority, 
     *  as determined by this' constructor
     */
    public Object insert(Object o, int priority) 
            throws IllegalArgumentException {
        try {
            return buckets[priority].addFirst(o);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Bad priority: "+priority);
        }
    }

    /**
     * @modifies this
     * @effects removes all o' s.t. o'.equals(o).  Note that p's
     *  priority is ignored.  Returns true if any elements were removed.
     */
    public boolean removeAll(Object o) {
        boolean ret=false;
        for (int i=0; i<buckets.length; i++) {
            ret=ret | buckets[i].removeAll(o);
        }
        return ret;
    }

    public Object extractMax() throws NoSuchElementException {
        for (int i=buckets.length-1; i>=0 ;i--) {
            if (! buckets[i].isEmpty()) {
                return buckets[i].removeFirst();
            }
        }
        throw new NoSuchElementException();
    }

    public Object getMax() throws NoSuchElementException {
        for (int i=buckets.length-1; i>=0 ;i--) {
            if (! buckets[i].isEmpty()) {
                return buckets[i].first();
            }
        }
        throw new NoSuchElementException();
    }

    public int size() {
        int ret=0;
        for (int i=0; i<buckets.length; i++) {
            ret+=buckets[i].getSize();
        }
        return ret;
    }

    /** 
     * @effects returns the number of entries with the given priority. 
     * @exception IllegalArgumentException priority is not a legal priority, 
     *  as determined by this' constructor
     */
    public int size(int priority) throws IllegalArgumentException {
        try {
            return buckets[priority].getSize();
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Bad priority: "+priority);
        }
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

    /** Unit test */
    /*
    public static void main(String args[]) {
        Endpoint e4=new Endpoint("garbage", 0); e4.setWeight(4);
        Endpoint e2a=new Endpoint("garbage", 0); e2a.setWeight(2);
        Endpoint e2b=new Endpoint("garbage", 0); e2b.setWeight(2);
        Endpoint e0=new Endpoint("garbage", 0); e0.setWeight(0);
        BucketQueue q=new BucketQueue(5, 10);
        Assert.that(q.isEmpty());

        Assert.that(q.insert(e0, 0)==null);
        Assert.that(q.insert(e4, 4)==null);
        Assert.that(q.insert(e2b, 2)==null);
        Assert.that(q.insert(e2a, 2)==null);
        Assert.that(q.size()==4);
        Assert.that(q.size(4)==1);
        Assert.that(q.size(2)==2);
        Assert.that(q.size(3)==0);
        Assert.that(! q.isEmpty());

        Iterator iter=q.iterator();
        Assert.that(iter.next()==e4);
        Assert.that(iter.next()==e2a);
        Assert.that(iter.next()==e2b);
        Assert.that(iter.next()==e0);
        try {
            iter.next();
            Assert.that(false);
        } catch (NoSuchElementException e) { }

        //Make sure hasNext is idempotent
        iter=q.iterator(4, 100);
        Assert.that(iter.hasNext());
        Assert.that(iter.next()==e4);
        Assert.that(iter.hasNext());
        Assert.that(iter.hasNext());
        Assert.that(iter.next()==e2a);
        Assert.that(iter.hasNext());
        Assert.that(iter.next()==e2b);
        Assert.that(iter.hasNext());
        Assert.that(iter.next()==e0);
        Assert.that(! iter.hasNext());
        try {
            iter.next();
            Assert.that(false);
        } catch (NoSuchElementException e) { }

        iter=q.iterator(4, 2);
        Assert.that(iter.next()==e4);
        Assert.that(iter.next()==e2a);
        Assert.that(! iter.hasNext());        

        iter=q.iterator(2, 3);     //sorting by priority
        Assert.that(iter.hasNext());
        Assert.that(iter.next()==e2a);
        Assert.that(iter.hasNext());
        Assert.that(iter.next()==e2b);
        Assert.that(iter.hasNext());
        Assert.that(iter.next()==e0);
        Assert.that(! iter.hasNext());

        iter=q.iterator(1, 3);     //sorting by priority
        Assert.that(iter.hasNext());
        Assert.that(iter.next()==e0);
        Assert.that(! iter.hasNext());

        Assert.that(q.getMax()==e4);
        Assert.that(q.extractMax()==e4);
        Assert.that(q.extractMax()==e2a);
        Assert.that(q.extractMax()==e2b);
        Assert.that(q.getMax()==e0);
        Assert.that(q.extractMax()==e0);
        try {
            q.getMax();
            Assert.that(true);
        } catch (NoSuchElementException e) { }            

        Endpoint f1=new Endpoint("garbage2", 6346);
        Assert.that(q.removeAll(f1)==false);
        Assert.that(q.insert(e0, 0)==null);
        Assert.that(q.insert(e4, 4)==null);
        Assert.that(q.insert(f1, 1)==null);
        Assert.that(q.insert(e2b, 2)==null);
        Assert.that(q.insert(e2a, 2)==null);
        Assert.that(q.removeAll(e0)==true);
        Assert.that(q.size()==1);
        Assert.that(q.getMax()==f1);
        Assert.that(q.removeAll(f1)==true);
        Assert.that(q.isEmpty());        

        //Test clone
        q=new BucketQueue(new int[] {10, 10, 10, 10, 10});
        q.insert(e4, 4);
        q.insert(e2a, 2);
        q.insert(e0, 0);

        BucketQueue q2=new BucketQueue(q);
        Assert.that(q.size()==q2.size());
        Iterator iter1=q.iterator();
        Iterator iter2=q.iterator();
        while (iter1.hasNext()) {
            try {
                Assert.that(iter1.next().equals(iter2.next()));
            } catch (NoSuchElementException e) {
                Assert.that(false);
            }
        }
        Assert.that(! iter2.hasNext());

        q.insert(e2b, 2);
        Assert.that(q.size()==(q2.size()+1));

        //More rigorous test of insertion.  Also checks objects besides
        //Endpoint.        
        q=new BucketQueue(3, 2);
        Assert.that(q.insert("oldest medium", 1)==null);
        Assert.that(q.insert("older medium", 1)==null);
        Assert.that(q.insert("medium", 1).equals("oldest medium"));
        Assert.that(q.insert("low", 0)==null);
        Assert.that(q.insert("high", 2)==null);
        Assert.that(q.extractMax().equals("high"));
        Assert.that(q.extractMax().equals("medium"));
        Assert.that(q.extractMax().equals("older medium"));
        Assert.that(q.extractMax().equals("low"));

        //Test exceptional cases
        try {
            q=new BucketQueue(new int[0]);
            Assert.that(false);
        } catch (IllegalArgumentException e) { }
        try {
            q=new BucketQueue(new int[] {1, 2, 0});
            Assert.that(false);
        } catch (IllegalArgumentException e) { }
        try {
            q=new BucketQueue(new int[] {1});
        } catch (IllegalArgumentException e) { 
            Assert.that(false);
        }
        try {
            q=new BucketQueue(0, 1);
            Assert.that(false);
        } catch (IllegalArgumentException e) { 
        }
        try {
            q=new BucketQueue(1, 0);
            Assert.that(false);
        } catch (IllegalArgumentException e) { 
        }

        q=new BucketQueue(new int[] {10, 10, 10});
        try {
            q.insert("oops", -1);
            Assert.that(false);
        } catch (IllegalArgumentException e) { }
        try {
            q.insert("oops", 3);
            Assert.that(false);
        } catch (IllegalArgumentException e) { }

        try {
            q.size(-1);
            Assert.that(false);
        } catch (IllegalArgumentException e) { }
        try {
            q.size(3);
            Assert.that(false);
        } catch (IllegalArgumentException e) { }


        try {
            iter=q.iterator(-1, 1);
            Assert.that(false);
        } catch (IllegalArgumentException e) { }
        try {
            iter=q.iterator(3, 1);
            Assert.that(false);
        } catch (IllegalArgumentException e) { }
    }
    */
}
