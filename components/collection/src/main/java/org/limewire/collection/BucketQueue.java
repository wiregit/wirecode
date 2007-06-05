package org.limewire.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** 
 * Provides a discrete-case priority queue. <code>BucketQueue</code> is designed 
 * to be a replacement for a binary heap for the special case when there 
 * are only a small number of positive priorities. (Priorities are zero based 
 * and therefore, when priority is set to 3, the values are [0, 1, 2]. Also, larger 
 * numbers are higher priority.)
 * <p>
 * You determine how many element cases per priority are allowed in the queue. 
 * When the queue attempts to add an element more than the maximum capacity for 
 * the priority, the first element is removed upon {@link #insert(Object, int)}. 
 * <p>
 * Note: <code>BucketQueue</code> is not synchronized.
 * 
 <pre>
    public class MyObject{
        public String s;
        public int item;
        public MyObject(String s, int item){
            this.s = s;
            this.item = item;
        }       

        public String toString(){
            return s + "=" + item ;
        }
    }   

   void sampleCodeBucketQueue(){
                                            //priorities, capacity
        BucketQueue&lt;MyObject&gt; bq = new BucketQueue&lt;MyObject&gt;(3,2);
        MyObject returnFromInsert ;
        
        bq.insert(new MyObject("a", 1), 1);
        bq.insert(new MyObject("b", 2), 1);
        System.out.println("List contents: ");

        for(MyObject  i : bq)
            System.out.println("\tElement is " + i);

        returnFromInsert = bq.insert(new MyObject("c", 3), 1);
        if(returnFromInsert != null)
            System.out.println("Element " + returnFromInsert + " popped because there are already 2 elements of priority 1.");
        
        System.out.println("List contents: ");
        for(MyObject  i : bq)
            System.out.println("\tElement is " + i);
        
        bq.insert(new MyObject("d", 4), 2);
        bq.insert(new MyObject("e", 5), 2);

        bq.insert(new MyObject("f", 1), 0);
        
        
        System.out.println("List contents: ");
        for(MyObject  i : bq)
            System.out.println("\tElement is " + i);
        
        System.out.println("Max: " + bq.getMax().toString() + " and bq is " + bq);
        
    }

    Output:
        List contents: 
            Element is b=2
            Element is a=1
        Element a=1 popped because there are already 2 elements of priority 1.
        List contents: 
            Element is c=3
            Element is b=2
        List contents: 
            Element is e=5
            Element is d=4
            Element is c=3
            Element is b=2
            Element is f=1
        Max: e=5 and bq is [[e=5, d=4], [c=3, b=2], [f=1]]
 </pre>
 
 * 
 */
public class BucketQueue<E> implements Cloneable, Iterable<E> {
    /** 
     * Within each bucket, elements at the FRONT are newer then the back.  It is
     * assumed that buckets is very small; otherwise additional state could
     * speed up some of the operations.  
     */
    private Buffer<E>[] buckets;
    /**
     * The size, stored for efficiency reasons.
     * INVARIANT: size=buckets[0].size()+...+buckets[buckets.length-1].size()
     */
    private int size=0;

    /** 
     * Has the effect that a new queue with the given number of priorities, and
     *  the given number of entries PER PRIORITY.  Hence 0 through 
     *  priorities-1 are the legal priorities, and there are up to
     *  capacityPerPriority*priorities elements in the queue.
     * @exception IllegalArgumentException priorities or capacityPerPriority
     *  is non-positive.
     */
    @SuppressWarnings("unchecked")
    public BucketQueue(int priorities, int capacityPerPriority) 
            throws IllegalArgumentException {
        if (priorities<=0)
            throw new IllegalArgumentException(
                "Bad priorities: "+priorities);
        if (capacityPerPriority<=0)
            throw new IllegalArgumentException(
                "Bad capacity: "+capacityPerPriority);

        this.buckets = new Buffer[priorities];
        for (int i=0; i<buckets.length; i++) {
            buckets[i] = new Buffer<E>(capacityPerPriority);
        }
    }

    /**
     * Has the effect to make a new queue that will hold up to capacities[i]
     *  elements of priority i.  Hence the legal priorities are 0
     *  through capacities.length-1
     * @exception IllegalArgumentException capacities.length<=0 or 
     *  capacities[i]<=0 for any i
     */
    @SuppressWarnings("unchecked")
    public BucketQueue(int[] capacities) throws IllegalArgumentException {
        if (capacities.length<=0)
            throw new IllegalArgumentException();
        this.buckets = new Buffer[capacities.length];

        for (int i=0; i<buckets.length; i++) {
            if (capacities[i]<=0)
                throw new IllegalArgumentException(
                    "Non-positive capacity: "+capacities[i]);
            buckets[i]=new Buffer<E>(capacities[i]);
        }
    }

    /** "Copy constructor": constructs a a new shallow copy of other. */
    @SuppressWarnings("unchecked")
    public BucketQueue(BucketQueue<? extends E> other) {
        //Note that we can't just shallowly clone other.buckets
        this.buckets = new Buffer[other.buckets.length];
        for (int i=0; i<this.buckets.length; i++) {
            this.buckets[i]=new Buffer<E>(other.buckets[i]); //clone
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
     * Modifies this.
     * Has the effect to add o to this, removing and returning some older element of
     *  same or lesser priority as needed
     * @exception IllegalArgumentException priority is not a legal priority, 
     *  as determined by this' constructor
     */
    public E insert(E o, int priority) {
        repOk();
        if(priority < 0 || priority >= buckets.length) {
            throw new IllegalArgumentException("Bad priority: "+priority);
        }

        E ret = buckets[priority].addFirst(o);
        if (ret == null)
            size++;     //Maintain invariant

        repOk();
        return ret;
    }

    /**
     * Modifies this.
     * Has the effects to remove all o' such that o'.equals(o).  Note that p's
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

    public E extractMax() throws NoSuchElementException {
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

    public E getMax() throws NoSuchElementException {
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
     * Has the effect to return the number of entries with the given priority. 
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
     * Requires this not modified while iterator in use
     * Has the effect to yield the elements of this exactly once, from highest priority
     *  to lowest priority. Within each priority level, newer elements are
     *  yielded before older ones.  
     */
    public Iterator<E> iterator() {
        return new BucketQueueIterator(buckets.length-1, this.size());
    }

    /** 
     * Requires this not modified while iterator in use
     * Has the effect to yield the best n elements from startPriority down to to lowest
     *  priority.  Within each priority level, newer elements are yielded before
     *  older ones, and each element is yielded exactly once.  May yield fewer
     *  than n elements.
     * @exception IllegalArgumentException startPriority is not a legal priority
     *  as determined by this' constructor
     */
    public Iterator<E> iterator(int startPriority, int n) 
            throws IllegalArgumentException {
        if (startPriority<0 || startPriority>=buckets.length)
            throw new IllegalArgumentException("Bad priority: "+startPriority);

        return new BucketQueueIterator(startPriority, n);
    }

    private class BucketQueueIterator extends UnmodifiableIterator<E> {
        private Iterator<E> currentIterator;
        private int currentBucket;
        private int left;

        /**
         * Requires buckets.length>0
         * Has the effect to creates an iterator that yields the best
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

        public synchronized E next() {
            //This relies on the benevolent side effects of hasNext.
            if (! hasNext())
                throw new NoSuchElementException();
            
            left--;
            return currentIterator.next();
        }
    }

    /** Returns a shallow copy of this, of type BucketQueue */
    public BucketQueue<E> clone() {
        return new BucketQueue<E>(this);        
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
        StringBuilder buf=new StringBuilder();
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
