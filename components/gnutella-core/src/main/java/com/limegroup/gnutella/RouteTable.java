package com.limegroup.gnutella;

import java.util.*;

/**
 * The reply routing table.  Given a GUID from a reply message header,
 * this tells you where to route the reply.  It is mutable mapping
 * from globally unique 16-byte message IDs to connections.  Old
 * mappings may be purged without warning, preferably using a FIFO
 * policy.
 */
public class RouteTable {
    /* The ForgetfulHashMap takes care of all the work.
     * TODO3: I wish we could avoid creating new GUIDs everytime
     */
    private Map map;


    /** 
     * @requires size>0
     * @effects Create a new RouteTable holds only the last "size" routing
     * entries. 
     */
    public RouteTable(int size) {
	map=new ForgetfulHashMap(size);
    }

    /**
     * @requires guid not in this, guid and c are non-null, guid.length==16
     * @effects adds the routing entry to this
     */
    public synchronized void put(byte[] guid, Connection c) {
	GUID g=new GUID(guid);
	map.put(g,c);
    }

    /** 
     * @requires guid.length==16
     * @effects returns the corresponding Connection for this GUID, or 
     *  null if none. 
     */
    public synchronized Connection get(byte[] guid) {
	Object o=map.get(new GUID(guid));
	if (o!=null)
	    return (Connection)o;
	else
	    return null;
    }

    /**
     * @requires guid.length==16
     * @effects true if I've seen requests with the given guid and hence
     *  know where to send the replies, i.e., get(guid)!=null
     */
    public synchronized boolean hasRoute(byte[] guid) {
	return map.get(new GUID(guid))!=null;
    }

    /** 
     * @modifies this
     * @effects removes all entries [guid, c2] s.t. c2.equals(c).
     *  This operation is fairly expensive.
     */
    public synchronized void remove(Connection c) {
	Iterator iter=map.keySet().iterator();
	while (iter.hasNext()) {
	    Object guid=iter.next();
	    if (guid==null) //it shouldn't be!
		continue;
	    if (map.get(guid).equals(c)) {
		map.remove(guid);
		//musn't remove keys while iterating over set
		iter=map.keySet().iterator();
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
	    buf.append(conn.toString());
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
    
    
