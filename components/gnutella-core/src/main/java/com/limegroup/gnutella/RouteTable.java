package com.limegroup.gnutella;

import com.limegroup.gnutella.util.ForgetfulHashMap;
import com.limegroup.gnutella.util.FixedsizeForgetfulHashMap;
import com.sun.java.util.collections.*;

/**
 * The reply routing table.  Given a GUID from a reply message header,
 * this tells you where to route the reply.  It is mutable mapping
 * from globally unique 16-byte message IDs to connections.  Old
 * mappings may be purged without warning, preferably using a FIFO
 * policy.  This class makes a distinction between not having a mapping
 * for a GUID and mapping that GUID to null (in the case of a removed
 * ReplyHandler).
 */
public class RouteTable {
    /**
     * The obvious implementation of this class is a mapping from GUID to
     * ReplyHandler's.  The problem with this representation is it's hard to
     * implement removeReplyHandler efficiently.  You either have to keep the
     * references to the ReplyHandler (which wastes memory) or iterate through
     * the entire table to clean all references (which wastes time AND removes
     * valuable information for preventing duplicate queries).
     *
     * Instead we use a layer of indirection.  _map maps GUIDs to Integers,
     * which act as IDs for each connection.  _idMap maps IDs to ReplyHandlers.
     * _handlerMap maps ReplyHandler to IDs.  So to clean up a connection, we
     * just purge the entries from _handlerMap and _idMap; there is no need to
     * iterate through the entire GUID mapping.  Adding GUIDs and routing
     * replies are still constant-time operations.
     *
     * IDs are allocated sequentially according with the nextID variable.  The
     * field does "wrap around" after reaching the maximum integer value.
     * Though no two open connections will have the same ID--we check
     * _idMap--there is a very low probability that an ID in _map could be
     * prematurely reused.
     *
     * To take care of FIFO replacement, _map is either a ForgetfulHashMap (pings
     * and queries) or a FixedsizeForgetfulHashMap (pushes).  Note however these
     * classes may not be fully implemented, so only some operations in _map can
     * be used.
     *
     * INVARIANT: _idMap and _replyMap are inverses
     *
     * TODO3: I wish we could avoid creating new GUIDs everytime
     * TODO3: I wish ForgetfulHashMap and FixedsizeForgetfulHashMap
     *  implemented some common interface.  
     * TODO3: if IDs were stored in each ReplyHandler, we would not need
     *  _replyMap.  Better yet, if the values of _map were indices (with tags)
     *  into ConnectionManager._initialized[Client]Connections, we would not
     *  need _idMap either.  However, this increases dependenceies.  
     */
    private Map /* GUID -> Integer */ _map;
    private Map /* Integer -> ReplyHandler */ _idMap=new HashMap();
    private Map /* ReplyHandler -> Integer */ _handlerMap=new HashMap();
    private int _nextID;

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
        //repOk();
        Assert.that(replyHandler != null);
        if (! replyHandler.isOpen())
            return;

        GUID g=new GUID(guid);
        Integer id=handler2id(replyHandler);
        _map.put(g, id);
    }

    /**
     * Adds a new routing entry if one doesn't exist.
     *
     * @requires guid and c are non-null, guid.length==16
     * @modifies this
     * @effects if no routing table entry for guid exists in this
     *  (including null mappings from calls to removeReplyHandler) and 
     *  replyHandler is still open, adds the routing entry to this
     *  and returns true.  Otherwise returns false, without modifying this.
     */
    public synchronized boolean tryToRouteReply(byte[] guid,
                                                ReplyHandler replyHandler) {
        //repOk();
        Assert.that(replyHandler != null);
        Assert.that(guid!=null, "Null GUID in tryToRouteReply");

        if (! replyHandler.isOpen())
            return false;

        GUID g=new GUID(guid);
        if(!_map.containsKey(g)) {
            Integer id=handler2id(replyHandler);
            _map.put(g, id);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Looks up the ReplyHandler for a given guid.
     *
     * @requires guid.length==16
     * @effects returns the corresponding Connection for this GUID.     
     *  Returns null if no mapping for guid, or guid maps to null (i.e., 
     *  to a removed ReplyHandler.
     */
    public synchronized ReplyHandler getReplyHandler(byte[] guid) {
        //repOk();
        Integer id=(Integer)_map.get(new GUID(guid));
        if (id==null)
            return null;
        else
            return id2handler(id);  //may be null
    }

    /**
     * Clears references to a given ReplyHandler.
     *
     * @modifies this
     * @effects replaces all entries [guid, rh2] s.t. 
     *  rh2.equals(replyHandler) with entries [guid, null].  This operation
     *  runs in constant time. [sic]
     */
    public synchronized void removeReplyHandler(ReplyHandler replyHandler) {        
        //repOk();
        //The aggressive asserts below are to make sure bug X75 has been fixed.
        Assert.that(replyHandler!=null,
                    "Null replyHandler in removeReplyHandler");
        //Note that _map is not modified.  See overview of class for rationale.
        Integer id=handler2id(replyHandler);
        if (id!=null) {
            _idMap.remove(id);
        }
        _handlerMap.remove(replyHandler);
    }

    /** 
     * @modifies nextID, _handlerMap, _idMap
     * @effects returns a unique ID for the given handler, updating
     *  _handlerMap and _idMap if handler has not been encountered before.
     *  With very low probability, the returned id may be a value _map.
     */
    private Integer handler2id(ReplyHandler handler) {
        //Have we encountered this handler recently?  If so, return the id.
        Integer id=(Integer)_handlerMap.get(handler);
        if (id!=null)
            return id;
    
        //Otherwise return the next free id, searching in extremely rare cases
        //if needed.  Note that his enters an infinite loop if all 2^32 IDs are
        //taken up.  BFD.
        while (true) {
            //don't worry about overflow; Java wraps around TODO1?
            id=new Integer(_nextID++);
            if (_idMap.get(id)==null)
                break;            
        }
    
        _handlerMap.put(handler, id);
        _idMap.put(id, handler);
        return id;
    }

    /**
     * Returns the ReplyHandler associated with the following ID, or
     * null if none.
     */
    private ReplyHandler id2handler(Integer id) {
        return (ReplyHandler)_idMap.get(id);
    }

    public synchronized String toString() {
        //IMPORTANT: because_map.values() may not be defined (see note above),
        //we can only use _map.keySet().iterator()
        StringBuffer buf=new StringBuffer("{");
        Iterator iter=_map.keySet().iterator();
        while (iter.hasNext()) {
            Object key=iter.next();
            buf.append(key); // GUID
            buf.append("->");
            Integer id=(Integer)_map.get(key);
            ReplyHandler handler=id2handler(id);
            buf.append(handler==null ? "null" : handler.toString());//connection
            if (iter.hasNext())
                buf.append(", ");
        }
        buf.append("}");
        return buf.toString();
    }

//      /** Tests internal consistency.  VERY slow. */
//      private void repOk() {
//          //Check that _idMap is inverse of _handlerMap...
//          for (Iterator iter=_idMap.keySet().iterator(); iter.hasNext(); ) {
//              Integer key=(Integer)iter.next();
//              ReplyHandler value=(ReplyHandler)_idMap.get(key);
//              Assert.that(_handlerMap.get(value)==key);
//          }
//          //..and vice versa
//          for (Iterator iter=_handlerMap.keySet().iterator(); iter.hasNext(); ) {
//              ReplyHandler key=(ReplyHandler)iter.next();
//              Integer value=(Integer)_handlerMap.get(key);
//              Assert.that(_idMap.get(value)==key);
//          }
//      }

    /** Unit test */
    /*
    public static void main(String args[]) {
        RouteTable rt=null;
        byte[] g1=new byte[16]; g1[0]=(byte)1;
        byte[] g2=new byte[16]; g2[0]=(byte)2;
        byte[] g3=new byte[16]; g3[0]=(byte)3;
        byte[] g4=new byte[16]; g4[0]=(byte)4;
        ReplyHandler c1=new StubReplyHandler();
        ReplyHandler c2=new StubReplyHandler();
        ReplyHandler c3=new StubReplyHandler();
        ReplyHandler c4=new StubReplyHandler();

        //1. Test FIFO ability
        rt=new RouteTable(3);
        rt.routeReply(g1, c1);
        rt.routeReply(g2, c2);
        rt.routeReply(g3, c3);
        Assert.that(rt.getReplyHandler(g1)==c1);
        Assert.that(rt.getReplyHandler(g2)==c2);
        Assert.that(rt.getReplyHandler(g3)==c3);
        rt.routeReply(g4, c4);
        Assert.that(rt.getReplyHandler(g1)==null);
        Assert.that(rt.getReplyHandler(g2)==c2);
        Assert.that(rt.getReplyHandler(g3)==c3);
        Assert.that(rt.getReplyHandler(g4)==c4);
        rt.routeReply(g1, c1);
        Assert.that(rt.getReplyHandler(g1)==c1);
        Assert.that(rt.getReplyHandler(g2)==null);
        Assert.that(rt.getReplyHandler(g3)==c3);
        Assert.that(rt.getReplyHandler(g4)==c4);

        rt=new RouteTable(1);
        rt.routeReply(g1, c1);
        Assert.that(rt.getReplyHandler(g1)==c1);
        rt.routeReply(g2, c2);
        Assert.that(rt.getReplyHandler(g1)==null);
        Assert.that(rt.getReplyHandler(g2)==c2);
        rt.routeReply(g3, c3);
        Assert.that(rt.getReplyHandler(g1)==null);
        Assert.that(rt.getReplyHandler(g2)==null);
        Assert.that(rt.getReplyHandler(g3)==c3);

        rt=new RouteTable(2);
        rt.routeReply(g1,c1);
        rt.removeReplyHandler(c1);
        Assert.that(rt.getReplyHandler(g1)==null);

        //2. Test routing/re-routing abilities...with glass box tests.
        rt=new RouteTable(1000);
        rt._nextID=Integer.MAX_VALUE;  //test wrap-around
        Assert.that(rt.tryToRouteReply(g1, c1));         //g1->c1
        Assert.that(! rt.tryToRouteReply(g1, c2));
        Assert.that(rt.getReplyHandler(g1)==c1);

        Assert.that(rt.tryToRouteReply(g2, c2));         //g2->c2
        Assert.that(rt.getReplyHandler(g2)==c2);
        rt.routeReply(g2, c3);                           //g2->c3
        Assert.that(rt.getReplyHandler(g1)==c1);
        Assert.that(rt.getReplyHandler(g2)==c3);

        rt.removeReplyHandler(c1);                       //g1->null
        rt.removeReplyHandler(c3);                       //g2->null
        Assert.that(! rt.tryToRouteReply(g1, c1));   
        Assert.that(! rt.tryToRouteReply(g2, c3));
        Assert.that(rt.getReplyHandler(g1)==null);
        Assert.that(rt.getReplyHandler(g2)==null);
        Assert.that(rt._handlerMap.size()==1);           //g2 only
        Assert.that(rt._idMap.size()==1);

        //Test that _idMap/_handlerMap don't grow without bound.
        rt=new RouteTable(1000);
        Assert.that(rt.tryToRouteReply(g1, c1));
        Assert.that(rt.tryToRouteReply(g2, c1));
        Assert.that(rt.tryToRouteReply(g3, c1));
        Assert.that(rt.tryToRouteReply(g4, c1));
        Assert.that(! rt.tryToRouteReply(g4, c1));
        Assert.that(rt.getReplyHandler(g1)==c1);
        Assert.that(rt.getReplyHandler(g2)==c1);
        Assert.that(rt.getReplyHandler(g3)==c1);
        Assert.that(rt.getReplyHandler(g4)==c1);
        Assert.that(rt._handlerMap.size()==1);
        Assert.that(rt._idMap.size()==1);
    }

    private static class StubReplyHandler implements ReplyHandler {
        public boolean isOpen() {
            return true;
        }
        public void handlePingReply(PingReply pingReply, 
                                    ManagedConnection receivingConnection) {
        }
        public void handlePushRequest(PushRequest pushRequest, 
                                      ManagedConnection receivingConnection) {
        }
        public void handleQueryReply(QueryReply queryReply, 
                                     ManagedConnection receivingConnection) {
        }
    }
    */
}


