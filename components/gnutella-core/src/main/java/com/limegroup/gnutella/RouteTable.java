
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.limegroup.gnutella.search.ResultCounter;

/**
 * A RouteTable maps Gnutella packet GUIDs to the remote computer that sent us the request packet, and will want the response packet when it arrives.
 * 
 * === How routing in Gnutella works. ===
 * 
 * A Gnutella program has several connections to remote computers also running Gnutella software.
 * Gnutella packets have message GUIDs that mark them unique.
 * 
 * The types of Gnutella packets exist in pairs, with a request packet and its corresponding reply packet.
 * For instance, a ping is a request packet, and a pong is it's corresponding reply packet.
 * The other pair is for searching: a query is a request packet, and a query hit is it's corresponding reply packet.
 * 
 * When we get a request packet like a ping or a query, we do 2 things.
 * First, we make a note of its message GUID and which connection it came from.
 * Then, we broadcast it forward, sending it down the other connections.
 * 
 * Later, a response packet like a pong or a query hit will come back.
 * It will have the same message GUID as the ping or query it's a response to.
 * We look its GUID up in our list, and find out which connection wanted it.
 * With each computer doing this, reply packets get routed all the way back to the computer that sent the request.
 * 
 * === How this RouteTable class lets LimeWire route Gnutella packets. ===
 * 
 * A RouteTable maps message GUIDs to ReplyHandler objects.
 * 
 * ReplyHandler is an interface that the ManagedConnection, UDPReplyHandler, and ForMeReplyHandler classes implement.
 * A ManagedConnection object represents a remote computer that we have a TCP socket Gnutella connection with.
 * A UDPReplyHandler object represents a remote computer that we've been exchanging UDP packets with.
 * The ForMeReplyHandler object represents us.
 * All 3 are ReplyHandler objects, and all 3 represent computers on the Internet running Gnutella software.
 * 
 * The ReplyHandler interface requires methods like handlePingReply(pong).
 * Give that method a pong packet, and it will send it to the remote computer it represents.
 * 
 * When LimeWire gets a request packet like a ping or a query, it adds its message GUID and the ReplyHandler it came from in a RouteTable.
 * Later, when LimeWire gets a response packet like a pong or a query hit, is seaches the RouteTable for the message GUID to see which computer it's for.
 * 
 * === The RouteTable object that LimeWire uses. ===
 * 
 * The MessageRouter object creates 4 RouteTable objects.
 * These are the only RouteTable objects that exist as LimeWire runs.
 * 
 * private RouteTable _pingRouteTable     = new RouteTable(2 * 60, MAX_ROUTE_TABLE_SIZE);
 * private RouteTable _queryRouteTable    = new RouteTable(5 * 60, MAX_ROUTE_TABLE_SIZE);
 * private RouteTable _pushRouteTable     = new RouteTable(7 * 60, MAX_ROUTE_TABLE_SIZE);
 * private RouteTable _headPongRouteTable = new RouteTable(10,     MAX_ROUTE_TABLE_SIZE);
 * 
 * _pingRouteTable is for ping and pong packets.
 * _queryRouteTable is for query and query hit packets.
 * _pushRouteTable is used differently than the others.
 * _headPongRouteTable is for head pong packets, a LimeWire vendor message that is not the same thing as a pong.
 * 
 * === How to use a RouteTable. ===
 * 
 * When you get a ping, add a new routing entry with:
 * 
 *   routeTable.routeReply(guid, replyHandler);
 * 
 * guid is the ping's message GUID.
 * replyHandler is the connection that sent the ping to us.
 * 
 * Later, when you get a pong, find out where to send it with:
 * 
 *   replyHandler = routeTable.getReplyHandler(guid);
 * 
 * guid is the pong's message GUID, which is the same as the ping's that we got before.
 * replyHandler is the connection that sent us the corresponding ping, and will want this pong.
 * 
 * === Classes related to RouteTable. ===
 * 
 * Here are some classes and interfaces related to RouteTable, and that RouteTable uses:
 * RouteTable class
 * RouteTableEntry nested class
 * ReplyRoutePair nested class
 * ResultCounter interface
 * 
 * A RouteTable object is a list that maps GUIDs to ReplyHandler objects.
 * The values under the GUIDs aren't actually ReplyHandler objects, but rather RouteTableEntry objects that lead to ReplyHandler objects and bundle transfer statistics.
 * ReplyRoutePair objects wrap a ReplyHandler with these transfer statistics.
 * getReplyHandler(byte[], int, short) returns a ReplyRoutePair to return a ReplyHandler with this additional information.
 * The ResultCounter interface is only implementd by one class in LimeWire, RouteTableEntry.
 * routeReply() and tryToRouteReply() return RouteTableEntry objects cast to their ResultCounter interface.
 * 
 * === How RouteTable works. ===
 * 
 * When you make a RouteTable, you give it a time interval, like 5 seconds.
 * The RouteTable has 2 maps inside, named new and old.
 * Every 5 seconds, it moves everything from new to old, and throws away the contents of old.
 * The purge() method does this.
 * 
 * When you add a GUID and ReplyHandler, it goes into the new map.
 * When you look up a GUID, the RouteTable searches both maps.
 * 
 * This is a very efficient way to keep only recent listings in the RouteTable.
 * A lucky GUID will get added right after the purge, and be in the RouteTable for almost 10 seconds.
 * An unlucky GUID will be added right before the purge, and be in the RouteTable for just more than 5 seconds.
 * Every listing will exist for an amount of time between the given interval, and twice that interval.
 * Exactly how long a listing exists is random, depending on when during the interval the program adds it.
 * 
 * ===
 * 
 * The reply routing table.  Given a GUID from a reply message header,
 * this tells you where to route the reply.  It is mutable mapping
 * from globally unique 16-byte message IDs to connections.  Old
 * mappings may be purged without warning, preferably using a FIFO
 * policy.  This class makes a distinction between not having a mapping
 * for a GUID and mapping that GUID to null (in the case of a removed
 * ReplyHandler).
 *
 * This class can also optionally keep track of the number of reply bytes 
 * routed per guid.  This can be useful for implementing fair flow-control
 * strategies.
 * 
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
 * _replyMap.  Better yet, if the values of _map were indices (with tags)
 * into ConnectionManager._initialized[Client]Connections, we would not
 * need _idMap either.  However, this increases dependenceies.
 */
public final class RouteTable {

    /*
     * RouteTable uses _newMap and _oldMap together.
     * 
     * Both map byte[] to RouteTableEntry.
     * The keys are GUID values in byte arrays.
     * The values are RouteTableEntry objects.
     * 
     * The maps are Java TreeMap objects that keep their contents in sorted order.
     * The constructor takes our GUID.GUIDByteComparator.compare(Object a, Object b) method and uses it to see which of two GUIDs should be placed first.
     */

    /** A map of message GUIDs and the ReplyHandler objects that represent the remote computers that sent us packets with them. */
    private Map _newMap = new TreeMap(new GUID.GUIDByteComparator());

    /** We'll move all the listings from _newMap here before throwing them away, keeping the listings this RouteTable keeps current. */
    private Map _oldMap = new TreeMap(new GUID.GUIDByteComparator());

    /**
     * The number of milliseconds this RouteTable will remember its routing information.
     * When you make a new RouteTable, you specify the time interval you want.
     * 
     * The purge() method notices if this much time has passed since the last purge.
     * If it has, it moves everything from the new map to the old map.
     */
    private int _mseconds;

    /**
     * The time when the purge() method must next shift everything from the new map into the old one, and throw out the contents of the old one.
     */
    private long _nextSwitchTime;

    /**
     * If the new map grows to hold this many items, the purge() method will shift its contents to the old map and throw the old contents away.
     * When you make a new RouteTable, you specify the maximum size you want.
     */
    private int _maxSize;

    /*
     * RouteTable uses _idMap and _handlerMap together.
     * Both are Java HashMap objects that map keys to values.
     * _idMap maps Integer keys to ReplyHandler values.
     * _handlerMap maps ReplyHandler keys to Integer values.
     * 
     * To add a ReplyHandler to the maps, call id = handler2id(handler).
     * This is also how you look up a handler that's already there, getting it's ID.
     * If you have an ID and want it's handler, use handler = id2handler(id).
     * Call removeReplyHandler(ReplyHandler) to remove a ReplyHandler from both maps.
     */

    /** _idMap maps Integer IDs to ReplyHandler objects. */
    private Map _idMap = new HashMap();

    /** _handlerMap maps ReplyHandler objects to Integer IDs. */
    private Map _handlerMap = new HashMap();

    /**
     * The next ID number to use in _idMap and _handlerMap.
     * Java initializes _nextID to 0, the first ID we use.
     */
    private int _nextID;

    /**
     * A RouteTable maps GUIDs to RouteTableEntry objects, which contain ReplyHandler objects and more information.
     * 
     * In a RouteTable, _newMap and _oldMap map message GUIDs to RouteTableEntry objects.
     * A RouteTableEntry object has our handler ID, and counts the number of bytes and packets we've routed to it.
     * To turn a handler ID into a ReplyHandler, use handler = id2handler(id).
     * 
     * A RouteTableEntry object also has a TTL.
     * You can access this TTL with getTTL() and setTTL(ttl).
     * (do) How is this used?
     * 
     * RouteTableEntry implements the ResultCounter interface, requiring it to have a getNumResults() method.
     */
    private static final class RouteTableEntry implements ResultCounter {

        /** The ID number that you can look up to get the ReplyHandler. */
        private int handlerID;

        /** The total size of reply packet data we've routed to the ReplyHandler for this GUID request. */
        private int bytesRouted;

        /** The total number of packets we've routed to the ReplyHandler for this GUID request. */
        private int repliesRouted;

        /**
         * The ttl associated with this RTE - meaningful only if > 0. (do)
         */
        private byte ttl = 0;

        /**
         * Make a new RouteTableEntry object to put it in _newMap or _oldMap.
         * 
         * @param handlerID An ID number this class will use in place of a reference to a RouteTable object
         */
        RouteTableEntry(int handlerID) {

            // Save the given handler ID
            this.handlerID = handlerID;

            // Start the transfer statistics at 0
            this.bytesRouted   = 0;
			this.repliesRouted = 0;
        }

        /**
         * Set the TTL this RouteTableEntry object will remember.
         * 
         * @param ttl The new TTL number to store in this object
         */
        public void setTTL(byte ttl) {

            // Save the given TTL number
            this.ttl = ttl;
        }

        /**
         * Get the TTL you set in this RouteTableEntry object.
         * 
         * @return The TTL code set with setTTL(ttl)
         */
        public byte getTTL() {

            // Return the set number
            return ttl;
        }

		/**
         * Get the number of reply packets with this GUID that we've received.
         * 
         * In a RouteTable, message GUIDs map to RouteTableEntry objects.
         * The RouteTableEntry counts this number of reply packets, and keeps the ReplyHandler which made the request.
         * 
         * The ResultCounter interface requires this method.
         * 
         * @return The number of reply packets we've routed to this remote computer for the request packet with this GUID
         */
		public int getNumResults() {

            // Return the count we've been keeping
            return repliesRouted;
        }
    }

    /**
     * Make a new RouteTable that will map message GUIDs to remote computers so we know where to send back a reply packet.
     * 
     * @param seconds The new RouteTable will remember a message GUID for from this long to twice this long
     * @param maxSize The new RouteTable will remember this many message GUIDs
     */
    public RouteTable(int seconds, int maxSize) {

        /*
         * Typically maxSize is very large, and serves only as a guarantee to
         * prevent worst case behavior.  Actually 2*maxSize elements can be held in
         * this in the worst case.
         */

        // Save the given values, and set the times
        this._mseconds       = seconds * 1000;                         // Convert the given time in seconds to milliseconds before saving it in _mseconds
        this._nextSwitchTime = System.currentTimeMillis() + _mseconds; // Let purge() run for the first time that long from now
        this._maxSize        = maxSize;
    }

    /**
     * Add a new routing entry to this RouteTable, renewing it if it already exits.
     * Do this when we get a request packet we're going to broadcast forward.
     * Add the packet's GUID and the remote computer that sent it to us to the RouteTable.
     * Later, when we get a reply packet with the same GUID, we'll know what computer to send it to.
     * 
     * Calls replyHandler.isOpen() to make sure the ManagedConnection can still send a packet to the remote computer it represents.
     * Returns null if it's closed, so it can't.
     * 
     * Removes the GUID from the old or new lists before adding it to the new list.
     * This has the effect of renewing the GUID.
     * 
     * @return guid         The message GUID of a request packet we received and are going to broadcast forward.
     * @return replyHandler The ManagedConnection, UDPReplyHandler, or ForMeReplyHandler that sent it to us.
     * @return              A RouteTableEntry object that contains the given ReplyHandler, and is now listed in this RouteTable under the given GUID.
     *                      RouteTableEntry implements the ResultCounter interface, letting you call getNumResults() on it.
     *                      If the given ReplyHandler is a ManagedConnection that's closed, doesn't add anything and returns null.
     */
    public synchronized ResultCounter routeReply(byte[] guid, ReplyHandler replyHandler) {

        // Used for testing
        repOk();

        // Discard the old records from this RouteTable
        purge();

        // Make sure the caller gave us a ReplyHandler object that will be able to send a reply packet to the remote computer it represents
		if (replyHandler == null) throw new NullPointerException("null reply handler");
        if (!replyHandler.isOpen()) return null; // Make sure it can still send a packet to the computer it represents

        /*
         * First clear out any old entries for the guid, memorizing the volume
         * routed if found.  Note that if the guid is found in _newMap, we don't
         * need to look in _oldMap.
         */

        // Look up or add and look up the given ReplyHandler, getting or assigning and getting our ID for it
        int id = handler2id(replyHandler).intValue();

        // Remove the given GUID from this RouteTable
        RouteTableEntry entry = (RouteTableEntry)_newMap.remove(guid);
        if (entry == null) entry = (RouteTableEntry)_oldMap.remove(guid); // We only need to remove it from _oldMap if it's not in _newMap

        /*
         * Now map the guid to the new reply handler, using the volume routed if
         * found, or zero otherwise.
         */

        // This RouteTable doesn't have an entry for the given GUID
        if (entry == null) {

            // Make a new RouteTableEntry for the given ReplyHandler, we'll store it under the given GUID
            entry = new RouteTableEntry(id); // The RouteTableEntry object will hold the ID that leads to the ReplyHandler, not the ReplyHandler itself

        // We found the GUID in this RouteTable
        } else {

            // Point the existing RouteTableEntry at the given ReplyHandler
            entry.handlerID = id; // This saved us from having to make a new RouteTableEntry object
        }

        // Add the given ReplyHandler under the given GUID in the new map, and return the RouteTableEntry that contains it
        _newMap.put(guid, entry);
		return entry;
    }

    /**
     * Add a new routing entry to this RouteTable, not doing anything it if it already exits.
     * Do this when we get a request packet we're going to broadcast forward.
     * Add the packet's GUID and the remote computer that sent it to us to the RouteTable.
     * Later, when we get a reply packet with the same GUID, we'll know what computer to send it to.
     * 
     * Calls replyHandler.isOpen() to make sure the ManagedConnection can still send a packet to the remote computer it represents.
     * Returns null if it's closed, so it can't.
     * 
     * If the GUID already has an entry in the old or new lists, doesn't renew it, just returns null.
     * 
     * @return guid         The message GUID of a request packet we received and are going to broadcast forward.
     * @return replyHandler The ManagedConnection, UDPReplyHandler, or ForMeReplyHandler that sent it to us.
     * @return              A RouteTableEntry object that contains the given ReplyHandler, and is now listed in this RouteTable under the given GUID.
     *                      RouteTableEntry implements the ResultCounter interface, letting you call getNumResults() on it.
     *                      If the given ReplyHandler is a ManagedConnection that's closed, doesn't add anything and returns null.
     *                      If the given GUID is already listed in this RouteTable, doesn't renew it and returns null.
     */
    public synchronized ResultCounter tryToRouteReply(byte[] guid, ReplyHandler replyHandler) {

        // Used for testing
        repOk();

        // Discard the old records from this RouteTable
        purge();

        // Make sure the caller gave us a ReplyHandler object that will be able to send a reply packet to the remote computer it represents, and a GUID to list it under
        Assert.that(replyHandler != null);
        Assert.that(guid != null, "Null GUID in tryToRouteReply");
        if (!replyHandler.isOpen()) return null; // Return null if the given ReplyHandler is a ManagedConnection that's lost its TCP socket connection

        // The GUID isn't in the new or old lists yet
        if (!_newMap.containsKey(guid) && !_oldMap.containsKey(guid)) {

            // Add the given ReplyHandler under the given GUID in the new map, and return the RouteTableEntry that contains it
            int id = handler2id(replyHandler).intValue();    // Assign a new ID for the given ReplyHandler
			RouteTableEntry entry = new RouteTableEntry(id); // Make a new RouteTableEntry that will lead to the given ReplyHandler by containing this ID
			_newMap.put(guid, entry);                        // Add the RouteTableEntry to the new map under the GUID key
            return entry;                                    // Return it as a ResultCounter you can call getNumResults() on

        // We already have this GUID
        } else {

            // Return null, call routeReply() to renew an already listed GUID
            return null;
        }
    }

    /**
     * Set the TTL that an entry in a RouteTable for a GUID keeps.
     * 
     * A RouteTableEntry, which is a ResultCounter, contains a ReplyHandler and a TTL.
     * Code in this class doesn't change or read the TTL, it just keeps it here.
     * 
     * Optional operation - if you want to remember the TTL associated with a
     * counter, in order to allow for extendable execution, you can set the TTL
     * a message (guid).
     * 
     * @param entry A RouteTableEntry stored under a GUID in this RouteTable.
     *              RouteTableEntry implements ResultCounter, so you may have a reference of that type.
     * @param ttl   The TTL to store in a GUID's entry.
     *              This should be greater than 0.
     */
    public synchronized void setTTL(ResultCounter entry, byte ttl) {

        // Make sure the caller gave us a RouteTableEntry object, and the TTL isn't 0
        if (entry == null)                       throw new IllegalArgumentException("Null entry!!");
        if (!(entry instanceof RouteTableEntry)) throw new IllegalArgumentException("entry is not recognized.");
        if (!(ttl > 0))                          throw new IllegalArgumentException("Input TTL too small: " + ttl);

        // Have the RouteTableEntry object remember the given TTL
        ((RouteTableEntry)entry).setTTL(ttl);
    }

    /**
     * Change the TTL stored under a GUID in this RouteTable if you know it's current value.
     * Only MessageRouter.wasProbeQuery() calls this.
     * 
     * Synchronizes a TTL get test with a set test.
     * 
     * @param guid   A GUID that should be listed in this RouteTable.
     * @param getTTL The TTL we think it is remembering.
     * @param setTTL We want to change from that TTL to this one.
     * @return       True if the GUID was listed here with getTTL, and we changed it to setTTL.
     *               False if the GUID isn't here, or doesn't have getTTL.
     */
    public synchronized boolean getAndSetTTL(byte[] guid, byte getTTL, byte setTTL) {

        // Make sure both TTLs are 1 or more, and getTTL is the same or bigger than setTTL
        if ((getTTL < 1) || (setTTL <= getTTL)) throw new IllegalArgumentException("Bad ttl input (get/set): " + getTTL + "/" + setTTL);

        // Look up the given GUID in this RouteTable
        RouteTableEntry entry = (RouteTableEntry)_newMap.get(guid);
        if (entry == null) entry = (RouteTableEntry)_oldMap.get(guid);

        // If we have the GUID listed and the TTL it's remembering is the same as getTTL
        if ((entry != null) && (entry.getTTL() == getTTL)) {

            // Have it remember the new TTL instead, and return true
            entry.setTTL(setTTL);
            return true;
        }

        // The GUID isn't listed, or doesn't have getTTL
        return false;
    }

    /**
     * Given a message GUID, look up the ReplyHandler object that represents the remote computer that sent us the request packet and should get the reply packet.
     * A ReplyHandler is a ManagedConnection or UDPReplyHandler object that represents a remote computer and can send a packet to it.
     * 
     * Doesn't purge the lists before using them like other methods here do.
     * 
     * @param guid The GUID of a reply message that we may have seen in a request packet before.
     * @return     The ReplyHandler object that represents our connection to the remote computer that sent us a request packet with that message GUID.
     *             null if not found.
     */
    public synchronized ReplyHandler getReplyHandler(byte[] guid) {

        /*
         * no purge
         */

        // Used for testing
        repOk();

        // Look up the given message GUID to find the ID we assigned the desired ReplyHandler
        RouteTableEntry entry = (RouteTableEntry)_newMap.get(guid);    // Look for it in _newMap
        if (entry == null) entry = (RouteTableEntry)_oldMap.get(guid); // If not there, look for it in _oldMap

        // Look up the ID to get the ReplyHandler, and return it
        return (entry == null) ? null : id2handler(new Integer(entry.handlerID)); // Returns null if not found
    }

    /**
     * Given a message GUID, look up the ReplyHandler object that represents the remote computer that sent us the request packet and should get the reply packet.
     * A ReplyHandler is a ManagedConnection or UDPReplyHandler object that represents a remote computer and can send a packet to it.
     * 
     * This getReplyHandler() method returns a ReplyRoutePair object, not a ReplyHandler.
     * The ReplyRoutePair contains the ReplyHandler, along with the number of packets and size of packet data we've sent in reply.
     * 
     * You can pass this method two more pieces of information, a byte size and a packet count.
     * They let you record that you've sent back that many more bytes of reply packet data and that many more reply packets because of this request.
     * getReplyHandler() returns the saved statistics, and then increments them.
     * 
     * Doesn't purge the lists before using them like other methods here do.
     * 
     * @param guid       The GUID of a reply message that we may have seen in a request packet before.
     * @param replyBytes The number of additional bytes of reply packet data we're routing back because of the request.
     * @param numReplies The number of additional file hit blocks in the packets we're routing back because of the request.
     * @return           A ReplyRoutePair object that contains the ReplyHandler that represents our connection to that computer, and packet statistics.
     *                   null if not found.
     */
    public synchronized ReplyRoutePair getReplyHandler(byte[] guid, int replyBytes, short numReplies) {

        /*
         * no purge
         */

        // Used for testing
        repOk();

        // Look up the given message GUID to find the RouteTableEntry that contains the ReplyHandler that represents the remote computer that sent us a request packet with that message GUID
        RouteTableEntry entry = (RouteTableEntry)_newMap.get(guid);    // Look for it in _newMap
        if (entry == null) entry = (RouteTableEntry)_oldMap.get(guid); // If not there, look for it in _oldMap
        if (entry == null) return null; // Not found

        // Look up the ID to get the ReplyHandler
        ReplyHandler handler = id2handler(new Integer(entry.handlerID));
        if (handler == null) return null; // Not found

        /*
         * Increment count, returning old count in tuple.
         */

        // Make a new ReplyRoutePair object to match the RouteTableEntry object for the given GUID
        ReplyRoutePair ret = new ReplyRoutePair(
            handler,              // From the GUID, we found the ID and then the ReplyHandler
            entry.bytesRouted,    // Save the RouteTableEntry's bytesRouted field in ReplyRoutePair.volume
            entry.repliesRouted); // Save the RouteTableEntry's repliesRouted field in ReplyRoutePair.REPLIES_ROUTED

        // Add the new bytes and file hit blocks we sent to the RouteTableEntry in the new or old map, leaving the ReplyRoutePair we just made unchanged
        entry.bytesRouted   += replyBytes;
        entry.repliesRouted += numReplies;

        // Return the ReplyRoutePair with the found ReplyHandler and the packet statistics before the current updates
        return ret;
    }

    /**
     * A ReplyRoutePair object keeps a ReplyHandler with transfer statistics.
     * A ReplyHandler is a ManagedConnection or UDPReplyHandler object that represents a remote computer and can send a reply packet to it.
     * The statistics are volume and REPLIES_ROUTED.
     * REPLIES_ROUTED is the number of reply packets its sent, and volume is their total size in bytes.
     * 
     * The getReplyHandler() method above returns a ReplyRoutePair object, not a ReplyHandler.
     * This lets it return the statistics along with the ReplyHandler.
     */
    public static final class ReplyRoutePair {

        /** A MangedConnection or UDPReplyHandler object that represents a remote computer and can send a packet to it. */
        private final ReplyHandler handler;

        /** The number of bytes of packet data the ReplyHandler object has sent its computer because of this GUID request. */
        private final int volume;

        /** The number of file hits the ReplyHandler object has sent its computer because of this GUID request. */
        private final int REPLIES_ROUTED;

        /**
         * Make a new ReplyRoutePair object given information from a RouteTableEntry.
         * 
         * @param handler The ReplyHandler, the ManagedConnection or UDPReplyHandler object that can send a reply packet to the remote computer it represents
         * @param volume  The total size of all the reply packets it's sent its remote computer because of this GUID request
         * @param hits    The number of file hits it's sent its remote computer because of this GUID request
         */
        ReplyRoutePair(ReplyHandler handler, int volume, int hits) {

            // Save the ReplyHandler in this object with its statistics
            this.handler   = handler;
            this.volume    = volume;
            REPLIES_ROUTED = hits;
        }

        /**
         * Get the ReplyHandler stored in this object.
         * A ReplyHandler is a ManagedConnection or UDPReplyHandler object that represents a remote computer and can send a packet to it.
         * This ReplyRoutePair object keeps a ReplyHandler together with some statistics about how many packets have gone to it because of a GUID request.
         * 
         * @return The ReplyHandler object inside this ReplyRoutePair
         */
        public ReplyHandler getReplyHandler() {

            // Return the reference the constructor saved
            return handler;
        }

        /**
         * The number of bytes of packet data the ReplyHandler in this ReplyRoutePair object has sent its computer because of this GUID request.
         * 
         * @return The data size in bytes
         */
        public int getBytesRouted() {

            // Return the number the constructor saved
            return volume;
        }

        /**
         * The number of packets the ReplyHandler in this ReplyRoutePair object has sent its computer because of this GUID request.
         * 
         * @return The number of packets
         */
        public int getResultsRouted() {

            // Return the number the constructor saved
            return REPLIES_ROUTED;
        }
    }

    /**
     * Remove a ReplyHandler and its given ID from _idMap and _handlerMap.
     * Doesn't purge the lists before using them like other methods here do.
     * Doesn't report an error if not found.
     * 
     * @param replyHandler A ReplyHandler object to remove from the _idMap and _handlerMap lists
     */
    public synchronized void removeReplyHandler(ReplyHandler replyHandler) {

        /*
         * no purge
         */

        // Used for testing
        repOk();

        /*
         * The aggressive asserts below are to make sure bug X75 has been fixed.
         */

        // Make sure the caller actually gave us a ReplyHandler to remove
        Assert.that(replyHandler != null, "Null replyHandler in removeReplyHandler");

        /*
         * Note that _map is not modified.  See overview of class for rationale.
         * Also, handler2id may actually allocate a new ID for replyHandler, when
         * killing a connection for which we've routed no replies.  That's ok;
         * we'll just clean up the new ID immediately.
         */

        // Get the ReplyHandler's ID
        Integer id = handler2id(replyHandler); // If not found, this will add it and assign it an ID

        // Remove the ReplyHandler and its ID from the _idMap and _handlerMap
        _idMap.remove(id);
        _handlerMap.remove(replyHandler);
    }

    /** 
     * Look up a ReplyHandler in the _handlerMap and _idMap, and get its ID.
     * If not found, adds the ReplyHandler to the lists, and returns the new ID we assigned it.
     * 
     * If the handler is already in _handlerMap and _idMap, returns its ID.
     * If it's not found, adds it.
     * Chooses a new ID that isn't being used.
     * Adds the handler and ID to _handlerMap and _idMap.
     * Returns the new ID.
     * 
     * @param handler A ReplyHandler to find or add in the lists.
     * @return        An Integer object, the associated ID we found or assigned.
     */
    private Integer handler2id(ReplyHandler handler) {

        /*
         * Have we encountered this handler recently?  If so, return the id.
         */

        // Look up the given handler in _handlerMap, and if found, return its ID value
        Integer id = (Integer)_handlerMap.get(handler);
        if (id != null) return id;

        /*
         * Otherwise return the next free id, searching in extremely rare cases
         * if needed.  Note that his enters an infinite loop if all 2^32 IDs are
         * taken up.  BFD.
         */

        // Loop to search for an ID that's not being used
        while (true) {

            /*
             * don't worry about overflow; Java wraps around TODO1?
             */

            // Loop until we find an ID number that isn't being used as a key in the _idMap list
            id = new Integer(_nextID++);       // Get the current value of _nextID, wrap it into an Integer object, and then move _nextID to the next higher value for next time
            if (_idMap.get(id) == null) break; // If looking up that ID finds nothing, leave the loop to use it as the new key
        }

        // Load the given ReplyHandler and newly chosen ID into the _handlerMap and _idMap lists, and return the ID
        _handlerMap.put(handler, id); // Use _handlerMap if you know the handler, and want the ID
        _idMap.put(id, handler);      // Use _idMap if you know the ID, and want the handler
        return id;                    // Return the ID we chose for the given hander
    }

    /**
     * Look up the ReplyHandler stored under the given ID key in _idMap.
     * 
     * @param id An ID number.
     * @return   The ReplyHandler stored under that key in _idMap.
     *           null if not found.
     */
    private ReplyHandler id2handler(Integer id) {

        // Look up the ID in _idMap, cast the value back to a ReplyHandler, and return it
        return (ReplyHandler)_idMap.get(id);
    }

    /**
     * Shifts the contents of _newMap into _oldMap, and throws the old contents away.
     * Only does this if it's been _mseconds since the last switch, or _newMap has grown too large.
     * 
     * @return True if the time expired or the new map grew too large, and we shifted the list contents.
     *         False if we still need to wait some more.
     */
    private final boolean purge() {

        // Only do something if we've waited long enough or the new map has grown too large
        long now = System.currentTimeMillis(); // Get the time right now
        if (now < _nextSwitchTime &&   // We haven't waited long enough yet, and
            _newMap.size() < _maxSize) // The new map hasn't overgrown yet
            return false;              // Come back later

        /*
         * System.out.println(now + " " + this.hashCode() + " purging " + _oldMap.size() + " old, " + _newMap.size() + " new");
         */

        // Clear the old map, and then move everything from the new map into it
        _oldMap.clear();   // Delete the contents of the old map
        Map tmp = _oldMap; // Point tmp at the old map, which is empty
        _oldMap = _newMap; // Point _oldMap at the new map, which still has contents
        _newMap = tmp;     // Point _newMap at tmp, our reference to the empty old map

        // Set the next time we'll do this
        _nextSwitchTime = now + _mseconds;

        // Report that we purged the lists
        return true;
    }

    /**
     * Express this RouteTable object as a string.
     * Composes text like: "{GGGGUUUUIIIIDDDD->CONNECTION: host=1.2.3.4 port=6346, GGGGUUUUIIIIDDDD->... }".
     * Calls ManagedConnection.toString(), UDPReplyHandler.toString(), and ForMeReplyHandler.toString() to have each ReplyHandler describe itself.
     * 
     * @return A String
     */
    public synchronized String toString() {

        /*
         * Inefficient, but this is only for debugging anyway.
         */

        // Make a StringBuffer that can grow to hold text, and start with a "{"
        StringBuffer buf = new StringBuffer("{");

        // Combine the _oldMap and _newMap lists into a single big one
        Map bothMaps = new TreeMap(new GUID.GUIDByteComparator()); // Have it use our GUID.GUIDByteComparator.compare(Object a, Object b) to keep the contents sorted
        bothMaps.putAll(_oldMap); // Add everything in both the old and new maps to it
        bothMaps.putAll(_newMap);

        // Loop through the combined list we just made
        Iterator iter = bothMaps.keySet().iterator();
        while (iter.hasNext()) {

            byte[] key = (byte[])iter.next(); // Get the key the iterator is on, the key is a byte array of 16 bytes holding a GUID value
            buf.append(new GUID(key));        // Make the 16 bytes into a GUID object, convert that into base 16 text, and add it to the text we're composing
            buf.append("->");                 // After the GUID, write an arrow

            int id = ((RouteTableEntry)bothMaps.get(key)).handlerID;   // Look up the byte array key in the combined map we made, get its RouteTableEntry value, and read its handler ID
            ReplyHandler handler = id2handler(new Integer(id));        // Look up that ID in the _idMap to get the ReplyHandler it's for
            buf.append(handler == null ? "null" : handler.toString()); // Have the ReplyHandler express itself in text, calls ManagedConnection.toString(), UDPReplyHandler.toString(), or ForMeReplyHandler.toString()
            if (iter.hasNext()) buf.append(", ");
        }

        // End the text with the closing "}", and return it
        buf.append("}");
        return buf.toString();
    }

    /**
     * Not used.
     * 
     * Used in the commented code in repOk().
     */
    private static boolean warned = false;

    /**
     * Does nothing.
     * 
     * Would look at the lists this RouteTable keeps to check that everything looks correct.
     * Very slow.
     */
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
