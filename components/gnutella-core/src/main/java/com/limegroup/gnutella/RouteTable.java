package com.limegroup.gnutella;

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
class RouteTable {
    /**
     * The obvious implementation of this class is a mapping from GUID to
     * ReplyHandler's.  The problem with this representation is it's hard to
     * implement removeReplyHandler efficiently.  You either have to keep the
     * references to the ReplyHandler (which wastes memory) or iterate through
     * the entire table to clean all references (which wastes time AND removes
     * valuable information for preventing duplicate queries).
     *
     * Instead we use a layer of indirection.  _newMap/_oldMap maps GUIDs to
     * Integers, which act as IDs for each connection.  _idMap maps IDs to
     * ReplyHandlers.  _handlerMap maps ReplyHandler to IDs.  So to clean up a
     * connection, we just purge the entries from _handlerMap and _idMap; there
     * is no need to iterate through the entire GUID mapping.  Adding GUIDs and
     * routing replies are still constant-time operations.
     *
     * IDs are allocated sequentially according with the nextID variable.  The
     * field does "wrap around" after reaching the maximum integer value.
     * Though no two open connections will have the same ID--we check
     * _idMap--there is a very low probability that an ID in _map could be
     * prematurely reused.
     *
     * To approximate FIFO behavior, we keep two sets around, _newMap and
     * _oldMap.  Every few seconds, when the system time is greater than
     * nextSwitch, we clear _oldMap and replace it with _newMap.
     * (DuplicateFilter uses the same trick.)  In this way, we remember the last
     * N to 2N minutes worth of GUIDs.  This is superior to a fixed size route
     * table.
     *
     * INVARIANT: keys of _newMap and _oldMap are disjoint
     * INVARIANT: _idMap and _replyMap are inverses
     *
     * TODO3: if IDs were stored in each ReplyHandler, we would not need
     *  _replyMap.  Better yet, if the values of _map were indices (with tags)
     *  into ConnectionManager._initialized[Client]Connections, we would not
     *  need _idMap either.  However, this increases dependenceies.  
     */
    private Map /* byte[] -> Integer */ _newMap=
        new TreeMap(new GUID.GUIDByteComparator());
    private Map /* byte[] -> Integer */ _oldMap=
        new TreeMap(new GUID.GUIDByteComparator());
    private int _mseconds;
    private long _nextSwitchTime;

    private Map /* Integer -> ReplyHandler */ _idMap=new HashMap();
    private Map /* ReplyHandler -> Integer */ _handlerMap=new HashMap();
    private int _nextID;

    /**
     * Creates a new route table with enough space to hold the last 
     * seconds to 2*seconds worth of data.
     */
    public RouteTable(int seconds) {
        this._mseconds=seconds*1000;
        this._nextSwitchTime=System.currentTimeMillis()+_mseconds;
    }

    /**
     * Adds a new routing entry.
     *
     * @requires guid and c are non-null, guid.length==16
     * @modifies this
     * @effects if replyHandler is open, adds the routing entry to this,
     *  replacing any routing entries for guid.  This has effect of 
     *  "renewing" guid.  Otherwise returns without modifying this.
     */
    public synchronized void routeReply(byte[] guid,
                                        ReplyHandler replyHandler) {
        repOk();
        purge();
        Assert.that(replyHandler != null);
        if (! replyHandler.isOpen())
            return;

        Integer id=handler2id(replyHandler);
        _oldMap.remove(guid);   //renews keys
        _newMap.put(guid, id);
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
        repOk();
        purge();
        Assert.that(replyHandler != null);
        Assert.that(guid!=null, "Null GUID in tryToRouteReply");

        if (! replyHandler.isOpen())
            return false;

        if(!_newMap.containsKey(guid) && !_oldMap.containsKey(guid)) {
            Integer id=handler2id(replyHandler);
            _newMap.put(guid, id);
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
        //no purge
        repOk();

        //Look up guid in _newMap. If not there, check _oldMap. 
        Integer id=(Integer)_newMap.get(guid);
        if (id==null)
            id=(Integer)_oldMap.get(guid);

        return (id==null) ? null : id2handler(id);  //may be null
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
        //no purge
        repOk();
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

    /**
     * Purges old entries.
     *
     * @modifies _nextSwitchTime, _newMap, _oldMap
     * @effects if the system time is less than _nextSwitchTime, returns
     *  false.  Otherwise, clears _oldMap and swaps _oldMap and _newMap,
     *  updates _nextSwitchTime, and returns true.
     */
    private final boolean purge() {
        long now=System.currentTimeMillis();
        if (now<_nextSwitchTime) 
            //not enough time has elapsed
            return false;

        //System.out.println(now+" "+this.hashCode()+" purging "
        //                   +_oldMap.size()+" old, "
        //                   +_newMap.size()+" new");
        _oldMap.clear();
        Map tmp=_oldMap;
        _oldMap=_newMap;
        _newMap=tmp;
        _nextSwitchTime=now+_mseconds;
        return true;
    }

    public synchronized String toString() {
        //Inefficient, but this is only for debugging anyway.
        StringBuffer buf=new StringBuffer("{");
        Map bothMaps=new TreeMap(new GUID.GUIDByteComparator());
        bothMaps.putAll(_oldMap);
        bothMaps.putAll(_newMap);

        Iterator iter=bothMaps.keySet().iterator();
        while (iter.hasNext()) {
            byte[] key=(byte[])iter.next();
            buf.append(new GUID(key)); // GUID
            buf.append("->");
            Integer id=(Integer)bothMaps.get(key);
            ReplyHandler handler=id2handler(id);
            buf.append(handler==null ? "null" : handler.toString());//connection
            if (iter.hasNext())
                buf.append(", ");
        }
        buf.append("}");
        return buf.toString();
    }

    /** Tests internal consistency.  VERY slow. */
    private final void repOk() {
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

//          //Check that keys of _newMap aren't in _oldMap
//          for (Iterator iter=_newMap.keySet().iterator(); iter.hasNext(); ) {
//              byte[] guid=(byte[])iter.next();
//              Assert.that(! _oldMap.containsKey(guid));
//          }

//          //Check that keys of _oldMap aren't in _newMap
//          for (Iterator iter=_oldMap.keySet().iterator(); iter.hasNext(); ) {
//              byte[] guid=(byte[])iter.next();
//              Assert.that(! _newMap.containsKey(guid));
//          }
    }

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

        //1. Test replacement policy (glass box).
        int MSECS=1000;
        rt=new RouteTable(MSECS/1000);
        rt.routeReply(g1, c1);
        rt.routeReply(g2, c2);                    //old, new:
        rt.routeReply(g3, c3);                    //{}, {g1, g2, g3}
        Assert.that(rt.getReplyHandler(g1)==c1);
        Assert.that(rt.getReplyHandler(g2)==c2);
        Assert.that(rt.getReplyHandler(g3)==c3);
        Assert.that(rt.getReplyHandler(g4)==null);
        try { Thread.sleep(MSECS); } catch (InterruptedException e) { }
        Assert.that(rt.tryToRouteReply(g4, c4));  //{g1, g2, g3}, {g4}
        Assert.that(rt.getReplyHandler(g1)==c1);
        rt.routeReply(g1, c1);                    //{g2, g3}, {g1, g4}
        Assert.that(rt.getReplyHandler(g1)==c1);
        Assert.that(rt.getReplyHandler(g2)==c2);
        Assert.that(rt.getReplyHandler(g3)==c3);
        Assert.that(rt.getReplyHandler(g4)==c4);
        try { Thread.sleep(MSECS); } catch (InterruptedException e) { }
        rt.routeReply(g2, c3);                     //{g1, g4}, {g2}
        System.out.println(rt.toString());
        Assert.that(rt.getReplyHandler(g1)==c1);
        Assert.that(rt.getReplyHandler(g2)==c3);
        Assert.that(rt.getReplyHandler(g3)==null);
        Assert.that(rt.getReplyHandler(g4)==c4);
        try { Thread.sleep(MSECS); } catch (InterruptedException e) { }
        Assert.that(! rt.tryToRouteReply(g2, c2));  //{g2}, {}}
        Assert.that(rt.getReplyHandler(g1)==null);
        Assert.that(rt.getReplyHandler(g2)==c3);
        Assert.that(rt.getReplyHandler(g3)==null);
        Assert.that(rt.getReplyHandler(g4)==null);
        rt.routeReply(g2, c2);                      //{}, {g2}
        Assert.that(rt.getReplyHandler(g1)==null);
        Assert.that(rt.getReplyHandler(g2)==c2);
        Assert.that(rt.getReplyHandler(g3)==null);
        Assert.that(rt.getReplyHandler(g4)==null);

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


