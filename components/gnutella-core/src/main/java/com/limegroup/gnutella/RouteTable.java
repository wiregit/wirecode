package com.limegroup.gnutella;

import com.limegroup.gnutella.util.ForgetfulHashMap;
import com.limegroup.gnutella.util.FixedsizeForgetfulHashMap;
import com.sun.java.util.collections.*;

/**
 * The reply routing table.  Given a GUID from a reply message header,
 * this tells you where to route the reply.  It is mutable mapping
 * from globally unique 16-byte message IDs to connections.  Old
 * mappings may be purged without warning, preferably using a FIFO
 * policy.
 */
class RouteTable {
    /* Map is either a ForgetfulHashMap (pings and queries) or a 
     * FixedsizeForgetfulHashMap (pushes).  These classes take care
     * of the work of expiring old entries.  Note however they may
     * not be fully implemented, so only some operations in _map
     * can be used.
     *
     * TODO3: I wish we could avoid creating new GUIDs everytime
     * TODO3: I wish ForgetfulHashMap and FixedsizeForgetfulHashMap
     *  implemented some common interface.
     */
    private Map _map;


    /**
     * @requires size>0
     * @effects same as RouteTable(size, false)
     */
    public RouteTable(int size) {
        this(size, false);
    }

    /**
     * @requires size>0
     * @effects Create a new RouteTable holds only the last "size" routing
     *  entries.  If allowRemap==false, it is assumed that guid's will never
     *  be remapped to different ReplyHandlers.
     */
    public RouteTable(int size, boolean allowRemap) {
        if (allowRemap)
            //More expensive, but has stronger ordering properties.
            _map=new FixedsizeForgetfulHashMap(size);
        else
            _map=new ForgetfulHashMap(size);
    }

    /**
     * Adds a new routing entry.
     *
     * @requires guid and c are non-null, guid.length==16
     * @modifies this
     * @effects if replyHandler is open, adds the routing entry to this,
     *  replacing any routing entries for guid.  Otherwise returns
     *  without modifying this.
     */
    public synchronized void routeReply(byte[] guid,
                                        ReplyHandler replyHandler) {
        Assert.that(replyHandler != null);
        if (! replyHandler.isOpen())
            return;

        GUID g=new GUID(guid);
        _map.put(g, replyHandler);
    }

    /**
     * Adds a new routing entry if one doesn't exist.
     *
     * @requires guid and c are non-null, guid.length==16
     * @modifies this
     * @effects if no routing table entry for guid exists in this
     *  and replyHandler is still open, adds the routing entry to this
     *  and returns true.  Otherwise returns false, without modifying this.
     */
    public synchronized boolean tryToRouteReply(byte[] guid,
                                                ReplyHandler replyHandler) {
        Assert.that(replyHandler != null);
        if (! replyHandler.isOpen())
            return false;

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
     * @effects removes all entries [guid, rh2] s.t. 
     *  rh2.equals(replyHandler).  This operation runs in linear
     *  time with respect to this' size.
     */
    public synchronized void removeReplyHandler(ReplyHandler replyHandler) {        
        //The aggressive asserts below are to make sure bug X75 has been fixed.
        Assert.that(replyHandler!=null,
                    "Null replyHandler in removeReplyHandler");
        //IMPORTANT: because_map.values() may not be defined (see note above),
        //we can only use _map.keySet().iterator()
        Iterator iter = _map.keySet().iterator();
        while (iter.hasNext()) {
            Object key=iter.next();
            Assert.that(key!=null, "Null key in removeReplyHandler");
            Object value=_map.get(key);
            Assert.that(value!=null, "Null value in removeReplyHandler");
            if (value.equals(replyHandler))
                iter.remove();
        }
    }

    public synchronized String toString() {
        //IMPORTANT: because_map.values() may not be defined (see note above),
        //we can only use _map.keySet().iterator()
        StringBuffer buf=new StringBuffer("\n");
        Iterator iter=_map.keySet().iterator();
        while (iter.hasNext()) {
            Object key=iter.next();
            buf.append(key); // GUID
            buf.append(" -> ");
            buf.append(_map.get(key)); // Connection
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


