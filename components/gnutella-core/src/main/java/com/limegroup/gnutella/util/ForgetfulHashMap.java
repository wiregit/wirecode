package com.limegroup.gnutella.util;

import java.util.HashMap;
import java.util.Map;

/**
 * A mapping that "forgets" keys and values using a FIFO replacement
 * policy, much like a cache.<p>
 *
 * More formally, a ForgetfulHashMap is a sequence of key-value pairs
 * [ (K1, V1), ... (KN, VN) ] ordered from youngest to oldest.  When
 * inserting a new pair, (KN, VN) is discarded if N is greater than
 * some threshold.  This threshold is fixed when the table is
 * constructed.  <b>However, this property may not hold if keys are
 * remapped to different values</b>; if you need this, use 
 * FixedsizeForgetfulHashmap.<p>
 *
 * Technically, this not a valid subtype of HashMap, but it makes
 * implementation really easy. =) Also, ForgetfulHashMap is <b>not
 * synchronized</b>.
 */
public class ForgetfulHashMap<K, V> extends HashMap<K, V> {
    /* The current implementation is a hashtable for the mapping and
     * a queue maintaining the ordering.
     *
     * The ABSTRACTION FUNCTION is
     *   [ (queue[next-1], super.get(queue[next-1])), ...,
     *     (queue[0], super.get(queue[0])),
     *     (queue[n], super.get(queue[n])), ...,
     *     (queue[next], super.get(queue[next])) ]
     * BUT with null keys and/or values removed.  This means that
     * not all entries of queue need be valid keys into the map.
     *
     * The REP. INVARIANT is
     *    n==queue.length
     *    {k | map.get(k)!=null} <= {x | queue[i]==x && x!=null}
     *
     * Here "<=" means "is a subset of".  In other words, every key in
     * the map must be in the queue, though the opposite need not hold.
     *
     * Note that you could reduce the number of hashes necessary to
     * purge old entries by exposing the rep. of the hashtable and
     * keeping a queue of indices, not pointers.  In this case, the
     * hashtable should use probing, not chaining.
     */

    private Object[] queue;
    private int next;
    private int n;

    /**
     * Create a new ForgetfulHashMap that holds only the last "size" entries.
     *
     * @param size the number of entries to hold
     * @exception IllegalArgumentException if size is less < 1.
     */
    public ForgetfulHashMap(int size) {
        if (size < 1)
            throw new IllegalArgumentException();
        queue=new Object[size];
        next=0;
        n=size;
    }

    /**
     * @requires key is not in this
     * @modifies this
     * @effects adds the given key value pair to this, removing some
     *  older mapping if necessary.  The return value is undefined; it
     *  exists solely to conform with the superclass' signature.
     */
    public V put(K key, V value) {
        V ret=super.put(key,value);
        //Purge oldest entry if we're all full, or if we'll become full
        //after adding this entry.  It's ok if queue[next] is no longer
        //in the map.
        if (queue[next]!=null) {
            super.remove(queue[next]);
        }
        //And make (key,value) the newest entry.  It is ok
        //if key is already in queue.
        queue[next]=key;
        next++;
        if (next>=n) {
            next=0;
        }
        return ret;
    }

    /** Calls put on all keys in t.  See put(Object, Object) for 
     *  specification. */
    public void putAll(Map<? extends K, ? extends V> t) {
        for(Map.Entry<? extends K, ? extends V> entry : t.entrySet())
            put(entry.getKey(), entry.getValue());
    }
}

