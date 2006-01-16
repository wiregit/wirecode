
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A BucketQueue sorts elements by their priority, and then by when you add them.
 * 
 * For instance, elements might have priorities of 0, 1, or 2.
 * The BucketQueue will have 3 buckets.
 * When you add an element to the BucketQueue, the BucketQueue will put it in the appropriate bucket.
 * Within each bucket, items are added to the top.
 * 
 * When you iterate through a BucketQueue, you get the highest priority most recently added item first.
 * Then, you get the rest of the items with that priority, from newest to oldest.
 * After that, the iterator will provide the most recently added item with the second highest priority.
 * This continues until the last item, which is the oldest with the lowest priority.
 * 
 * A discrete-case priority queue.
 * Designed to be a replacement for BinaryHeap for the special case when there are only a small number of positive priorities, where larger numbers are higher priority.
 * Unless otherwise noted, all methods have the same specifications as BinaryHeap.
 * This also has a few additional methods not found in BinaryHeap.
 * 
 * This class is not synchronized.
 */
public class BucketQueue implements Cloneable {

    /**
     * An array of buckets that will hold the elements of each priority.
     * 
     * Buffer is LimeWire's circular buffer.
     * buckets is an array of Buffer objects.
     * 
     * There is one Buffer for each different priority.
     * For instance, there might be 3 buckets, with the priorities 0, 1, and 2.
     * 
     * Each bucket holds elements of that priority only.
     * Inside a bucket, the elements we added most recently are at the front.
     */
    private Buffer[] buckets;

    /**
     * The total number of elements in this BucketQueue.
     * A BucketQueue has a number of buckets, each of which can hold a different maximum number of elements.
     * Each time we add an element to a bucket, we increment this count.
     * 
     * Keeping this count up to date is more efficient than totaling all the buckets.
     * 
     * An invariant is something that is true no matter how empty or full this collection class is.
     * The size satisifies the following invariant:
     * size = buckets[0].size() + ... + buckets[buckets.length - 1].size()
     * This is how you would total all the buckets.
     */
    private int size = 0; // At the start, the're aren't any elements in any of the buckets

    /**
     * Make a new BucketQueue with a bucket for each priority that holds elements of that priority in it.
     * 
     * The priorities are the bucket numbers, 0 through priorities - 1.
     * There are up to priorities * capacityPerPriority elements in the BucketQueue.
     * 
     * PongCacher.addPong() and the PriorityMessageQueue constructor make new BucketQueue objects this way.
     * 
     * @param priorities          The number of different priorities this new BucketQueue will sort elements into
     * @param capacityPerPriority The number of elements this new BucketQueue will keep for each priority
     */
    public BucketQueue(int priorities, int capacityPerPriority) throws IllegalArgumentException {

        // Make sure the caller gave us a positive number of buckets that can each hold a positive number of elements
        if (priorities          <= 0) throw new IllegalArgumentException("Bad priorities: " + priorities);
        if (capacityPerPriority <= 0) throw new IllegalArgumentException("Bad capacity: "   + capacityPerPriority);

        // Make the array of buckets, there will be one bucket for each priority
        this.buckets = new Buffer[priorities];

        // Loop for each priority bucket
        for (int i = 0; i < buckets.length; i++) {

            // Make the Buffer of elements of that priority
            buckets[i] = new Buffer(capacityPerPriority); // Use LimeWire Buffer, a circular buffer that throws away the oldest item when full
        }
    }

    /**
     * Make a new BucketQueue from an array that lists how many elements each bucket should hold.
     * 
     * The priorities are the bucket numbers, 0 through capacities.length - 1.
     * 
     * HostCatcher.ENDPOINT_QUEUE is a BucketQueue made with this constructor.
     * It passes the int array {400, 1000, 20}.
     * This creates a BucketQueue with 3 buckets, numbered 0, 1, and 2.
     * Bucket 0 holds 400 elements, bucket 1 holds 1000, and bucket 2 holds 20.
     * 
     * @param capacities An array like {8, 8, 8} to make 3 buckets that hold up to 8 elements each
     */
    public BucketQueue(int[] capacities) throws IllegalArgumentException {

        // Make sure we'll have at least one bucket
        if (capacities.length <= 0) throw new IllegalArgumentException();

        // Make buckets an array with one bucket for each number in the given array
        this.buckets = new Buffer[capacities.length];

        // Loop through the buckets
        for (int i = 0; i < buckets.length; i++) {

            // Make sure this bucket will be able to hold at least one element
            if (capacities[i] <= 0) throw new IllegalArgumentException("Non-positive capacity: " + capacities[i]);

            // Make a circular buffer that holds the number of elements given by the value of the array
            buckets[i] = new Buffer(capacities[i]);
        }
    }

    /**
     * Make a new BucketQueue object, copying all the references from another one.
     * This is a copy constructor.
     * It constructs a new shallow copy of the given object.
     * 
     * @param other The other BucketQueue object to make this new one a copy of
     */
    public BucketQueue(BucketQueue other) {

        /*
         * Note that we can't just shallowly clone other.buckets
         */

        // Make a new array of LimeWire Buffer objects
        this.buckets = new Buffer[other.buckets.length]; // Make it have as many buckets as the other object

        // Loop for each bucket, if length is 3 loops for 0, 1, and 2
        for (int i = 0; i < this.buckets.length; i++) {

            // Copy all the references from the other bucket into this one
            this.buckets[i] = new Buffer(other.buckets[i]);
        }

        // Copy across the size so we don't have to calculate it here
        this.size = other.size;
    }

    /**
     * Remove all the elements from all the buckets, making them all empty.
     * After this runs, there are still buckets for each priority.
     * But, they are all empty, there are no elements in this BucketQueue at all.
     */
    public void clear() {

        repOk();

        // Loop for each bucket, and clear all the elements from it
        for (int i = 0; i < buckets.length; i++) buckets[i].clear();

        // Record this BucketQueue is empty
        size = 0;

        repOk();
    }

    /**
     * Add an object to this BucketQueue.
     * 
     * A BucketQueue object has a bucket for each priority.
     * Each bucket can hold a certain number of objects.
     * If a bucket for a priority is full, adding a new object with that priority will push one out.
     * The one that gets pushed out will have the same priority as the one we added.
     * The buckets are LimeWire circular Buffer objets.
     * The object that gets pushed out will be the one that has been in the bucket the longest.
     * 
     * @param o        The object to insert
     * @param priority The priority of this object, like 0, 1, 2 or so on, which is the bucket we'll put it in
     * @return         The object with the same priority that has been in the full bucket the longest.
     *                 If the bucket for the given priority isn't full, nothing gets pushed out, returns null.
     */
    public Object insert(Object o, int priority) {

        repOk();

        // Make sure this object will fall into a bucket
        if (priority < 0 || priority >= buckets.length) throw new IllegalArgumentException("Bad priority: " + priority);

        // Add the given object to the bucket for its priorty
        Object ret = buckets[priority].addFirst(o); // Adds it in a spot that will get pushed out last

        /*
         * If the buckets[priority] Buffer was full, addFirst(o) returned ret, the object that got pushed out.
         * If ret isn't null, something got pushed out and the number of elements in the whole BucketQueue is the same.
         * If ret is null, we added an object to the BucketQueue, and need to count one more.
         * This maintains the invariant for the size count.
         */

        // If addFirst(o) didn't return an object it removed, record that this BucketQueue is holding one more object
        if (ret == null) size++;

        repOk();

        // Return the object that we pushed out
        return ret;
    }

    /**
     * Remove the given object every place it exists in this BucketQueue.
     * This uses the equals method that the object o implements.
     * This doesn't use the priority of the object at all.
     * 
     * @param o The object to find and remove from all the buckets
     * @return  True if we found and removed the object, false if not found
     */
    public boolean removeAll(Object o) {

        repOk();

        /*
         * For each bucket, remove o, noting if any elements were removed.
         */

        // Loop for each bucket
        boolean ret = false;
        for (int i = 0; i < buckets.length; i++) {

            // Remove every instance of o from this bucket
            ret = ret | buckets[i].removeAll(o); // Set ret to true if removeAll found and removed something
        }

        /*
         * Maintain size invariant.
         * The problem is that removeAll() can remove multiple elements from this.
         * As a slight optimization, we could incrementally update size by looking at buckets[i].getSize() before and after the call to removeAll(..).
         * Doing it this way is simpler.
         */

        // Only recalcuate total size if we removed something
        if (ret) {

            // Starting from 0, total up the number of elements in each bucket
            this.size = 0;
            for (int i = 0; i < buckets.length; i++) this.size += buckets[i].getSize();
        }

        repOk();

        // Return true if we removed something, false if o was not found
        return ret;
    }

    /**
     * Remove and return the lowest priority element that we added most recently.
     * This will probably be an element from a low priority bucket, an element with a priority number of -1 or 0.
     * This will be the element we added most recently to that bucket.
     * Removes the element from the bucket.
     * 
     * @return The first element in the lowest priority bucket
     */
    public Object extractMax() throws NoSuchElementException {

        repOk();

        try {

            // Loop for each bucket
            for (int i = buckets.length - 1; i >= 0; i--) {

                // Only do something if this bucket has an element or two
                if (!buckets[i].isEmpty()) {

                    // Record that there will be one fewer element in this BucketQueue
                    size--;

                    // Remove and return the first element in this bucket
                    return buckets[i].removeFirst();
                }
            }

            // All the buckets are empty
            throw new NoSuchElementException();

        } finally {

            repOk();
        }
    }

    /**
     * Return a reference to the highest priority element that we added most recently.
     * This will be an element from a high priority bucket, an element with a priority number like 2 or 1.
     * This will be the element we added most recently to that bucket.
     * Leaves the element in the bucket.
     * 
     * @return The first element in the highest priority bucket
     */
    public Object getMax() throws NoSuchElementException {

        /*
         * TODO: we can optimize this by storing the position of the first non-empty bucket.
         */

        // Loop from the highest priority bucket down to the lowest priority one
        for (int i = buckets.length - 1; i >= 0; i--) {

            // This priority bucket isn't empty
            if (!buckets[i].isEmpty()) {

                // Return a reference to the first element of this bucket
                return buckets[i].first(); // Doesn't remove it, leaves it in the bucket
            }
        }

        // All the buckets are empty
        throw new NoSuchElementException();
    }

    /**
     * The number of elements in this BucketQueue.
     * This BucketQueue object has a bucket for each priority.
     * Each bucket has a certain number of elements.
     * The size variable counts the total number of elements in all the buckets.
     * 
     * @return The number of elements in this BucketQueue
     */
    public int size() {

        // Return the value we totaled by adding the number of elements in each bucket
        return size;
    }

    /**
     * Get the number of elements this BucketQueue is holding with the given priority number.
     * 
     * @return The size of our bucket for the given priority
     */
    public int size(int priority) throws IllegalArgumentException {

        // Make sure we have a bucket for the given priority
        if (priority < 0 || priority >= buckets.length) throw new IllegalArgumentException("Bad priority: " + priority);

        // Return the number of elements we have for that priority
        return buckets[priority].getSize();
    }

    /**
     * True if none of the priority buckets in this BucketQueue have any elements.
     * 
     * @return True if this BucketQueue is empty, false if there is at least one element
     */
    public boolean isEmpty() {

        // If size is 0, return true
        return size() == 0;
    }

    /**
     * Get an iterator that will loop down all the elements in this BucketQueue.
     * It will start with the highest priority items, returning the one we most recently added.
     * Staying in that priority, it will return items we added longer and longer ago.
     * When it's done with those, it will move to the next highest priority.
     * 
     * Yields the elements of this BucketQueue object exactly once, from highest priority to lowest priority.
     * Within each priority level, yields newer elements before older ones.
     * 
     * Don't modify the contents of this BucketQueue while you are using this iterator.
     * 
     * @return A Java Iterator object you can use to iterate through the elements in this BucketQueue
     */
    public Iterator iterator() {

        // Make and return a new instance of the nested BucketQueueIterator class
        return new BucketQueueIterator(
            buckets.length - 1, // Start in the highest priority bucket we have
            this.size());       // Let the iterator return all the elements in this BucketQueue, don't make it stop early
    }

    /**
     * Get an interator that will loop down the elements in this BucketQueue, starting with those that have the given priority and not returning more than n elements.
     * Within each priority, the iterator will return the most recently added elements first.
     * When it's done with a priority, it will move to the next most desirable priority.
     * 
     * Don't modify the contents of this BucketQueue while you are using this iterator.
     * 
     * Yields the best n elements from startPriority down to the lowest priority.
     * Within each priority level, yields newer elements before older ones.
     * Yields each element exactly once.
     * May yield fewer than n elements.
     * 
     * @return A Java Iterator object you can use to iterate through the elements in this BucketQueue
     */
    public Iterator iterator(int startPriority, int n) throws IllegalArgumentException {

        // Make sure we have a bucket of the given priority
        if (startPriority < 0 || startPriority >= buckets.length) throw new IllegalArgumentException("Bad priority: " + startPriority);

        // Make and return a new instance of the nested BucketQueueIterator class
        return new BucketQueueIterator(
            startPriority, // Start in the bucket for the given priority
            n);            // Return only this number of items, then act like we're out even if there are really more
    }

    /**
     * To loop through the elements in this BucketQueue, make a BucketQueueIterator object, and call hasNext() and next() on it.
     * 
     * When you make a BucketQueueIterator, you pass it a priority number, like 2.
     * First, you'll get elements of that priority.
     * You'll get the one you added most recently first.
     * You'll then get them all, down to the oldest one.
     * Then, the iterator will move to the next highest priority, like 1.
     * The last bucket the iterator will go into is the priority 0 bucket.
     * It won't move into a negative priority bucket at all.
     */
    private class BucketQueueIterator extends UnmodifiableIterator {

        /** An iterator in the bucket we're currently in. */
        private Iterator currentIterator;

        /** The bucket we're currently moving through. */
        private int currentBucket;

        /** The number of elements we can still return. */
        private int left;

        /**
         * Make a new BucketQueueIterator object.
         * It will start returning items of the given priority, then move to the lower priority.
         * It will return the best n elements in this BucketQueue.
         * 
         * @param startPriority The iterator will start in the bucket with this priority
         * @param n             Restrict the iterator to only return this many items, even if there really are more
         */
        public BucketQueueIterator(int startPriority, int n) {

            // Save the priority that we'll start in
            this.currentBucket = startPriority;

            // Get an iterator from the bucket with that priority
            this.currentIterator = buckets[currentBucket].iterator();

            // Record that we can only return n items
            this.left = n;
        }

        /**
         * Determines if this BucketQueueIterator can return another item or not.
         * The iterator currentIterator is within one of the priority buckets.
         * If it reaches the end, this method moves it to the start of the next bucket.
         * 
         * @return True if this BucketQueueIterator can return an element, false if it's done
         */
        public synchronized boolean hasNext() {

            // If our count of how many items we can return has gone negative, report no, we can't return an item
            if (left <= 0) return false;

            // If the bucket we're currently on has another item, return true
            if (currentIterator.hasNext()) return true;

            // If we've moved from the positive priority buckets to the 0 priority buckets and beyond into a negative priority bucket, return false
            if (currentBucket < 0) return false;

            /*
             * Find non-empty bucket.
             * Note the "benevolent side effect".
             * Changes internal state, but not visible to caller.
             */

            // Loop on the bucket with a priority one number lower, then one number lower than that, last looping with a priority of 0
            for (currentBucket--;    // Before the loop runs the first time, make currentBucket one priority number lower
                 currentBucket >= 0; // Loop for the last time when currentBucket is 0, when it reaches -1, don't loop
                 currentBucket--) {  // After the loop runs, make currentBucket one less

                // Get an iterator on the bucket with the priority currentBucket
                currentIterator = buckets[currentBucket].iterator(); // The iterator will point to the first element in the bucket

                // If that iterator can return an item
                if (currentIterator.hasNext()) return true; // Return true, we can return an item
            }

            // We looped from the startPriority bucket down through the 0 priority bucket, and they're all empty
            return false; // Return false, we can't return an item
        }

        /**
         * Return the next element in this BucketQueue.
         * Gets the next element in the bucket we're currently on.
         * If we run out of elements in that bucket, calling hasNext() moves the iterator to the start of the next bucket.
         * 
         * @return The next element in this BucketQueue
         */
        public synchronized Object next() {

            /*
             * This relies on the benevolent side effects of hasNext.
             */

            // Determine if the bucket we're currently on has another element, moving currentIterator to the next bucket if necessary
            if (!hasNext()) throw new NoSuchElementException();

            // Record we have one less element left to return
            left--;

            // Have the iterator in the bucket return the element it's on, and move to the next element
            return currentIterator.next();
        }
    }

    /**
     * Copy this BucketQueue object, and return the copy.
     * This makes a new BucketQueue object, sets it up with the same priority buckets, and copies all the references to elements.
     * This is called a shallow copy because the elements themselves are not copied, just the references to them.
     * 
     * @return A new BucketQueue object with exactly the same buckets and references to elements that this one has
     */
    public Object clone() {

        // Use the copy constructor, giving it a reference to this BucketQueue object
        return new BucketQueue(this);
    }

    /** Commented out to not do testing. */
    private void repOk() {

        /*
        int count=0;
        for (int i=0; i<buckets.length; i++) {
            count+=buckets[i].getSize();
        }
        Assert.that(count==size);
        */
    }

    /**
     * Composes text like "[[element, element, element], [element, element], ]".
     * The whole thing is enclosed in square brackets, and inside each bucket is enclosed in square brackets.
     * The text element is the result of calling toString on each element in this BucketQueue.
     * 
     * @return Text that describes the elements in the buckets in this BucketQueue
     */
    public String toString() {

        // Make a Java StringBuffer that we can add individual characters to without it being slow
        StringBuffer buf = new StringBuffer();

        // Start the text like "["
        buf.append("[");

        // Loop through all the buckets, if there are 3 buckets, loops through bucket numbers 2, 1, and 0
        for (int i = buckets.length - 1; i >= 0; i--) {

            // If this isn't the first bucket, separate it with text like ", "
            if (i != buckets.length - 1) buf.append(", ");

            // Call Buffer.toString() to have this bucket describe itself, and add that text to the StringBuffer
            buf.append(buckets[i].toString());
        }

        // Add the closing "]" and return the text as a String
        buf.append("]");
        return buf.toString(); // Makes a regular String from the StringBuffer object
    }
}
