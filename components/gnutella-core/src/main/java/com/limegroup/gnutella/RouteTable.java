package com.limegroup.gnutella;

import com.limegroup.gnutella.util.ForgetfulHashMap;
import com.sun.java.util.collections.*;

/**
 * The reply routing table.  Given a GUID from a reply message header,
 * this tells you where to route the reply.  It is mutable mapping
 * from globally unique 16-byte message IDs to connections.  Old
 * mappings may be purged without warning, preferably using a FIFO
 * policy.
 */
class RouteTable {
    /* The ForgetfulHashMap takes care of all the work.
     * TODO3: I wish we could avoid creating new GUIDs everytime
     */
    private Map _map;


    /**
     * @requires size>0
     * @effects Create a new RouteTable holds only the last "size" routing
     * entries.
     */
    public RouteTable(int size) {
        _map=new ForgetfulHashMap(size);
    }

    /**
     * @requires guid and c are non-null, guid.length==16
     * @effects adds the routing entry to this
     */
    public synchronized void routeReply(byte[] guid,
                                        ReplyHandler replyHandler) {
        Assert.that(replyHandler != null);
        GUID g=new GUID(guid);
        _map.put(g, replyHandler);
    }

    /**
     * Same as routeReply, except that the routing is only done if a routing
     * for the given GUID doesn't already exist.
     * @requires guid and c are non-null, guid.length==16
     * @effects adds the routing entry to this or returns false
     * @return true if the routing was successfully created, false if a routing
     *   for the given GUID already existed
     */
    public boolean tryToRouteReply(byte[] guid,
                                       ReplyHandler replyHandler) {
        Assert.that(replyHandler != null);
        GUID g=new GUID(guid);
        if(!_map.containsKey(g)) {
            _map.put(g, replyHandler);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @requires guid.length==16
     * @effects returns the corresponding Connection for this GUID, or
     *  null if none.
     */
    public synchronized ReplyHandler getReplyHandler(byte[] guid) {
        return (ReplyHandler)_map.get(new GUID(guid));
    }

    /**
     * @modifies this
     * @effects removes all entries [guid, rh2] s.t. rh2.equals(replyHandler).
     *  This operation is fairly expensive.
     */
    public synchronized void removeReplyHandler(ReplyHandler replyHandler) {
        Collection values = _map.values();
        // Loop, removing values until there are no more
        while(values.remove(replyHandler));
    }

    public synchronized String toString() {
        StringBuffer buf=new StringBuffer("\n");
        Iterator iter=_map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)(iter.next());
            buf.append(entry.getKey()); // GUID
            buf.append(" -> ");
            buf.append(entry.getValue()); // Connection
            buf.append("\n");
        }
        return buf.toString();
    }

//      /** Unit test */
//      public static void main(String args[]) {
//      RouteTable rt=null;
//      byte[] g1=new byte[16]; g1[0]=(byte)1;
//      byte[] g2=new byte[16]; g2[0]=(byte)2;
//      byte[] g3=new byte[16]; g3[0]=(byte)3;
//      byte[] g4=new byte[16]; g4[0]=(byte)4;
//      Connection c1=new Connection(); //calls stub
//      Connection c2=new Connection();
//      Connection c3=new Connection();
//      Connection c4=new Connection();

//      rt=new RouteTable(3);
//      rt.put(g1, c1);
//      rt.put(g2, c2);
//      rt.put(g3, c3);
//      Assert.that(rt.get(g1)==c1);
//      Assert.that(rt.get(g2)==c2);
//      Assert.that(rt.get(g3)==c3);
//      rt.put(g4, c4);
//      Assert.that(rt.get(g1)==null);
//      Assert.that(rt.get(g2)==c2);
//      Assert.that(rt.get(g3)==c3);
//      Assert.that(rt.get(g4)==c4);
//      rt.put(g1, c1);
//      Assert.that(rt.get(g1)==c1);
//      Assert.that(rt.get(g2)==null);
//      Assert.that(rt.get(g3)==c3);
//      Assert.that(rt.get(g4)==c4);

//      rt=new RouteTable(1);
//      rt.put(g1, c1);
//      Assert.that(rt.get(g1)==c1);
//      rt.put(g2, c2);
//      Assert.that(rt.get(g1)==null);
//      Assert.that(rt.get(g2)==c2);
//      rt.put(g3, c3);
//      Assert.that(rt.get(g1)==null);
//      Assert.that(rt.get(g2)==null);
//      Assert.that(rt.get(g3)==c3);

//      rt=new RouteTable(2);
//      rt.put(g1,c1);
//      rt.remove(c1);
//      Assert.that(rt.get(g1)==null);
//      }
}


