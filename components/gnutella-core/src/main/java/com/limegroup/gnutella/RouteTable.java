package com.limegroup.gnutella;

import java.util.*;

/**
 * A mutable mapping from globally unique IDs to connections.
 * Old mappings may be purged without warning.  In all cases,
 * IDs are assumed to be 16 byte arrays.<p>
 * 
 * More formally, a RouteTable is a sequence [ (G1, C1), ... (G2, GN) ]
 * ordered from youngest to oldest entries.
 */
public class RouteTable {
    /* The current implementation is a hashtable for the mapping and
     * a queue maintaining the ordering.
     *
     * The abstraction function is
     *   [ (queue[next-1], map.get(queue[next-1])), ..., 
     *     (queue[0], map.get(queue[0])), 
     *     (queue[n], map.get(queue[n])), ...,
     *     (queue[next], map.get(queue[next])) ]
     * BUT with null keys and/or values removed.  See remove()
     * for an understanding of the implications.
     *
     * The rep. invariant is
     *            n==queue.length
     *
     * Note that you could reduce the number of hashes necessary to 
     * purge old entries by exposing the rep. of the hashtable and
     * keeping a queue of indices, not pointers.  In this case, the
     * hashtable should use probing, not chaining.
     *
     * TODO3: I wish we could avoid creating new GUIDs everytime
     */
    private Map map=new HashMap();
    private Object[] queue;
    private int next;
    private int n;

    /** 
     * @requires size>0
     * @effects Create a new RouteTable holds only the last "size" routing
     * entries. 
     */
    public RouteTable(int size) {
	Assert.that(size>0);
	queue=new Object[size];
	next=0;
	n=size;
    }

    /**
     * @requires guid not in this, guid and c are non-null
     * @effects adds the routing entry to this, i.e., this=[(guid,c)]+this
     */
    public synchronized void put(byte[] guid, Connection c) {
	Assert.that(guid!=null);
	Assert.that(c!=null);
	GUID g=new GUID(guid);
	map.put(g,c);
	//Purge oldest entry if we're all full, or if we'll become full
	//after adding this entry.
	if (queue[next]!=null) {
	    map.remove(queue[next]);
	}
	//And make (guid,c) the newest entry
	queue[next]=g;
	next++;
	if (next>=n) {
	    next=0;
	}
    }

    /** Returns the corresponding Connection for this GUID, or null if none. */
    public synchronized Connection get(byte[] guid) {
	Object o=map.get(new GUID(guid));
	if (o!=null)
	    return (Connection)o;
	else
	    return null;
    }

    public synchronized boolean hasRoute(byte[] guid) {
	return map.get(new GUID(guid))!=null;
    }

    /** 
     * @modifies this
     * @effects removes all entries [guid, c2] s.t. c2.equals(c).
     *  This operation runs in O(n) time, where n is the max number
     *  of routing table entries. 
     */
    public synchronized void remove(Connection c) {
	for (int i=0; i<queue.length; i++) {
	    Object guid=queue[i];
	    if (guid==null)
		continue;
	    if (map.get(guid).equals(c)) {
		map.remove(guid);
		queue[i]=null;
	    }
	}
    }	

    public synchronized String toString() {
	StringBuffer buf=new StringBuffer("\n");
	Iterator iter=map.keySet().iterator();
	while (iter.hasNext()) {
	    GUID guid=(GUID)(iter.next());
	    Connection conn=get(guid.bytes());
	    Assert.that(conn!=null);
	    buf.append(guid.toString());
	    buf.append(" -> ");
	    buf.append(conn.sock.toString());
	    buf.append("\n");
	}
	return buf.toString();
    }

//      /** Unit test */
//      public static void main(String args[]) {
//  	RouteTable rt=null;
//  	byte[] g1=new byte[16]; g1[0]=(byte)1;
//  	byte[] g2=new byte[16]; g2[0]=(byte)2;
//  	byte[] g3=new byte[16]; g3[0]=(byte)3;
//  	byte[] g4=new byte[16]; g4[0]=(byte)4;
//  	Connection c1=new Connection(); //calls stub
//  	Connection c2=new Connection();
//  	Connection c3=new Connection();
//  	Connection c4=new Connection();

//  	rt=new RouteTable(3);
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

//  	rt=new RouteTable(1);
//  	rt.put(g1, c1); 
//  	Assert.that(rt.get(g1)==c1);
//  	rt.put(g2, c2); 
//  	Assert.that(rt.get(g1)==null); 
//  	Assert.that(rt.get(g2)==c2); 
//  	rt.put(g3, c3); 
//  	Assert.that(rt.get(g1)==null); 
//  	Assert.that(rt.get(g2)==null); 
//  	Assert.that(rt.get(g3)==c3);

//  	rt=new RouteTable(2);
//  	rt.put(g1,c1);
//  	rt.remove(c1);
//  	Assert.that(rt.get(g1)==null);
//      }
}
    
    
