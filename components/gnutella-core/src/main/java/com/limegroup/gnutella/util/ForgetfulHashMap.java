package com.limegroup.gnutella.util;

import java.util.*;

/**
 * A mapping that "forgets" keys and values using a FIFO replacement
 * policy, much like a cache.<p>
 * 
 * More formally, a RouteTable is a sequence of key-value pairs
 * [ (K1, V1), ... (KN, VN) ] ordered from youngest to oldest.  When
 * inserting a new pair, (KN, VN) is discarded if N is greater than
 * some threshold.  This threshold is fixed when the table is
 * constructed.<p>
 *
 * Technically, this not a valid subtype of HashMap, but it makes
 * implementation really easy. =) Also, ForgetfulHashMap is <b>not
 * synchronized</b>. 
 */
public class ForgetfulHashMap extends HashMap {
    /* The current implementation is a hashtable for the mapping and
     * a queue maintaining the ordering.
     *
     * The abstraction function is
     *   [ (queue[next-1], super.get(queue[next-1])), ..., 
     *     (queue[0], super.get(queue[0])), 
     *     (queue[n], super.get(queue[n])), ...,
     *     (queue[next], super.get(queue[next])) ]
     * BUT with null keys and/or values removed.  This means that
     * not all entries of queue need be valid keys into the map.
     *
     * The rep. invariant is
     *            n==queue.length
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
     * Create a new RouteTable that holds only the last "size" entries.
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

    public Object put(Object key, Object value) {
	Object ret=super.put(key,value);
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

    public void putAll(Map t) {
	Iterator iter=t.keySet().iterator();
	while (iter.hasNext()) {
	    Object key=iter.next();
	    put(key,t.get(key));
	}
    }

//      /** Unit test */
//      public static void main(String args[]) {
//  	//The cryptic variable names are um..."legacy variables" =)
//  	ForgetfulHashMap rt=null;
//  	String g1="key1";
//  	String g2="key2";
//  	String g3="key3";
//  	String g4="key4";
//  	String c1="value1";
//  	String c2="value2";
//  	String c3="value3";
//  	String c4="value4";
	
//  	//1. FIFO put/get tests
//  	rt=new ForgetfulHashMaforcep(3);
//  	rt.put(g1, c1); 
//  	rt.put(g2, c2); 
//  	rt.put(g3, c3); 
//  	Assert.that(rt.get(g1)==c1);
//  	Assert.that(rt.get(g2)==c2); 
//  	Assert.that(rt.get(g3)==c3); 
//  	rt.put(g4, c4); 
//  	Assert.that(rt.get(g1)==null); 
//  	Assert.that(rt.get(g2)==c2); 
//  	Assert.that(rt.get(g3)==c3); 
//  	Assert.that(rt.get(g4)==c4);
//  	rt.put(g1, c1); 
//  	Assert.that(rt.get(g1)==c1); 
//  	Assert.that(rt.get(g2)==null); 
//  	Assert.that(rt.get(g3)==c3); 
//  	Assert.that(rt.get(g4)==c4);

//  	rt=new ForgetfulHashMap(1);
//  	rt.put(g1, c1); 
//  	Assert.that(rt.get(g1)==c1);
//  	rt.put(g2, c2); 
//  	Assert.that(rt.get(g1)==null); 
//  	Assert.that(rt.get(g2)==c2); 
//  	rt.put(g3, c3); 
//  	Assert.that(rt.get(g1)==null); 
//  	Assert.that(rt.get(g2)==null); 
//  	Assert.that(rt.get(g3)==c3);

//  	rt=new ForgetfulHashMap(2);
//  	rt.put(g1,c1);
//  	rt.remove(g1);
//  	Assert.that(rt.get(g1)==null);

//  	//2. Remove tests
//  	rt=new ForgetfulHashMap(2);
//  	rt.put(g1,c1);
//  	rt.remove(g1);
//  	Assert.that(rt.get(g1)==null);
//  	rt.put(g1,c2);
//  	Assert.that(rt.get(g1)==c2);
	
//  	//3. putAll tests.
//  	rt=new ForgetfulHashMap(3);
//  	Map m=new HashMap();
//  	m.put(g1,c1);
//  	m.put(g2,c2);
//  	rt.putAll(m);
//  	Assert.that(rt.get(g1)==c1);
//  	Assert.that(rt.get(g2)==c2);	
//      }
}

