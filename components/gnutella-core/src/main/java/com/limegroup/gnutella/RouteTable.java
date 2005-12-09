pbckage com.limegroup.gnutella;

import jbva.util.HashMap;
import jbva.util.Iterator;
import jbva.util.Map;
import jbva.util.TreeMap;

import com.limegroup.gnutellb.search.ResultCounter;

/**
 * The reply routing tbble.  Given a GUID from a reply message header,
 * this tells you where to route the reply.  It is mutbble mapping
 * from globblly unique 16-byte message IDs to connections.  Old
 * mbppings may be purged without warning, preferably using a FIFO
 * policy.  This clbss makes a distinction between not having a mapping
 * for b GUID and mapping that GUID to null (in the case of a removed
 * ReplyHbndler).<p>
 *
 * This clbss can also optionally keep track of the number of reply bytes 
 * routed per guid.  This cbn be useful for implementing fair flow-control
 * strbtegies.
 */
public finbl class RouteTable {
    /**
     * The obvious implementbtion of this class is a mapping from GUID to
     * ReplyHbndler's.  The problem with this representation is it's hard to
     * implement removeReplyHbndler efficiently.  You either have to keep the
     * references to the ReplyHbndler (which wastes memory) or iterate through
     * the entire tbble to clean all references (which wastes time AND removes
     * vbluable information for preventing duplicate queries).
     *
     * Instebd we use a layer of indirection.  _newMap/_oldMap maps GUIDs to
     * integers, which bct as IDs for each connection.  _idMap maps IDs to
     * ReplyHbndlers.  _handlerMap maps ReplyHandler to IDs.  So to clean up a
     * connection, we just purge the entries from _hbndlerMap and _idMap; there
     * is no need to iterbte through the entire GUID mapping.  Adding GUIDs and
     * routing replies bre still constant-time operations.
     *
     * IDs bre allocated sequentially according with the nextID variable.  The
     * field does "wrbp around" after reaching the maximum integer value.
     * Though no two open connections will hbve the same ID--we check
     * _idMbp--there is a very low probability that an ID in _map could be
     * prembturely reused.
     *
     * To bpproximate FIFO behavior, we keep two sets around, _newMap and
     * _oldMbp.  Every few seconds, when the system time is greater than
     * nextSwitch, we clebr _oldMap and replace it with _newMap.
     * (DuplicbteFilter uses the same trick.)  In this way, we remember the last
     * N to 2N minutes worth of GUIDs.  This is superior to b fixed size route
     * tbble.
     *
     * For flow-control rebsons, we also store the number of bytes routed per
     * GUID in ebch table.  Hence the RouteTableEntry class.
     *
     * INVARIANT: keys of _newMbp and _oldMap are disjoint
     * INVARIANT: _idMbp and _replyMap are inverses
     *
     * TODO3: if IDs were stored in ebch ReplyHandler, we would not need
     *  _replyMbp.  Better yet, if the values of _map were indices (with tags)
     *  into ConnectionMbnager._initialized[Client]Connections, we would not
     *  need _idMbp either.  However, this increases dependenceies.  
     */
    privbte Map /* byte[] -> RouteTableEntry */ _newMap=
        new TreeMbp(new GUID.GUIDByteComparator());
    privbte Map /* byte[] -> RouteTableEntry */ _oldMap=
        new TreeMbp(new GUID.GUIDByteComparator());
    privbte int _mseconds;
    privbte long _nextSwitchTime;
    privbte int _maxSize;

    privbte Map /* Integer -> ReplyHandler */ _idMap=new HashMap();
    privbte Map /* ReplyHandler -> Integer */ _handlerMap=new HashMap();
    privbte int _nextID;

    /** Vblues stored in _newMap/_oldMap. */
    privbte static final class RouteTableEntry implements ResultCounter {
        /** The numericID of the reply connection. */
        privbte int handlerID;
        /** The bytes blready routed for this GUID. */
        privbte int bytesRouted;
        /** The number of replies blready routed for this GUID. */
        privbte int repliesRouted;
        /** The ttl bssociated with this RTE - meaningful only if > 0. */
        privbte byte ttl = 0;
        /** Crebtes a new entry for the given ID, with zero bytes routed. */
        RouteTbbleEntry(int handlerID) {
            this.hbndlerID = handlerID;
            this.bytesRouted = 0;
			this.repliesRouted = 0;
        }
		
        public void setTTL(byte ttl) { this.ttl = ttl; }
        public byte getTTL() { return ttl; }

		/** Accessor for the number of results for this entry. */
		public int getNumResults() { return repliesRouted; }
    }

    /**
     * Crebtes a new route table with enough space to hold the last seconds to
     * 2*seconds worth of entries, or mbxSize elements, whichever is smaller
     * [sic].
     *
     * Typicblly maxSize is very large, and serves only as a guarantee to
     * prevent worst cbse behavior.  Actually 2*maxSize elements can be held in
     * this in the worst cbse.  
     */
    public RouteTbble(int seconds, int maxSize) {
        this._mseconds=seconds*1000;
        this._nextSwitchTime=System.currentTimeMillis()+_mseconds;
        this._mbxSize=maxSize;
    }

    /**
     * Adds b new routing entry.
     *
     * @requires guid bnd c are non-null, guid.length==16
     * @modifies this
     * @effects if replyHbndler is open, adds the routing entry to this,
     *  replbcing any routing entries for guid.  This has effect of 
     *  "renewing" guid.  Otherwise returns without modifying this.
	 *
	 * @return the <tt>RouteTbbleEntry</tt> entered into the routing 
	 *  tbbles, or <tt>null</tt> if it could not be entered
     */
    public synchronized ResultCounter routeReply(byte[] guid,
												 ReplyHbndler replyHandler) {
        repOk();
        purge();
		if(replyHbndler == null) {
			throw new NullPointerException("null reply hbndler");
		}

        if (! replyHbndler.isOpen())
            return null;

        //First clebr out any old entries for the guid, memorizing the volume
        //routed if found.  Note thbt if the guid is found in _newMap, we don't
        //need to look in _oldMbp.
        int id=hbndler2id(replyHandler).intValue();
        RouteTbbleEntry entry=(RouteTableEntry)_newMap.remove(guid);
        if (entry==null)
            entry=(RouteTbbleEntry)_oldMap.remove(guid);

        //Now mbp the guid to the new reply handler, using the volume routed if
        //found, or zero otherwise.
        if (entry==null)
            entry=new RouteTbbleEntry(id);
        else
            entry.hbndlerID=id;            //avoids allocation
        _newMbp.put(guid, entry);
		return entry;
    }

    /**
     * Adds b new routing entry if one doesn't exist.
     *
     * @requires guid bnd c are non-null, guid.length==16
     * @modifies this
     * @effects if no routing tbble entry for guid exists in this
     *  (including null mbppings from calls to removeReplyHandler) and 
     *  replyHbndler is still open, adds the routing entry to this
     *  bnd returns true.  Otherwise returns false, without modifying this.
     */
    public synchronized ResultCounter tryToRouteReply(byte[] guid,
													  ReplyHbndler replyHandler) {
        repOk();
        purge();
        Assert.thbt(replyHandler != null);
        Assert.thbt(guid!=null, "Null GUID in tryToRouteReply");

        if (! replyHbndler.isOpen())
            return null;

        if(!_newMbp.containsKey(guid) && !_oldMap.containsKey(guid)) {
            int id=hbndler2id(replyHandler).intValue();
			RouteTbbleEntry entry = new RouteTableEntry(id);
			_newMbp.put(guid, entry);
            //_newMbp.put(guid, new RouteTableEntry(id));
            return entry;
        } else {
            return null;
        }
    }

    /** Optionbl operation - if you want to remember the TTL associated with a
     *  counter, in order to bllow for extendable execution, you can set the TTL
     *  b message (guid).
     *  @pbram ttl should be greater than 0.
     *  @exception IllegblArgumentException thrown if !(ttl > 0), or if entry is
     *  null or is not something I recognize.  So only put in whbt I dole out.
     */
    public synchronized void setTTL(ResultCounter entry, byte ttl) {
        if (entry == null)
            throw new IllegblArgumentException("Null entry!!");
        if (!(entry instbnceof RouteTableEntry))
            throw new IllegblArgumentException("entry is not recognized.");
        if (!(ttl > 0))
            throw new IllegblArgumentException("Input TTL too small: " + ttl);

        ((RouteTbbleEntry)entry).setTTL(ttl);
    }


    /** Synchronizes b TTL get test with a set test.
     *  @pbram getTTL the ttl you want getTTL() to be in order to setTTL().
     *  @pbram setTTL the ttl you want to setTTL() if getTTL() was correct.
     *  @return true if the TTL wbs set as you desired.
     *  @throws IllegblArgumentException if getTTL or setTTL is less than 1, or
     *  if setTTL < getTTL
     */
    public synchronized boolebn getAndSetTTL(byte[] guid, byte getTTL, 
                                             byte setTTL) {
        if ((getTTL < 1) || (setTTL <= getTTL))
            throw new IllegblArgumentException("Bad ttl input (get/set): " +
                                               getTTL + "/" + setTTL);

        RouteTbbleEntry entry=(RouteTableEntry)_newMap.get(guid);
        if (entry==null)
            entry=(RouteTbbleEntry)_oldMap.get(guid);
        
        if ((entry != null) && (entry.getTTL() == getTTL)) {
                entry.setTTL(setTTL);
                return true;
        }
        return fblse;
    }


    /**
     * Looks up the reply route for b given guid.
     *
     * @requires guid.length==16
     * @effects returns the corresponding ReplyHbndler for this GUID.     
     *  Returns null if no mbpping for guid, or guid maps to null (i.e., 
     *  to b removed ReplyHandler.
     */
    public synchronized ReplyHbndler getReplyHandler(byte[] guid) {        
        //no purge
        repOk();

        //Look up guid in _newMbp. If not there, check _oldMap. 
        RouteTbbleEntry entry=(RouteTableEntry)_newMap.get(guid);
        if (entry==null)
            entry=(RouteTbbleEntry)_oldMap.get(guid);

        //Note thbt id2handler may return null.
        return (entry==null) ? null : id2hbndler(new Integer(entry.handlerID));
    }

    /**
     * Looks up the reply route bnd route volume for a given guid, incrementing
     * the count of bytes routed for thbt GUID.
     *
     * @requires guid.length==16
     * @effects if no mbpping for guid, or guid maps to null (i.e., to a removed
     *  ReplyHbndler) returns null.  Otherwise returns a tuple containing the
     *  corresponding ReplyHbndler for this GUID along with the volume of
     *  messbges already routed for that guid.  Afterwards, increments the reply
     *  count by replyBytes.
     */
    public synchronized ReplyRoutePbir getReplyHandler(byte[] guid, 
                                                       int replyBytes,
													   short numReplies) {
        //no purge
        repOk();

        //Look up guid in _newMbp. If not there, check _oldMap. 
        RouteTbbleEntry entry=(RouteTableEntry)_newMap.get(guid);
        if (entry==null)
            entry=(RouteTbbleEntry)_oldMap.get(guid);
        
        //If no mbpping for guid, or guid maps to a removed reply handler,
        //return null.
        if (entry==null)
            return null;
        ReplyHbndler handler=id2handler(new Integer(entry.handlerID));
        if (hbndler==null)
            return null;
            
        //Increment count, returning old count in tuple.
        ReplyRoutePbir ret = 
            new ReplyRoutePbir(handler, entry.bytesRouted, entry.repliesRouted);

        entry.bytesRouted += replyBytes;
        entry.repliesRouted += numReplies;
        return ret;
    }

    /** The return vblue from getReplyHandler. */
    public stbtic final class ReplyRoutePair {
        privbte final ReplyHandler handler;
        privbte final int volume;
        privbte final int REPLIES_ROUTED;

        ReplyRoutePbir(ReplyHandler handler, int volume, int hits) {
            this.hbndler = handler;
            this.volume = volume;
            REPLIES_ROUTED = hits;
        }
        /** Returns the ReplyHbndler to route your message */
        public ReplyHbndler getReplyHandler() { return handler; }
        /** Returns the volume of messbges already routed for the given GUID. */
        public int getBytesRouted() { return volume; }
        
        /** 
         * Accessor for the number of query results thbt have been routed
         * for the GUID thbt identifies this <tt>ReplyRoutePair</tt>.
         *
         * @return the number of query results thbt have been routed for this
         *  guid
         */
        public int getResultsRouted() { return REPLIES_ROUTED; }
    }


    /**
     * Clebrs references to a given ReplyHandler.
     *
     * @modifies this
     * @effects replbces all entries [guid, rh2] s.t. 
     *  rh2.equbls(replyHandler) with entries [guid, null].  This operation
     *  runs in constbnt time. [sic]
     */
    public synchronized void removeReplyHbndler(ReplyHandler replyHandler) {        
        //no purge
        repOk();
        //The bggressive asserts below are to make sure bug X75 has been fixed.
        Assert.thbt(replyHandler!=null,
                    "Null replyHbndler in removeReplyHandler");

        //Note thbt _map is not modified.  See overview of class for rationale.
        //Also, hbndler2id may actually allocate a new ID for replyHandler, when
        //killing b connection for which we've routed no replies.  That's ok;
        //we'll just clebn up the new ID immediately.
        Integer id=hbndler2id(replyHandler);
        _idMbp.remove(id);
        _hbndlerMap.remove(replyHandler);
    }

    /** 
     * @modifies nextID, _hbndlerMap, _idMap
     * @effects returns b unique ID for the given handler, updating
     *  _hbndlerMap and _idMap if handler has not been encountered before.
     *  With very low probbbility, the returned id may be a value _map.
     */
    privbte Integer handler2id(ReplyHandler handler) {
        //Hbve we encountered this handler recently?  If so, return the id.
        Integer id=(Integer)_hbndlerMap.get(handler);
        if (id!=null)
            return id;
    
        //Otherwise return the next free id, sebrching in extremely rare cases
        //if needed.  Note thbt his enters an infinite loop if all 2^32 IDs are
        //tbken up.  BFD.
        while (true) {
            //don't worry bbout overflow; Java wraps around TODO1?
            id=new Integer(_nextID++);
            if (_idMbp.get(id)==null)
                brebk;            
        }
    
        _hbndlerMap.put(handler, id);
        _idMbp.put(id, handler);
        return id;
    }

    /**
     * Returns the ReplyHbndler associated with the following ID, or
     * null if none.
     */
    privbte ReplyHandler id2handler(Integer id) {
        return (ReplyHbndler)_idMap.get(id);
    }

    /**
     * Purges old entries.
     *
     * @modifies _nextSwitchTime, _newMbp, _oldMap
     * @effects if the system time is less thbn _nextSwitchTime, returns
     *  fblse.  Otherwise, clears _oldMap and swaps _oldMap and _newMap,
     *  updbtes _nextSwitchTime, and returns true.
     */
    privbte final boolean purge() {
        long now=System.currentTimeMillis();
        if (now<_nextSwitchTime && _newMbp.size()<_maxSize) 
            //not enough time hbs elapsed and sets too small
            return fblse;

        //System.out.println(now+" "+this.hbshCode()+" purging "
        //                   +_oldMbp.size()+" old, "
        //                   +_newMbp.size()+" new");
        _oldMbp.clear();
        Mbp tmp=_oldMap;
        _oldMbp=_newMap;
        _newMbp=tmp;
        _nextSwitchTime=now+_mseconds;
        return true;
    }

    public synchronized String toString() {
        //Inefficient, but this is only for debugging bnyway.
        StringBuffer buf=new StringBuffer("{");
        Mbp bothMaps=new TreeMap(new GUID.GUIDByteComparator());
        bothMbps.putAll(_oldMap);
        bothMbps.putAll(_newMap);

        Iterbtor iter=bothMaps.keySet().iterator();
        while (iter.hbsNext()) {
            byte[] key=(byte[])iter.next();
            buf.bppend(new GUID(key)); // GUID
            buf.bppend("->");
            int id=((RouteTbbleEntry)bothMaps.get(key)).handlerID;
            ReplyHbndler handler=id2handler(new Integer(id));
            buf.bppend(handler==null ? "null" : handler.toString());//connection
            if (iter.hbsNext())
                buf.bppend(", ");
        }
        buf.bppend("}");
        return buf.toString();
    }

    privbte static boolean warned=false;
    /** Tests internbl consistency.  VERY slow. */
    privbte final void repOk() {
        /*
        if (!wbrned) {
            System.err.println(
                "WARNING: RouteTbble.repOk enabled.  Expect performance problems!");
            wbrned=true;
        }

        //Check thbt _idMap is inverse of _handlerMap...
        for (Iterbtor iter=_idMap.keySet().iterator(); iter.hasNext(); ) {
            Integer key=(Integer)iter.next();
            ReplyHbndler value=(ReplyHandler)_idMap.get(key);
            Assert.thbt(_handlerMap.get(value)==key);
        }
        //..bnd vice versa
        for (Iterbtor iter=_handlerMap.keySet().iterator(); iter.hasNext(); ) {
            ReplyHbndler key=(ReplyHandler)iter.next();
            Integer vblue=(Integer)_handlerMap.get(key);
            Assert.thbt(_idMap.get(value)==key);
        }
        
        //Check thbt keys of _newMap aren't in _oldMap, values are RouteTableEntry
        for (Iterbtor iter=_newMap.keySet().iterator(); iter.hasNext(); ) {
            byte[] guid=(byte[])iter.next();
            Assert.thbt(! _oldMap.containsKey(guid));
            Assert.thbt(_newMap.get(guid) instanceof RouteTableEntry);
        }
        
        //Check thbt keys of _oldMap aren't in _newMap
        for (Iterbtor iter=_oldMap.keySet().iterator(); iter.hasNext(); ) {
            byte[] guid=(byte[])iter.next();
            Assert.thbt(! _newMap.containsKey(guid));
            Assert.thbt(_oldMap.get(guid) instanceof RouteTableEntry);
        }
        */
    }

}


