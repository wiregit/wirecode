package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.*;

/** 
 * A discrete-case priority queue.  Designed to be a replacement for BinaryHeap
 * for the special case when there are only a small number of positive
 * priorities, where larger numbers are higher priority.  Provides strict FIFO
 * behavior at a given priority level and a total bound on size.  Unless
 * otherwise noted, all methods have the same specifications as BinaryHeap.
 * This also has a few additional methods not found in BinaryHeap.  <b>This
 * class is not synchronized.</b>
 */
public class BucketQueue implements Cloneable {
    /** 
     * The actual data.  Abstraction function: this represents the following queue:
     *   [ buckets[N].get(0), buckets[N].get(1), ...
     *     buckets[N-1].get(0), buckets[N-1].get(1), ... ]
     * where N==buckets.length-1.  In other words, within each bucket, elements
     * at the FRONT are newer then the back.  It is assumed that buckets is very
     * small; otherwise additional state could speed up some of the operations.
     */
    private LinkedList[] buckets;
    /** The maximum number of elements to store in this. */
    private int capacity;
    /** The number of elements stored in this. 
     *  INVARIANT: size=sum over all i of buckets[i].size()
     *  INVARIANT: size()<=capacity */
    private int size;

    /** 
     * Creates a new queue with the given number of priorities and capacities
     *
     * @param priorities the number of priority levels.  Hence 0 through priorities-1
     *  are the legal priorities
     * @param capacity the maximum number of elements to store
     * @exception IllegalArgumentException priorities or capacity is non-positive.
     */
    public BucketQueue(int priorities, int capacity) 
            throws IllegalArgumentException {
        if (priorities<=0 || capacity<=0)
            throw new IllegalArgumentException();

        this.buckets=new LinkedList[priorities];
        for (int i=0; i<buckets.length; i++) {
            buckets[i]=new LinkedList();
        }
        this.capacity=capacity;
        this.size=0;
    }


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
    public Object insert(Object o, int priority) 
            throws IllegalArgumentException {
        repOk();
        try {
            buckets[priority].addFirst(o);
            size++;
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Bad priority: "+priority);
        }
        Object ret=removeExcess();
        repOk();
        return ret;
    }

    /** Ensures this is not greater than the capacity by removing and returning
     *  the oldest element if needed. */
    private Object removeExcess() {
        int extras=size-capacity;
        Assert.that(extras<=1, 
                    "Size exceeded by too much: "+size+" vs. "+capacity);
        if (extras>0) {
            //TODO: we could avoid the loop by maintaining the index of the first
            //non-empty bucket.
            for (int i=0; i<buckets.length; i++) {
                if (! buckets[i].isEmpty()) {
                    size--;
                    return buckets[i].removeLast();
                }
            }
        }
        return null;
    }

//     /**
//      * @modifies this
//      * @effects removes all o' s.t. o'.equals(o).  Note that p's
//      *  priority is ignored.  Returns true if any elements were removed.
//      */
//     public boolean removeAll(Object o) {
//         repOk();
//         boolean ret=false;
//         for (int i=0; i<buckets.length; i++) {
//             boolean removed=buckets[i].removeAll(o);
//             if (removed) {
//                 size-=number of removed elements;
//                 ret=true;
//             }
//         }
//         repOk();
//         return ret;
//     }

    public Object extractMax() throws NoSuchElementException {
        repOk();
        for (int i=buckets.length-1; i>=0 ;i--) {
            if (! buckets[i].isEmpty()) {
                size--;
                Object ret=buckets[i].removeFirst();
                repOk();
                return ret;
            }
        }
        repOk();
        throw new NoSuchElementException();
    }

    public Object getMax() throws NoSuchElementException {
        for (int i=buckets.length-1; i>=0 ;i--) {
            if (! buckets[i].isEmpty()) {
                return buckets[i].getFirst();
            }
        }
        throw new NoSuchElementException();
    }

    /** 
     * Returns the maximum number of elements this can store, i.e., the value
     * passed to the constructor.
     */
    public int capacity() {
        return capacity;
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
        try {
            return buckets[priority].size();
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Bad priority: "+priority);
        }
    }

    public boolean isEmpty() {
        return size==0;
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

    /** Checks internal consistency. */
    private final void repOk() {
//         int count=0;
//         for (int i=0; i<buckets.length; i++)
//             count+=buckets[i].size();
//         Assert.that(count==size, "Count mismatches size: "+count+" vs. "+size);
//         Assert.that(count<=capacity, "Capacity exceeded: "+count+" vs. "+capacity);
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
        testIllegalArguments();
        testWithoutLimits();
        testLimits();
    }

    private static void testWithoutLimits() {
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
    }

    private static void testLimits() {
        //More rigorous test of insertion.  Also checks objects besides
        //Endpoint.        
        BucketQueue q=new BucketQueue(3, 4);
        Assert.that(q.insert("low", 0)==null);
        Assert.that(q.insert("older high", 2)==null);
        Assert.that(q.insert("oldest medium", 1)==null);
        Assert.that(q.insert("older medium", 1)==null);
        Assert.that(q.insert("medium", 1).equals("low"));
        Assert.that(q.insert("high", 2).equals("oldest medium"));  //h, oh, m, om
        Assert.that(q.insert("another low", 0).equals("another low"));

        Iterator iter=q.iterator();     //sorting by priority
        Assert.that(iter.hasNext());
        Assert.that(iter.next().equals("high"));
        Assert.that(iter.hasNext());
        Assert.that(iter.next().equals("older high"));
        Assert.that(iter.hasNext());
        Assert.that(iter.next().equals("medium"));
        Assert.that(iter.hasNext());
        Assert.that(iter.next().equals("older medium"));
        Assert.that(! iter.hasNext());

        Assert.that(q.extractMax().equals("high"));
        Assert.that(q.extractMax().equals("older high"));
        Assert.that(q.extractMax().equals("medium"));
        Assert.that(q.extractMax().equals("older medium"));                    
    }

    private static void testIllegalArguments() {
        BucketQueue q=null;
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

        q=new BucketQueue(3, 10);
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

        Iterator iter=null;
        try {
            iter=q.iterator(-1, 1);
            Assert.that(false);
        } catch (IllegalArgumentException e) { }
        try {
            iter=q.iterator(3, 1);
            Assert.that(false);
        } catch (IllegalArgumentException e) { }
    }

//         Endpoint f1=new Endpoint("garbage2", 6346);
//         Assert.that(q.removeAll(f1)==false);
//         Assert.that(q.insert(e0, 0)==null);
//         Assert.that(q.insert(e4, 4)==null);
//         Assert.that(q.insert(f1, 1)==null);
//         Assert.that(q.insert(e2b, 2)==null);
//         Assert.that(q.insert(e2a, 2)==null);
//         Assert.that(q.removeAll(e0)==true);
//         Assert.that(q.size()==1);
//         Assert.that(q.getMax()==f1);
//         Assert.that(q.removeAll(f1)==true);
//         Assert.that(q.isEmpty()); 
        

        //Test clone
//         q=new BucketQueue(new int[] {10, 10, 10, 10, 10});
//         q.insert(e4, 4);
//         q.insert(e2a, 2);
//         q.insert(e0, 0);

//         BucketQueue q2=new BucketQueue(q);
//         Assert.that(q.size()==q2.size());
//         Iterator iter1=q.iterator();
//         Iterator iter2=q.iterator();
//         while (iter1.hasNext()) {
//             try {
//                 Assert.that(iter1.next().equals(iter2.next()));
//             } catch (NoSuchElementException e) {
//                 Assert.that(false);
//             }
//         }
//         Assert.that(! iter2.hasNext());

//         q.insert(e2b, 2);
//         Assert.that(q.size()==(q2.size()+1));
    */

}
