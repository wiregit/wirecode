package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.NoSuchElementException;
import com.sun.java.util.collections.Iterator;

/** 
 * A queue of Endpoints.  Designed to be a replacement for BinaryHeap for the
 * special case when there are only a small number of positive priorities, where
 * larger numbers are higher priority.  Unless otherwise noted, all methods have
 * the same specifications as BinaryHeap.  This also has a few additional
 * methods not found in BinaryHeap.  <b>This class is not synchronized.</b> 
 */
public class BucketQueue implements Cloneable {
    /** 
     * Within each bucket, elements at the FRONT are newer then the back.  It is
     * assumed that buckets is very small; otherwise additional state could
     * speed up some of the operations.  
     */
    private Buffer[] buckets;

    /**
     * @requires capacities.length!=0 && capacities[i]>0
     * @effects makes a new queue that will hold up to capacities[i]
     *  elements of weight i.  
     */
    public BucketQueue(int[] capacities) {
        this.buckets=new Buffer[capacities.length];
        for (int i=0; i<buckets.length; i++) 
            buckets[i]=new Buffer(capacities[i]);
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
     * @requires e.getWeight is a legal priority
     * @modifies this
     * @effects adds e to this, removing some older element of same or lesser
     *  priority as needed
     */
    public Endpoint insert(Endpoint e) {
        int weight=e.getWeight();
        return (Endpoint)buckets[weight].addFirst(e);
    }

    /**
     * @modifies this
     * @effects removes all e' s.t. e'.equals(e).  Note that e's
     *  weight is ignored.  Returns true if any elements were removed.
     */
    public boolean removeAll(Endpoint e) {
        boolean ret=false;
        for (int i=0; i<buckets.length; i++) {
            ret=ret | buckets[i].removeAll(e);
        }
        return ret;
    }

    public Endpoint extractMax() throws NoSuchElementException {
        for (int i=buckets.length-1; i>=0 ;i--) {
            if (! buckets[i].isEmpty()) {
                return (Endpoint)buckets[i].removeFirst();
            }
        }
        throw new NoSuchElementException();
    }

    public Endpoint getMax() throws NoSuchElementException {
        for (int i=buckets.length-1; i>=0 ;i--) {
            if (! buckets[i].isEmpty()) {
                return (Endpoint)buckets[i].first();
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
     * Returns the number of entries with the given priority. 
     * @param priority MUST be a valid priority, i.e., non-negative and less
     *  than the length of the array passed to this' constructor 
     */
    public int size(int priority) {
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
     */
    public Iterator iterator(int startPriority, int n) {
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
        BucketQueue q=new BucketQueue(new int[] {10, 10, 10, 10, 10});
        Assert.that(q.isEmpty());

        Assert.that(q.insert(e0)==null);
        Assert.that(q.insert(e4)==null);
        Assert.that(q.insert(e2b)==null);
        Assert.that(q.insert(e2a)==null);
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
        Assert.that(q.insert(e0)==null);
        Assert.that(q.insert(e4)==null);
        Assert.that(q.insert(f1)==null);
        Assert.that(q.insert(e2b)==null);
        Assert.that(q.insert(e2a)==null);
        Assert.that(q.removeAll(e0)==true);
        Assert.that(q.size()==1);
        Assert.that(q.getMax()==f1);
        Assert.that(q.removeAll(f1)==true);
        Assert.that(q.isEmpty());        

        //Test clone
        q=new BucketQueue(new int[] {10, 10, 10, 10, 10});
        q.insert(e4);
        q.insert(e2a);
        q.insert(e0);

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

        q.insert(e2b);
        Assert.that(q.size()==(q2.size()+1));
    }
    */
}
