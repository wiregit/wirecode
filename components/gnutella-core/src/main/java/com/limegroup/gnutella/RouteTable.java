package com.limegroup.gnutella;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.search.ResultCounter;

/**
 * The reply routing table.  Given a GUID from a reply message header,
 * this tells you where to route the reply.  It is mutable mapping
 * from globally unique 16-byte message IDs to connections.  Old
 * mappings may be purged without warning, preferably using a FIFO
 * policy.  This class makes a distinction between not having a mapping
 * for a GUID and mapping that GUID to null (in the case of a removed
 * ReplyHandler).<p>
 *
 * This class can also optionally keep track of the number of reply bytes 
 * routed per guid.  This can be useful for implementing fair flow-control
 * strategies.
 */
public final class RouteTable {
    /**
     * The obvious implementation of this class is a mapping from GUID to
     * ReplyHandler's.  The problem with this representation is it's hard to
     * implement removeReplyHandler efficiently.  You either have to keep the
     * references to the ReplyHandler (which wastes memory) or iterate through
     * the entire table to clean all references (which wastes time AND removes
     * valuable information for preventing duplicate queries).
     *
     * Instead we use a layer of indirection.  _newMap/_oldMap maps GUIDs to
     * integers, which act as IDs for each connection.  _idMap maps IDs to
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
     * For flow-control reasons, we also store the number of bytes routed per
     * GUID in each table.  Hence the RouteTableEntry class.
     *
     * INVARIANT: keys of _newMap and _oldMap are disjoint
     * INVARIANT: _idMap and _replyMap are inverses
     *
     * TODO3: if IDs were stored in each ReplyHandler, we would not need
     *  _replyMap.  Better yet, if the values of _map were indices (with tags)
     *  into ConnectionManager._initialized[Client]Connections, we would not
     *  need _idMap either.  However, this increases dependenceies.  
     */
    private Map /* byte[] -> RouteTableEntry */ _newMap=
        new TreeMap(new GUID.GUIDByteComparator());
    private Map /* byte[] -> RouteTableEntry */ _oldMap=
        new TreeMap(new GUID.GUIDByteComparator());
    private int _mseconds;
    private long _nextSwitchTime;
    private int _maxSize;

    private Map /* Integer -> ReplyHandler */ _idMap=new HashMap();
    private Map /* ReplyHandler -> Integer */ _handlerMap=new HashMap();
    private int _nextID;

    /** Values stored in _newMap/_oldMap. */
    private static final class RouteTableEntry implements ResultCounter {
        /** The numericID of the reply connection. */
        private int handlerID;
        /** The bytes already routed for this GUID. */
        private int bytesRouted;
        /** The number of replies already routed for this GUID. */
        private int repliesRouted;
        /** The ttl associated with this RTE - meaningful only if > 0. */
        private byte ttl = 0;
        /** Creates a new entry for the given ID, with zero bytes routed. */
        RouteTableEntry(int handlerID) {
            this.handlerID = handlerID;
            this.bytesRouted = 0;
			this.repliesRouted = 0;
        }
		
        public void setTTL(byte ttl) { this.ttl = ttl; }
        public byte getTTL() { return ttl; }

		/** Accessor for the number of results for this entry. */
		public int getNumResults() { return repliesRouted; }
    }

    /**
     * Creates a new route table with enough space to hold the last seconds to
     * 2*seconds worth of entries, or maxSize elements, whichever is smaller
     * [sic].
     *
     * Typically maxSize is very large, and serves only as a guarantee to
     * prevent worst case behavior.  Actually 2*maxSize elements can be held in
     * this in the worst case.  
     */
    public RouteTable(int seconds, int maxSize) {
        this._mseconds=seconds*1000;
        this._nextSwitchTime=System.currentTimeMillis()+_mseconds;
        this._maxSize=maxSize;
    }

    /**
     * Adds a new routing entry.
     *
     * @requires guid and c are non-null, guid.length==16
     * @modifies this
     * @effects if replyHandler is open, adds the routing entry to this,
     *  replacing any routing entries for guid.  This has effect of 
     *  "renewing" guid.  Otherwise returns without modifying this.
	 *
	 * @return the <tt>RouteTableEntry</tt> entered into the routing 
	 *  tables, or <tt>null</tt> if it could not be entered
     */
    public synchronized ResultCounter routeReply(byte[] guid,
												 ReplyHandler replyHandler) {
        repOk();
        purge();
		if(replyHandler == null) {
			throw new NullPointerException("null reply handler");
		}

        if (! replyHandler.isOpen())
            return null;

        //First clear out any old entries for the guid, memorizing the volume
        //routed if found.  Note that if the guid is found in _newMap, we don't
        //need to look in _oldMap.
        int id=handler2id(replyHandler).intValue();
        RouteTableEntry entry=(RouteTableEntry)_newMap.remove(guid);
        if (entry==null)
            entry=(RouteTableEntry)_oldMap.remove(guid);

        //Now map the guid to the new reply handler, using the volume routed if
        //found, or zero otherwise.
        if (entry==null)
            entry=new RouteTableEntry(id);
        else
            entry.handlerID=id;            //avoids allocation
        _newMap.put(guid, entry);
		return entry;
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
    public synchronized ResultCounter tryToRouteReply(byte[] guid,
													  ReplyHandler replyHandler) {
        repOk();
        purge();
        Assert.that(replyHandler != null);
        Assert.that(guid!=null, "Null GUID in tryToRouteReply");

        if (! replyHandler.isOpen())
            return null;

        if(!_newMap.containsKey(guid) && !_oldMap.containsKey(guid)) {
            int id=handler2id(replyHandler).intValue();
			RouteTableEntry entry = new RouteTableEntry(id);
			_newMap.put(guid, entry);
            //_newMap.put(guid, new RouteTableEntry(id));
            return entry;
        } else {
            return null;
        }
    }

    /** Optional operation - if you want to remember the TTL associated with a
     *  message, in order to allow for extendable execution, you can set the TTL
     *  a message (guid).
     *  If the guid is not present (evicted or forgotten), this is a noop.
     *  @param ttl should be greater than 0.
     *  @exception IllegalArgumentException thrown if !(ttl > 0)
     */
    public synchronized void setTTL(byte[] guid, byte ttl) {
        if (!(ttl > 0))
            throw new IllegalArgumentException("Input TTL too small: " + ttl);

        //Look up guid in _newMap. If not there, check _oldMap. 
        RouteTableEntry entry=(RouteTableEntry)_newMap.get(guid);
        if (entry==null)
            entry=(RouteTableEntry)_oldMap.get(guid);

        if (entry==null)
            ; // nothing to do
        else
            entry.setTTL(ttl);
    }

    /** Optional accessor - use this if you used setTTL(byte[]) to remember the
     *  ttl of a message (guid).  If it returns 0, then it was never set.
     *  @return 0 if the guid was never remembered, >0 if so.
     */
    public synchronized byte getTTL(byte[] guid) {
        //Look up guid in _newMap. If not there, check _oldMap. 
        RouteTableEntry entry=(RouteTableEntry)_newMap.get(guid);
        if (entry==null)
            entry=(RouteTableEntry)_oldMap.get(guid);

        if (entry==null)
            return (byte) 0;
        else 
            return entry.getTTL();
    }

    /**
     * Looks up the reply route for a given guid.
     *
     * @requires guid.length==16
     * @effects returns the corresponding ReplyHandler for this GUID.     
     *  Returns null if no mapping for guid, or guid maps to null (i.e., 
     *  to a removed ReplyHandler.
     */
    public synchronized ReplyHandler getReplyHandler(byte[] guid) {        
        //no purge
        repOk();

        //Look up guid in _newMap. If not there, check _oldMap. 
        RouteTableEntry entry=(RouteTableEntry)_newMap.get(guid);
        if (entry==null)
            entry=(RouteTableEntry)_oldMap.get(guid);

        //Note that id2handler may return null.
        return (entry==null) ? null : id2handler(new Integer(entry.handlerID));
    }

    /**
     * Looks up the reply route and route volume for a given guid, incrementing
     * the count of bytes routed for that GUID.
     *
     * @requires guid.length==16
     * @effects if no mapping for guid, or guid maps to null (i.e., to a removed
     *  ReplyHandler) returns null.  Otherwise returns a tuple containing the
     *  corresponding ReplyHandler for this GUID along with the volume of
     *  messages already routed for that guid.  Afterwards, increments the reply
     *  count by replyBytes.
     */
    public synchronized ReplyRoutePair getReplyHandler(byte[] guid, 
                                                       int replyBytes,
													   short numReplies) {
        //no purge
        repOk();

        //Look up guid in _newMap. If not there, check _oldMap. 
        RouteTableEntry entry=(RouteTableEntry)_newMap.get(guid);
        if (entry==null)
            entry=(RouteTableEntry)_oldMap.get(guid);
        
        //If no mapping for guid, or guid maps to a removed reply handler,
        //return null.
        if (entry==null)
            return null;
        ReplyHandler handler=id2handler(new Integer(entry.handlerID));
        if (handler==null)
            return null;
            
        //Increment count, returning old count in tuple.
        ReplyRoutePair ret=new ReplyRoutePair(handler, entry.bytesRouted);

        entry.bytesRouted += replyBytes;
        entry.repliesRouted += numReplies;
        return ret;
    }

    /** The return value from getReplyHandler. */
    public static class ReplyRoutePair {
        private final ReplyHandler handler;
        private final int volume;

        ReplyRoutePair(ReplyHandler handler, int volume) {
            this.handler = handler;
            this.volume = volume;
        }
        /** Returns the ReplyHandler to route your message */
        public ReplyHandler getReplyHandler() { return handler; }
        /** Returns the volume of messages already routed for the given GUID. */
        public int getBytesRouted() { return volume; }
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
        //Also, handler2id may actually allocate a new ID for replyHandler, when
        //killing a connection for which we've routed no replies.  That's ok;
        //we'll just clean up the new ID immediately.
        Integer id=handler2id(replyHandler);
        _idMap.remove(id);
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
        if (now<_nextSwitchTime && _newMap.size()<_maxSize) 
            //not enough time has elapsed and sets too small
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
            int id=((RouteTableEntry)bothMaps.get(key)).handlerID;
            ReplyHandler handler=id2handler(new Integer(id));
            buf.append(handler==null ? "null" : handler.toString());//connection
            if (iter.hasNext())
                buf.append(", ");
        }
        buf.append("}");
        return buf.toString();
    }

    private static boolean warned=false;
    /** Tests internal consistency.  VERY slow. */
    private final void repOk() {
        /*
        if (!warned) {
            System.err.println(
                "WARNING: RouteTable.repOk enabled.  Expect performance problems!");
            warned=true;
        }

        //Check that _idMap is inverse of _handlerMap...
        for (Iterator iter=_idMap.keySet().iterator(); iter.hasNext(); ) {
            Integer key=(Integer)iter.next();
            ReplyHandler value=(ReplyHandler)_idMap.get(key);
            Assert.that(_handlerMap.get(value)==key);
        }
        //..and vice versa
        for (Iterator iter=_handlerMap.keySet().iterator(); iter.hasNext(); ) {
            ReplyHandler key=(ReplyHandler)iter.next();
            Integer value=(Integer)_handlerMap.get(key);
            Assert.that(_idMap.get(value)==key);
        }
        
        //Check that keys of _newMap aren't in _oldMap, values are RouteTableEntry
        for (Iterator iter=_newMap.keySet().iterator(); iter.hasNext(); ) {
            byte[] guid=(byte[])iter.next();
            Assert.that(! _oldMap.containsKey(guid));
            Assert.that(_newMap.get(guid) instanceof RouteTableEntry);
        }
        
        //Check that keys of _oldMap aren't in _newMap
        for (Iterator iter=_oldMap.keySet().iterator(); iter.hasNext(); ) {
            byte[] guid=(byte[])iter.next();
            Assert.that(! _newMap.containsKey(guid));
            Assert.that(_oldMap.get(guid) instanceof RouteTableEntry);
        }
        */
    }

}


