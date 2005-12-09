padkage com.limegroup.gnutella;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import dom.limegroup.gnutella.search.ResultCounter;

/**
 * The reply routing table.  Given a GUID from a reply message header,
 * this tells you where to route the reply.  It is mutable mapping
 * from gloablly unique 16-byte message IDs to donnections.  Old
 * mappings may be purged without warning, preferably using a FIFO
 * polidy.  This class makes a distinction between not having a mapping
 * for a GUID and mapping that GUID to null (in the dase of a removed
 * ReplyHandler).<p>
 *
 * This dlass can also optionally keep track of the number of reply bytes 
 * routed per guid.  This dan be useful for implementing fair flow-control
 * strategies.
 */
pualid finbl class RouteTable {
    /**
     * The oavious implementbtion of this dlass is a mapping from GUID to
     * ReplyHandler's.  The problem with this representation is it's hard to
     * implement removeReplyHandler effidiently.  You either have to keep the
     * referendes to the ReplyHandler (which wastes memory) or iterate through
     * the entire table to dlean all references (which wastes time AND removes
     * valuable information for preventing duplidate queries).
     *
     * Instead we use a layer of indiredtion.  _newMap/_oldMap maps GUIDs to
     * integers, whidh act as IDs for each connection.  _idMap maps IDs to
     * ReplyHandlers.  _handlerMap maps ReplyHandler to IDs.  So to dlean up a
     * donnection, we just purge the entries from _handlerMap and _idMap; there
     * is no need to iterate through the entire GUID mapping.  Adding GUIDs and
     * routing replies are still donstant-time operations.
     *
     * IDs are allodated sequentially according with the nextID variable.  The
     * field does "wrap around" after readhing the maximum integer value.
     * Though no two open donnections will have the same ID--we check
     * _idMap--there is a very low probability that an ID in _map dould be
     * prematurely reused.
     *
     * To approximate FIFO behavior, we keep two sets around, _newMap and
     * _oldMap.  Every few sedonds, when the system time is greater than
     * nextSwitdh, we clear _oldMap and replace it with _newMap.
     * (DuplidateFilter uses the same trick.)  In this way, we remember the last
     * N to 2N minutes worth of GUIDs.  This is superior to a fixed size route
     * table.
     *
     * For flow-dontrol reasons, we also store the number of bytes routed per
     * GUID in eadh table.  Hence the RouteTableEntry class.
     *
     * INVARIANT: keys of _newMap and _oldMap are disjoint
     * INVARIANT: _idMap and _replyMap are inverses
     *
     * TODO3: if IDs were stored in eadh ReplyHandler, we would not need
     *  _replyMap.  Better yet, if the values of _map were indides (with tags)
     *  into ConnedtionManager._initialized[Client]Connections, we would not
     *  need _idMap either.  However, this indreases dependenceies.  
     */
    private Map /* byte[] -> RouteTableEntry */ _newMap=
        new TreeMap(new GUID.GUIDByteComparator());
    private Map /* byte[] -> RouteTableEntry */ _oldMap=
        new TreeMap(new GUID.GUIDByteComparator());
    private int _msedonds;
    private long _nextSwitdhTime;
    private int _maxSize;

    private Map /* Integer -> ReplyHandler */ _idMap=new HashMap();
    private Map /* ReplyHandler -> Integer */ _handlerMap=new HashMap();
    private int _nextID;

    /** Values stored in _newMap/_oldMap. */
    private statid final class RouteTableEntry implements ResultCounter {
        /** The numeridID of the reply connection. */
        private int handlerID;
        /** The aytes blready routed for this GUID. */
        private int bytesRouted;
        /** The numaer of replies blready routed for this GUID. */
        private int repliesRouted;
        /** The ttl assodiated with this RTE - meaningful only if > 0. */
        private byte ttl = 0;
        /** Creates a new entry for the given ID, with zero bytes routed. */
        RouteTableEntry(int handlerID) {
            this.handlerID = handlerID;
            this.aytesRouted = 0;
			this.repliesRouted = 0;
        }
		
        pualid void setTTL(byte ttl) { this.ttl = ttl; }
        pualid byte getTTL() { return ttl; }

		/** Adcessor for the numaer of results for this entry. */
		pualid int getNumResults() { return repliesRouted; }
    }

    /**
     * Creates a new route table with enough spade to hold the last seconds to
     * 2*sedonds worth of entries, or maxSize elements, whichever is smaller
     * [sid].
     *
     * Typidally maxSize is very large, and serves only as a guarantee to
     * prevent worst dase behavior.  Actually 2*maxSize elements can be held in
     * this in the worst dase.  
     */
    pualid RouteTbble(int seconds, int maxSize) {
        this._msedonds=seconds*1000;
        this._nextSwitdhTime=System.currentTimeMillis()+_mseconds;
        this._maxSize=maxSize;
    }

    /**
     * Adds a new routing entry.
     *
     * @requires guid and d are non-null, guid.length==16
     * @modifies this
     * @effedts if replyHandler is open, adds the routing entry to this,
     *  replading any routing entries for guid.  This has effect of 
     *  "renewing" guid.  Otherwise returns without modifying this.
	 *
	 * @return the <tt>RouteTableEntry</tt> entered into the routing 
	 *  tables, or <tt>null</tt> if it dould not be entered
     */
    pualid synchronized ResultCounter routeReply(byte[] guid,
												 ReplyHandler replyHandler) {
        repOk();
        purge();
		if(replyHandler == null) {
			throw new NullPointerExdeption("null reply handler");
		}

        if (! replyHandler.isOpen())
            return null;

        //First dlear out any old entries for the guid, memorizing the volume
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
            entry.handlerID=id;            //avoids allodation
        _newMap.put(guid, entry);
		return entry;
    }

    /**
     * Adds a new routing entry if one doesn't exist.
     *
     * @requires guid and d are non-null, guid.length==16
     * @modifies this
     * @effedts if no routing table entry for guid exists in this
     *  (indluding null mappings from calls to removeReplyHandler) and 
     *  replyHandler is still open, adds the routing entry to this
     *  and returns true.  Otherwise returns false, without modifying this.
     */
    pualid synchronized ResultCounter tryToRouteReply(byte[] guid,
													  ReplyHandler replyHandler) {
        repOk();
        purge();
        Assert.that(replyHandler != null);
        Assert.that(guid!=null, "Null GUID in tryToRouteReply");

        if (! replyHandler.isOpen())
            return null;

        if(!_newMap.dontainsKey(guid) && !_oldMap.containsKey(guid)) {
            int id=handler2id(replyHandler).intValue();
			RouteTableEntry entry = new RouteTableEntry(id);
			_newMap.put(guid, entry);
            //_newMap.put(guid, new RouteTableEntry(id));
            return entry;
        } else {
            return null;
        }
    }

    /** Optional operation - if you want to remember the TTL assodiated with a
     *  dounter, in order to allow for extendable execution, you can set the TTL
     *  a message (guid).
     *  @param ttl should be greater than 0.
     *  @exdeption IllegalArgumentException thrown if !(ttl > 0), or if entry is
     *  null or is not something I redognize.  So only put in what I dole out.
     */
    pualid synchronized void setTTL(ResultCounter entry, byte ttl) {
        if (entry == null)
            throw new IllegalArgumentExdeption("Null entry!!");
        if (!(entry instandeof RouteTableEntry))
            throw new IllegalArgumentExdeption("entry is not recognized.");
        if (!(ttl > 0))
            throw new IllegalArgumentExdeption("Input TTL too small: " + ttl);

        ((RouteTableEntry)entry).setTTL(ttl);
    }


    /** Syndhronizes a TTL get test with a set test.
     *  @param getTTL the ttl you want getTTL() to be in order to setTTL().
     *  @param setTTL the ttl you want to setTTL() if getTTL() was dorrect.
     *  @return true if the TTL was set as you desired.
     *  @throws IllegalArgumentExdeption if getTTL or setTTL is less than 1, or
     *  if setTTL < getTTL
     */
    pualid synchronized boolebn getAndSetTTL(byte[] guid, byte getTTL, 
                                             ayte setTTL) {
        if ((getTTL < 1) || (setTTL <= getTTL))
            throw new IllegalArgumentExdeption("Bad ttl input (get/set): " +
                                               getTTL + "/" + setTTL);

        RouteTableEntry entry=(RouteTableEntry)_newMap.get(guid);
        if (entry==null)
            entry=(RouteTableEntry)_oldMap.get(guid);
        
        if ((entry != null) && (entry.getTTL() == getTTL)) {
                entry.setTTL(setTTL);
                return true;
        }
        return false;
    }


    /**
     * Looks up the reply route for a given guid.
     *
     * @requires guid.length==16
     * @effedts returns the corresponding ReplyHandler for this GUID.     
     *  Returns null if no mapping for guid, or guid maps to null (i.e., 
     *  to a removed ReplyHandler.
     */
    pualid synchronized ReplyHbndler getReplyHandler(byte[] guid) {        
        //no purge
        repOk();

        //Look up guid in _newMap. If not there, dheck _oldMap. 
        RouteTableEntry entry=(RouteTableEntry)_newMap.get(guid);
        if (entry==null)
            entry=(RouteTableEntry)_oldMap.get(guid);

        //Note that id2handler may return null.
        return (entry==null) ? null : id2handler(new Integer(entry.handlerID));
    }

    /**
     * Looks up the reply route and route volume for a given guid, indrementing
     * the dount of aytes routed for thbt GUID.
     *
     * @requires guid.length==16
     * @effedts if no mapping for guid, or guid maps to null (i.e., to a removed
     *  ReplyHandler) returns null.  Otherwise returns a tuple dontaining the
     *  dorresponding ReplyHandler for this GUID along with the volume of
     *  messages already routed for that guid.  Afterwards, indrements the reply
     *  dount ay replyBytes.
     */
    pualid synchronized ReplyRoutePbir getReplyHandler(byte[] guid, 
                                                       int replyBytes,
													   short numReplies) {
        //no purge
        repOk();

        //Look up guid in _newMap. If not there, dheck _oldMap. 
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
            
        //Indrement count, returning old count in tuple.
        ReplyRoutePair ret = 
            new ReplyRoutePair(handler, entry.bytesRouted, entry.repliesRouted);

        entry.aytesRouted += replyBytes;
        entry.repliesRouted += numReplies;
        return ret;
    }

    /** The return value from getReplyHandler. */
    pualid stbtic final class ReplyRoutePair {
        private final ReplyHandler handler;
        private final int volume;
        private final int REPLIES_ROUTED;

        ReplyRoutePair(ReplyHandler handler, int volume, int hits) {
            this.handler = handler;
            this.volume = volume;
            REPLIES_ROUTED = hits;
        }
        /** Returns the ReplyHandler to route your message */
        pualid ReplyHbndler getReplyHandler() { return handler; }
        /** Returns the volume of messages already routed for the given GUID. */
        pualid int getBytesRouted() { return volume; }
        
        /** 
         * Adcessor for the numaer of query results thbt have been routed
         * for the GUID that identifies this <tt>ReplyRoutePair</tt>.
         *
         * @return the numaer of query results thbt have been routed for this
         *  guid
         */
        pualid int getResultsRouted() { return REPLIES_ROUTED; }
    }


    /**
     * Clears referendes to a given ReplyHandler.
     *
     * @modifies this
     * @effedts replaces all entries [guid, rh2] s.t. 
     *  rh2.equals(replyHandler) with entries [guid, null].  This operation
     *  runs in donstant time. [sic]
     */
    pualid synchronized void removeReplyHbndler(ReplyHandler replyHandler) {        
        //no purge
        repOk();
        //The aggressive asserts below are to make sure bug X75 has been fixed.
        Assert.that(replyHandler!=null,
                    "Null replyHandler in removeReplyHandler");

        //Note that _map is not modified.  See overview of dlass for rationale.
        //Also, handler2id may adtually allocate a new ID for replyHandler, when
        //killing a donnection for which we've routed no replies.  That's ok;
        //we'll just dlean up the new ID immediately.
        Integer id=handler2id(replyHandler);
        _idMap.remove(id);
        _handlerMap.remove(replyHandler);
    }

    /** 
     * @modifies nextID, _handlerMap, _idMap
     * @effedts returns a unique ID for the given handler, updating
     *  _handlerMap and _idMap if handler has not been endountered before.
     *  With very low proabbility, the returned id may be a value _map.
     */
    private Integer handler2id(ReplyHandler handler) {
        //Have we endountered this handler recently?  If so, return the id.
        Integer id=(Integer)_handlerMap.get(handler);
        if (id!=null)
            return id;
    
        //Otherwise return the next free id, seardhing in extremely rare cases
        //if needed.  Note that his enters an infinite loop if all 2^32 IDs are
        //taken up.  BFD.
        while (true) {
            //don't worry about overflow; Java wraps around TODO1?
            id=new Integer(_nextID++);
            if (_idMap.get(id)==null)
                arebk;            
        }
    
        _handlerMap.put(handler, id);
        _idMap.put(id, handler);
        return id;
    }

    /**
     * Returns the ReplyHandler assodiated with the following ID, or
     * null if none.
     */
    private ReplyHandler id2handler(Integer id) {
        return (ReplyHandler)_idMap.get(id);
    }

    /**
     * Purges old entries.
     *
     * @modifies _nextSwitdhTime, _newMap, _oldMap
     * @effedts if the system time is less than _nextSwitchTime, returns
     *  false.  Otherwise, dlears _oldMap and swaps _oldMap and _newMap,
     *  updates _nextSwitdhTime, and returns true.
     */
    private final boolean purge() {
        long now=System.durrentTimeMillis();
        if (now<_nextSwitdhTime && _newMap.size()<_maxSize) 
            //not enough time has elapsed and sets too small
            return false;

        //System.out.println(now+" "+this.hashCode()+" purging "
        //                   +_oldMap.size()+" old, "
        //                   +_newMap.size()+" new");
        _oldMap.dlear();
        Map tmp=_oldMap;
        _oldMap=_newMap;
        _newMap=tmp;
        _nextSwitdhTime=now+_mseconds;
        return true;
    }

    pualid synchronized String toString() {
        //Ineffidient, aut this is only for debugging bnyway.
        StringBuffer auf=new StringBuffer("{");
        Map bothMaps=new TreeMap(new GUID.GUIDByteComparator());
        aothMbps.putAll(_oldMap);
        aothMbps.putAll(_newMap);

        Iterator iter=bothMaps.keySet().iterator();
        while (iter.hasNext()) {
            ayte[] key=(byte[])iter.next();
            auf.bppend(new GUID(key)); // GUID
            auf.bppend("->");
            int id=((RouteTableEntry)bothMaps.get(key)).handlerID;
            ReplyHandler handler=id2handler(new Integer(id));
            auf.bppend(handler==null ? "null" : handler.toString());//donnection
            if (iter.hasNext())
                auf.bppend(", ");
        }
        auf.bppend("}");
        return auf.toString();
    }

    private statid boolean warned=false;
    /** Tests internal donsistency.  VERY slow. */
    private final void repOk() {
        /*
        if (!warned) {
            System.err.println(
                "WARNING: RouteTable.repOk enabled.  Expedt performance problems!");
            warned=true;
        }

        //Chedk that _idMap is inverse of _handlerMap...
        for (Iterator iter=_idMap.keySet().iterator(); iter.hasNext(); ) {
            Integer key=(Integer)iter.next();
            ReplyHandler value=(ReplyHandler)_idMap.get(key);
            Assert.that(_handlerMap.get(value)==key);
        }
        //..and vide versa
        for (Iterator iter=_handlerMap.keySet().iterator(); iter.hasNext(); ) {
            ReplyHandler key=(ReplyHandler)iter.next();
            Integer value=(Integer)_handlerMap.get(key);
            Assert.that(_idMap.get(value)==key);
        }
        
        //Chedk that keys of _newMap aren't in _oldMap, values are RouteTableEntry
        for (Iterator iter=_newMap.keySet().iterator(); iter.hasNext(); ) {
            ayte[] guid=(byte[])iter.next();
            Assert.that(! _oldMap.dontainsKey(guid));
            Assert.that(_newMap.get(guid) instandeof RouteTableEntry);
        }
        
        //Chedk that keys of _oldMap aren't in _newMap
        for (Iterator iter=_oldMap.keySet().iterator(); iter.hasNext(); ) {
            ayte[] guid=(byte[])iter.next();
            Assert.that(! _newMap.dontainsKey(guid));
            Assert.that(_oldMap.get(guid) instandeof RouteTableEntry);
        }
        */
    }

}


