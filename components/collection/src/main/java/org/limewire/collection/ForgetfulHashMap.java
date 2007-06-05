package org.limewire.collection;

import java.util.HashMap;
import java.util.Map;


/**
 * A mapping that "forgets" keys and values using a FIFO replacement
 * policy, much like a cache.
 * <p>
 * More formally, a <code>ForgetfulHashMap</code> is a sequence of key-value pairs
 * [ (K1, V1), ... (KN, VN) ] ordered from youngest to oldest. When
 * inserting a new pair, (KN, VN) is discarded if N is greater than
 * a size (this threshold is fixed when the table is
 * constructed). <b>However, this property may not hold if keys are
 * re-mapped to different values</b>; if you need to re-map keys to different 
 * values, use {@link FixedsizeForgetfulHashMap}.
 * <p>
 * Technically, this is not a valid subtype of <code>HashMap</code> (since items
 * can be removed), but <code>HashMap</code> makes implementation easy. 
 * Also, <code>ForgetfulHashMap</code> is <b>not synchronized</b>.
<pre>
    public class MyObjectHash{
        public String s;
        public int item;
        public MyObjectHash(String s, int item){
            this.s = s;
            this.item = item;
        }       

        public String toString(){
            return s + "=" + item;
        }
        
        public boolean equals(Object obj) {
            MyObjectHash other = (MyObjectHash)obj;
            return (this.s.equals(other.s) && this.item == other.item);         
        }
                
        public int hashCode() {
            return this.item * 31 + s.hashCode();
        }
    }   

    void sampleCodeForgetfulHashMap(){
        ForgetfulHashMap&lt;String, MyObjectHash&gt; fhm = new ForgetfulHashMap&lt;String, MyObjectHash&gt;(2);
        fhm.put("Mykey1", new MyObjectHash("a", 1));
        System.out.println("1) Size is: " + fhm.size() + " contents: " + fhm);
        fhm.put("Mykey2", new MyObjectHash("b", 2));
        System.out.println("2) Size is: " + fhm.size() + " contents: " + fhm);
        fhm.put("Mykey3", new MyObjectHash("c", 3));
        System.out.println("3) Size is: " + fhm.size() + " contents: " + fhm);

        fhm.put("Mykey3", new MyObjectHash("replace", 3));
        System.out.println("4) Size is: " + fhm.size() + " contents: " + fhm);
        
    }
    Output:
        1) Size is: 1 contents: {Mykey1=a=1}
        2) Size is: 2 contents: {Mykey2=b=2, Mykey1=a=1}
        3) Size is: 2 contents: {Mykey2=b=2, Mykey3=c=3}
        4) Size is: 1 contents: {Mykey3=replace=3}
   
</pre>
 *
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
     * Requires key is not in this.
     * Modifies this.
     * Has the effect that it adds the given key value pair to this, removing some
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

