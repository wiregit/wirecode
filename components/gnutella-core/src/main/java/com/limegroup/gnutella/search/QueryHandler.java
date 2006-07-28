
// Commented for the Learning branch

package com.limegroup.gnutella.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.ForMeReplyHandler;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;

/**
 * A QueryHandler object represents a search this ultrapeer is performing on the Gnutella network.
 * We're either performing it for our user, or for one of our leaves.
 * 
 * LimeWire searches the Gnutella network with a process called dynamic querying.
 * In a dynamic query, a LimeWire ultrapeer sends out a query packet every so often until it gets enough hits.
 * Only ultrapeers do dynamic querying, leaves have one of their ultrapeers do it for them.
 * The code in this QueryHandler class is only used when we're an ultrapeer.
 * 
 * A QueryHandler object keeps the following objects together:
 * A QueryRequest object representing the query packet we got or made and can search with.
 * A ReplyHandler, the ManagedConnection that represents the remote computer we're searching on behalf of and will send query hit packets back to.
 * A ResultCounter, the RouteTableEntry that records how many results we've received and routed back because of this search.
 * 
 * When the user searches for something, MessageRouter.sendDynamicQuery() calls createHandlerForMe().
 * When one of our leaves gives us a search to perform, MessageRouter.handleQueryRequest() calls createHandlerForNewLeaf().
 * 
 * Right after MesageRouter.sendDynamicQuery() and MessageRouter.handleQueryRequest() make QueryHandler objects, they give them to the QueryDispatcher.
 * The call that does this is QueryDispatcher.addQuery(QueryHandler).
 * The QueryDispatcher adds the given QueryHandler to its list, and has its "QueryDispatcher" thread call sendQuery() on it every 0.4 seconds.
 * 
 * sendQuery() runs repeatedly to perform the steps of the dynamic query.
 * It makes sure it's waited long enough since performing the last step before doing the next one.
 * If we haven't searched our leaves yet, sends them query packets.
 * If we haven't sent the probe query yet, sends it.
 * If we have already sent the probe query, calls SendQuery(List) to send query packets to the ultrapeers we're connected to.
 * 
 * This class is a factory for creating <tt>QueryRequest</tt> instances
 * for dynamic queries.  Dynamic queries adjust to the varying conditions of
 * a query, such as the number of results received, the number of nodes
 * hit or theoretically hit, etc.  This class makes it convenient to
 * rapidly generate <tt>QueryRequest</tt>s with similar characteristics,
 * such as guids, the query itself, the xml query, etc, but with customized
 * settings, such as the TTL.
 */
public final class QueryHandler {

    /** A log we can save lines of text in as the program runs. */
    private static final Log LOG = LogFactory.getLog(QueryHandler.class);

	/** The number of hits we'll try to get. */
	private final int RESULTS;

    /**
     * 6, we'll never send out a query packet with a TTL bigger than 6.
     * Actually, calculateNewTTL() tries TTLs less than MAX_QUERY_TTL, so the maximum is 5.
     * In practice, LimeWire never sends query messages with TTLs of 1, 2, or 3 only.
     */
    public static final byte MAX_QUERY_TTL = (byte)6;

    /** 150, as an ultrapeer searching for ourselves, we'll stop after getting 150 * 1.15 = 172 hits. */
	public static final int ULTRAPEER_RESULTS = 150;

    /** 1.15, as an ultrapeer searching for ourselves, we'll stop after getting 150 * 1.15 = 172 hits. */
    public static final double UP_RESULT_BUMP = 1.15;

	/**
     * 20, if we're searching for a leaf that doesn't support advanced Gnutella features, we'll try to get it 20 hits.
     * 
	 * The number of results to try to get if the query came from an old
	 * leaf -- they are connected to 2 other Ultrapeers that may or may
	 * not use this algorithm.
	 */
	private static final int OLD_LEAF_RESULTS = 20;

	/**
     * 38, we'll try to get a leaf 38 hits for its search that we're running.
     * 
	 * The number of results to try to get for new leaves -- they only
	 * maintain 2 connections and don't generate as much overall traffic,
	 * so give them a little more.
	 */
	private static final int NEW_LEAF_RESULTS = 38;

	/**
     * LimeWire doesn't allow searching by hash.
     * 
	 * The number of results to try to get for queries by hash -- really
	 * small since you need relatively few exact matches.
	 */
	private static final int HASH_QUERY_RESULTS = 10;

    /** 75, if we're running a leaf's search for it, we'll stop searching after sending the leaf 75 hits. */
    private static final int MAXIMUM_ROUTED_FOR_LEAVES = 75;

    /**
     * Controls how long we'll wait before sending another query packet.
     * 
     * The sendQuery() method sends out a query packet.
     * Then, it waits a certain amount of time for results to come back.
     * After this amount of time, it sees if it has enough results or needs to keep going.
     * If it needs to keep going, it sends another query packet.
     * 
     * _timeToWaitPerHop is the amount of time it waits for each hop of the query packet it sent out.
     * For instance, if _timeToWaitPerHop is 2400 and its sending out a TTL 2 packet, it will wait 4800 milliseconds.
     * 
     * sendQuery() lowers _timeToWaitPerHop as the search goes on, making it more impatient.
     */
    private volatile long _timeToWaitPerHop = 2400;

    /**
     * Each time sendQuery() sends a packet, it lowers the time we'll wait for responses.
     * 
     * Variable for the number of milliseconds to shave off of the time
     * to wait per hop after a certain point in the query.  As the query
     * continues, the time to shave may increase as well.
     */
    private volatile long _timeToDecreasePerHop = 10;

    /**
     * The number of times we've lowered _timeToWaitPerHop.
     * 
     * Variable for the number of times we've decremented the per hop wait
     * time.  This is used to determine how much more we should decrement
     * it on this pass.
     */
    private volatile int _numDecrements = 0;

    /** 200 seconds in milliseconds, if a search goes on for more than 3 minutes 20 seconds, hasEnoughResults() will return true, stopping the search. */
    public static final int MAX_QUERY_TIME = 200 * 1000;

	/** The program's MessageRouter object, which keeps route tables that match search message GUIDs to connected remote computers. */
	private static MessageRouter _messageRouter = RouterService.getMessageRouter();

	/** The program's ConnectionManager object, which keeps the list of our connections. */
	private static ConnectionManager _connectionManager = RouterService.getConnectionManager();

    /**
     * The number of hits the leaf we're running this search for tells us it has.
     * 
     * In LimeWire's dynamic querying system, we run searches on behalf of our leaves.
     * When we run a search for one of our leaves, the leaf sends us BEAR 12 1 QueryStatusResponse vendor messages that tells us how many hits it has.
     * MessageRouter.handleQueryStatus() calls QueryDispatcher.updateLeafResultsForQuery(), which calls updateLeafResults(int), setting this number.
     */
    private volatile int _numResultsReportedByLeaf = 0;

	/**
     * The time when we'll let ourselves send out more query packets.
     * sendQuery() uses _curTime, _nextQueryTime, and _timeToWaitPerHop to only send out query packets every 2.4 seconds.
	 */
	private volatile long _nextQueryTime = 0;

	/** The number of computers this search has reached. */
	private volatile int _theoreticalHostsQueried = 1;

	/**
     * The RouteTableEntry object for this search that's stored in the MessageRouter object's RouteTable for queries and query hits.
     * Call RESULT_COUNTER.getNumResults() to find out how many search results we've received.
     * 
     * Only the nested RouteTable.RouteTableEntry class implements the ResultCounter interface.
     * RESULT_COUNTER is a RouteTableEntry object.
     * The ResultCounter interface only has one method we can call, RESULT_COUNTER.getNumResults().
     * This calls RouteTableEntry.getNumResults().
	 */
	private final ResultCounter RESULT_COUNTER;

	/**
     * The remote computers that we've already sent our query packet to.
     * An ArrayList of ManagedConnection objects.
	 */
	private final List QUERIED_CONNECTIONS = new ArrayList();

    /**
     * The remote computers that we've sent this search's TTL 1 probe query packet to.
     * An ArrayList of ManagedConnection objects.
     */
    private final List QUERIED_PROBE_CONNECTIONS = new ArrayList();

	/**
     * The time we sent the first query packet for this search.
     * sendQuery() sets this to the current time.
	 */
	private volatile long _queryStartTime = 0;

    /**
     * The last time we sent out query packets, or tried to.
     * sendQuery() uses _curTime, _nextQueryTime, and _timeToWaitPerHop to only send out query packets every 2.4 seconds.
     */
    private volatile long _curTime = 0;

	/**
     * The computer that started this search, and wants response packets sent back to it.
     * REPLY_HANDLER is a ManagedConnection or UDPReplyHandler object representing a remote computer, or the ForMeReplyHandler object that represents us.
	 */
	private final ReplyHandler REPLY_HANDLER;

	/** A query packet we'll copy and modify to make the query packets we'll send out for this search. */
	final QueryRequest QUERY;

    /**
     * When we search our leaves, we'll set _forwardedToLeaves to true.
     * Only used when we're an ultrapeer.
     */
    private volatile boolean _forwardedToLeaves = false;

    /** When sendQuery() sends the probe query, we'll set _probeQuerySent to true. */
    private boolean _probeQuerySent;

    /**
     * The language preference of the remote computer we're searching for, like "fr" for French.
     * 
     * Suppose we're an English ultrapeer.
     * Most of our leaves are also English, while a few of them are French.
     * A French leaf sends us a search.
     * We'll set _prefLocale to "fr", and forward the search to our other French leaves first.
     */
    private final String _prefLocale;

	/**
     * Make a new QueryHandler object from a query packet, desired number of hits, remote computer, and route table entry.
     * 
     * This constructor is private.
     * The factory methods that come next call it.
     * 
     * @param query   The query packet we're making this new QueryHandler for.
     * @param results The number of hits we want.
     * @param handler The remote computer that wants the search results.
     *                An object that implements the ReplyHandler interface, like ManagedConnection, UDPReplyHandler, or ForMeReplyHandler.
     * @param counter The search's entry in the route table.
     *                A RouteTableEntry object placed in the search RouteTable the MessageRouter object keeps.
	 */
	private QueryHandler(QueryRequest query, int results, ReplyHandler handler, ResultCounter counter) {

        // Make sure we have the objects we need
        if (query   == null) throw new IllegalArgumentException("null query");
        if (handler == null) throw new IllegalArgumentException("null reply handler");
        if (counter == null) throw new IllegalArgumentException("null result counter");

        // If the given query packet has a text extension like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ", it's a search by hash
		boolean isHashQuery = !query.getQueryUrns().isEmpty();

        // Save the query packet
		QUERY = query;

        // Set how many results we'll try to get
		if (isHashQuery) RESULTS = HASH_QUERY_RESULTS; // Search by hash, limit to 10 hits
		else             RESULTS = results;            // We'll try to get the number of hits the caller wanted us to get

        // Save the remote computer and route table entry
		REPLY_HANDLER = handler;
        RESULT_COUNTER = counter;

        // Save the remote computer's language preference
        _prefLocale = handler.getLocalePref();
	}

	/** Not used. */
	public static QueryHandler createHandler(QueryRequest query, ReplyHandler handler, ResultCounter counter) {
		return new QueryHandler(query, ULTRAPEER_RESULTS, handler, counter);
	}

	/**
     * Make a QueryHandler object to keep track of a search we're performing for ourselves.
     * This method is only called when we're an ultrapeer.
     * Makes a new QueryHandler that keeps the given objects together, and sets a goal of 172 hits.
     * This factory method doesn't take a ReplyHandler, and uses the ForMeReplyHandler instead.
     * MessageRouter.sendDynamicQuery() calls this.
	 * 
     * @param query   The query packet to make a new QueryHandler object for
     * @param counter The RouteTableEntry for the search, call counter.getNumResults() to find out how many hits it has
     * @return        A new QueryHandler object that groups the given objects together, and keeps track of how many hits the search has
	 */
	public static QueryHandler createHandlerForMe(QueryRequest query, ResultCounter counter) {

        // Make and return a new QueryHandler object
		return new QueryHandler(
            query,                                     // The query packet
            (int)(ULTRAPEER_RESULTS * UP_RESULT_BUMP), // We want 172 hits
            ForMeReplyHandler.instance(),              // This search is for us, give response packets to the ForMeReplyHandler instead of a ManagedConnection object
            counter);                                  // The RouteTableEntry for this search, call counter.getNumResults() to find out how many we've gotten
	}

	/**
     * Make a QueryHandler object to keep track of a search we're performing for one of our leaves.
     * This method is only called when we're an ultrapeer.
     * The leaf is running older Gnutella software that doesn't support advanced Gnutella features.
     * Makes a new QueryHandler that keeps the given objects together, and sets a goal of 20 hits.
     * MessageRouter.handleQueryRequest() calls this.
     * 
     * @param query   The query packet to make a new QueryHandler object for
     * @param handler The remote computer to send response packets back to
     * @param counter The RouteTableEntry for the search, call counter.getNumResults() to find out how many hits it has
     * @return        A new QueryHandler object that groups the given objects together, and keeps track of how many hits the search has
	 */
	public static QueryHandler createHandlerForOldLeaf(QueryRequest query, ReplyHandler handler, ResultCounter counter) {
        
        // Make and return a new QueryHandler object
		return new QueryHandler(
            query,            // The query packet
            OLD_LEAF_RESULTS, // We want 20 hits
            handler,          // The remote computer to give response packets to
            counter);         // The RouteTableEntry for this search, call counter.getNumResults() to find out how many we've gotten
	}

	/**
     * Make a QueryHandler object to keep track of a search we're performing for one of our leaves.
     * This method is only called when we're an ultrapeer.
     * Makes a new QueryHandler that keeps the given objects together, and sets a goal of 38 hits.
     * MessageRouter.handleQueryRequest() calls this.
     * 
     * @param query   The query packet to make a new QueryHandler object for
     * @param handler The remote computer to send response packets back to
     * @param counter The RouteTableEntry for the search, call counter.getNumResults() to find out how many hits it has
     * @return        A new QueryHandler object that groups the given objects together, and keeps track of how many hits the search has
	 */
	public static QueryHandler createHandlerForNewLeaf(QueryRequest query, ReplyHandler handler, ResultCounter counter) {

        // Make and return a new QueryHandler object
		return new QueryHandler(
            query,            // The query packet
            NEW_LEAF_RESULTS, // We want 38 hits
            handler,          // The remote computer to give response packets to
            counter);         // The RouteTableEntry for this search, call counter.getNumResults() to find out how many we've gotten
	}

	/**
     * Copy a query packet, giving the copy a new TTL.
     * This method is public, but only code in this QueryHandler class calls it.
     * 
     * @param query A query packet to copy
     * @param ttl   The TTL for the new query packet this method will make, must be 1 through 6
     * @return      A copy of the given query packet with the given TTL
	 */
	public static QueryRequest createQuery(QueryRequest query, byte ttl) {

        // Make sure the TTL is 1 through 6
		if (ttl < 1 || ttl > MAX_QUERY_TTL) throw new IllegalArgumentException("ttl too high: " + ttl);
		if (query == null) throw new NullPointerException("null query");

        // The given query packet has a hops count of 0, it's our query that we made here
		if (query.getHops() == 0) {

            /*
             * build it from scratch if it's from us
             */

            // Copy the query packet, putting in the new TTL, and return it
            return QueryRequest.createQuery(query, ttl);

        // The given query packet hopped 1 or more times to get here
		} else {

			try {

                // Copy the query packet, putting in the new TTL, and return it
				return QueryRequest.createNetworkQuery(query.getGUID(), ttl, query.getHops(), query.getPayload(), query.getNetwork());

            // Shouldn't happen
			} catch (BadPacketException e) {

                /*
                 * this should never happen, since the query was already
                 * read from the network, so report an error
                 */

                // Give the exception to the ErrorService and return null
				ErrorService.error(e);
				return null;
			}
		}
	}

    /**
     * Copy this QueryHandler object's query packet, giving the copy a new TTL.
     * 
     * @param ttl The TTL for the new query packet this method will make, must be 1 through 6
     * @return    A copy of this object's query packet with the given TTL
     */
    QueryRequest createQuery(byte ttl) {

        // Copy this object's query packet, giving it the new TTL, and return it
        return createQuery(QUERY, ttl);
    }

	/**
     * Search our leaves and ultrapeers, sending out a proby query first.
     * Call this method repeatedly to perform the time separated steps of the search.
     * If you call it too frequently, it will return without doing anything until its waited long enough for the next step.
     * 
     * Before doing anything, sendQuery() checks 2 things:
     * We still need more hits for this search.
     * We've waited long enough since the last time we sent out query packets.
     * 
     * If we haven't searched our leaves yet, sends them query packets.
     * If we haven't sent the probe query yet, sends it.
     * If we have already sent the probe query, calls SendQuery(List) to send query packets to the ultrapeers we're connected to.
     * 
     * The QueryDispatcher object keeps QueryHandler objects like this one in a list.
     * It has a "QueryDispatcher" thread that calls sendQuery() on them every 0.4 seconds.
     * This is how the program runs the searches.
     * 
     * QueryDispatcher.addQuery() and QueryDispatcher.processQueries() call sendQuery().
	 */
	public void sendQuery() {

        // If this search already has enough results, or it has been going on too long, leave now
		if (hasEnoughResults()) return;

        // Only do something if we've waited 2.4 seconds
		_curTime = System.currentTimeMillis(); // Make a note of the time now, the time we're going to send the query
		if (_curTime < _nextQueryTime) return; // If we haven't waited long enough yet, leave

		//zootella
		System.out.println("Passed by the next query time of " + _nextQueryTime);

        // Make a note in the debugging log
        if (LOG.isTraceEnabled()) LOG.trace("Query = " + QUERY.getQuery() + ", numHostsQueried: " + _theoreticalHostsQueried);

        // If this is the start of this search, record that it started now
		if (_queryStartTime == 0) _queryStartTime = _curTime;

        /*
         * handle 3 query cases
         */

        /*
         * 1) If we haven't sent the query to our leaves, send it
         */

        // We haven't sent this query to our leaves yet
        if (!_forwardedToLeaves) {

            // Record that now we have to not do this twice
            _forwardedToLeaves = true;

            // Get our query route table, which will show which searches we and our leaves can't possibly return results for
            QueryRouteTable qrt = RouterService.getMessageRouter().getQueryRouteTable();

            // Make a query packet for this search with a TTL of 1
            QueryRequest query = createQuery(QUERY, (byte)1);

            // Add 25 to the number of computers we estimate our search has reached, this is the number of leaves we have
            _theoreticalHostsQueried += 25;

            /*
             * send the query to our leaves if there's a hit and wait,
             * otherwise we'll move on to the probe
             */

            // If we have a query route table and this search passes through it
            if (qrt != null && qrt.contains(query)) {

                // Send the query packet to our leaves
                RouterService.getMessageRouter().forwardQueryRequestToLeaves(query, REPLY_HANDLER);

                // Set _nextQueryTime to 2.4 seconds from now to wait that long before sending any more query packets
                _nextQueryTime = System.currentTimeMillis() + _timeToWaitPerHop;

                // We're done for now
                return;
            }
        }

        /*
         * 2) If we haven't sent the probe query, send it
         */

        // We haven't sent the probe query yet
        if (!_probeQuerySent) {

            // Make a ProbeQuery from this QueryHandler object, giving it our list of connected ultrapeers
            ProbeQuery pq = new ProbeQuery(_connectionManager.getInitializedConnections(), this);

            // Find out how long we should wait for results from this probe query to get back to us
            long timeToWait = pq.getTimeToWait();

            // Send the probe query to our connected ultrapeers, and get back an estimate of how many computers it reached
            _theoreticalHostsQueried += pq.sendProbe();

            // Set the time when we'll let ourselves send more query packets
            _nextQueryTime = System.currentTimeMillis() + timeToWait; // Get the delay from the ProbeQuery object

            // Record that we sent the probe query
            _probeQuerySent = true;

            // We're done for now
            return;

        // We sent the probe query the last time the program ran sendQuery()
        } else {

            /*
             * 3) If we haven't yet satisfied the query, keep trying
             */

            /*
             * Otherwise, just send a normal query -- make a copy of the
             * connections because we'll be modifying it.
             */

            // Give our list of connected ultrapeers to sendQuery(List), to have it send this search's query packet to them
            int newHosts = sendQuery(new ArrayList(_connectionManager.getInitializedConnections())); // Returns the number of computers we estimate we reached

            // sendQuery(List) didn't get our search to any computers at all
            if (newHosts == 0) {

                /*
                 * if we didn't query any new hosts, wait awhile for new
                 * connections to potentially appear
                 */

                // Don't let us send more query packets for 6 seconds
                _nextQueryTime = System.currentTimeMillis() + 6000;
            }

            // Add the number of computers sendQuery(List) reached to our count of them
            _theoreticalHostsQueried += newHosts;

            /*
             * if we've already queried quite a few hosts, not gotten
             * many results, and have been querying for awhile, start
             * decreasing the per-hop wait time
             */

            // The time we've been waiting for each hop is still more than 0.1 seconds, and we won't search again for more than 6 seconds 
            if (_timeToWaitPerHop > 100 && (System.currentTimeMillis() - _queryStartTime) > 6000) {

                // Lower the time we'll wait for each hop
                _timeToWaitPerHop -= _timeToDecreasePerHop;

                /*
                 * Tour Point
                 * 
                 * The math LimeWire uses to balance time in searching.
                 */

                /*
                 * the current decrease is weighted based on the number
                 * of results returned and on the number of connections
                 * we've tried -- the fewer results and the more
                 * connections, the more the decrease
                 */

                // Use our hit goal and our hit count to determine how much less time we'll wait for hits to come back to us
                int resultFactor = Math.max(1, (RESULTS / 2) - (30 * RESULT_COUNTER.getNumResults()));
                int decrementFactor = Math.max(1, (_numDecrements / 6));
                int currentDecrease = resultFactor * decrementFactor;
                currentDecrease = Math.max(5, currentDecrease);
                _timeToDecreasePerHop += currentDecrease;
                _numDecrements++; // Record that we've decreased our wait time once again
                if (_timeToWaitPerHop < 100) _timeToWaitPerHop = 100; // Don't let it get less than 0.1 seconds
            }
        }
    }

    /**
     * Pick a TTL for our query packet, and send it to an ultrapeer.
     * 
     * Takes a list of ultrapeers we're connected to.
     * This method will randomly pick one to send our query packet to.
     * It chooses from ultrapeers that match the language preference of the search first.
     * It picks one that we've been connected to for at least 5 seconds.
     * 
     * This method calls calculateNewTTL() to pick the TTL for the query packet we'll send.
     * It decides how many computers the query needs to reach, and sets the TTL accordingly.
     * 
     * This method calls sendQueryToHost(query, connection, this) to send the query packet.
     * 
     * Only the sendQuery() method above calls this.
     * ultrapeersAll is _connectionManager.getInitializedConnections().
     * 
     * Sends a query to one of the specified List of connections.
     * This is the heart of the dynamic query.  We dynamically calculate the
     * appropriate TTL to use based on our current estimate of how widely the
     * file is distributed, how many connections we have, etc.  This is static
     * to decouple the algorithm from the specific QueryHandler
     * instance, making testing significantly easier.
     * 
     * @param ultrapeersAll A list of all the ultrapeers we're connected to
     * @return              The number of computers we estimate the query packet we sent will reach
     */
    private int sendQuery(List ultrapeersAll) {

        // Get a list of all the ultrapeers we're connected to that have the same language preference of the computer we're searching for
        List ultrapeers = _connectionManager.getInitializedConnectionsMatchLocale(_prefLocale); // This method returns a copy of the list

        /*
         * We have two lists of connected ultrapeers:
         * 
         * ultrapeers    is from _connectionManager.getInitializedConnectionsMatchLocale(_prefLocale).
         * ultrapeersAll is from _connectionManager.getInitializedConnections().
         * 
         * ultrapeers lists our ultrapeers that match the search's locale, and ultrapeersAll is all of them.
         * This QueryHandler object keeps two more lists of ultrapeers:
         * 
         * QUERIED_CONNECTIONS       lists the ultrapeers we've sent our query packet to.
         * QUERIED_PROBE_CONNECTIONS lists the ultrapeers we've sent our probe query to.
         */

        // If there's a ManagedConnection in QUERIED_CONNECTIONS or QUERIED_PROBE_CONNECTIONS not found in ultrapeersAll, remove it
        QUERIED_CONNECTIONS.retainAll(ultrapeersAll); // Removes ultrapeers in our list that we're no longer connected to
        QUERIED_PROBE_CONNECTIONS.retainAll(ultrapeersAll);

        /*
         * if we did get a list of connections that matches the locale
         * of the query
         */

        // We're connected to some ultrapeers with the same language preference as this search
        if (!ultrapeers.isEmpty()) {

            // Remove the ultrapeers we've already contacts from the ultrapeers list
            ultrapeers.removeAll(QUERIED_CONNECTIONS);
            ultrapeers.removeAll(QUERIED_PROBE_CONNECTIONS);

            /*
             * Now, ultrapeers lists the language-matching ultrapeers that we haven't contacted yet.
             * At this point, the ultrapeers list could be empty.
             */
        }

        // We don't have any language-matching ultrapeers we haven't contacted yet
        if (ultrapeers.isEmpty()) {

            // Instead of using the ultrapeers list from getInitializedConnectionsMatchLocale, use the given ultrapeersAll list
            ultrapeers = ultrapeersAll;                      // Bring in the foreign language ultrapeers
            ultrapeers.removeAll(QUERIED_CONNECTIONS);       // Remove the ultrapeers we've already contacted
            ultrapeers.removeAll(QUERIED_PROBE_CONNECTIONS);
        }

        // Find out how many ultrapeers we have in our list
		int length = ultrapeers.size();
        if (LOG.isTraceEnabled()) LOG.trace("potential querier size: " + length);

        // Make variables for the TTL we'll choose and the ultrapeer we'll send our search
        byte ttl = 0;                // We'll copy our query packet and give it this TTL we choose for it
        ManagedConnection mc = null; // We'll send the query packet to this ultrapeer we're connected to

        // Shuffle the list of ultrapeers so we don't contact them in any special order
        Collections.shuffle(ultrapeers);

        // Loop for each ultrapeer in our list to find one we've been connected to for at least 5 seconds
        for (int i = 0; i < length; i++) {
			ManagedConnection curConnection = (ManagedConnection)ultrapeers.get(i); // The ultrapeers are represented by ManagedConnection objects

            /*
             * if the connection hasn't been up for long, don't use it,
             * as the replies will never make it back to us if the
             * connection is dropped, wasting bandwidth
             */

            // If we haven't been exchanging Gnutella packets with this remote ultrapeer for 5 seconds yet, loop to check out the next one
            if (!curConnection.isStable(_curTime)) continue;

            // We've been connected to this ultrapeer for 5 seconds, point mc at it and leave the loop
            mc = curConnection;
            break;
        }

        // If we don't have any connections to query, leave now
        int remainingConnections = Math.max(length + QUERIED_PROBE_CONNECTIONS.size(), 0);
        if (remainingConnections == 0) return 0;

        /*
         * pretend we have fewer connections than we do in case we
         * lose some
         */

        // If we have more than 4 connections, assume that we have 4 less than we actually do
        if (remainingConnections > 4) remainingConnections -= 4;

        // True if we're going to send the probe query
        boolean probeConnection = false;

        // If we found a remote ultrapeer to send our query packet to
        if (mc == null) {

            /*
             * if we have no connections to query, simply return for now
             */

            // If we haven't sent any probe queries yet, leave now
            if (QUERIED_PROBE_CONNECTIONS.isEmpty()) return 0;

            /*
             * we actually remove this from the list to make sure that
             * QUERIED_CONNECTIONS and QUERIED_PROBE_CONNECTIONS do
             * not have any of the same entries, as this connection
             * will be added to QUERIED_CONNECTIONS
             */

            // Remove one ManagedConnection object from the QUERIED_PROBE_CONNECTIONS list
            mc = (ManagedConnection)QUERIED_PROBE_CONNECTIONS.remove(0);
            probeConnection = true;
        }

        // Determine how many hits we've gotten for this search
        int results = (
            _numResultsReportedByLeaf > 0 ?  // If the leaf we're searching for has told us how many hits it's gotten
            _numResultsReportedByLeaf :      // Use that number, otherwise
            RESULT_COUNTER.getNumResults()); // Check the RouteTableEntry for the number of hits we've routed back to the computer that wants them

        // Estimate how many hits each computer our search has reached generated
        double resultsPerHost = (double)results / (double)_theoreticalHostsQueried;

        // Get the number of additional hits this search needs
        int resultsNeeded = RESULTS - results;

        /*
         * Tour Point
         * 
         * Setting the TTL based on how many computers we want the search to reach.
         */

        // Set the TTL for our query packets based on how many computers we want to reach
        int hostsToQuery = 40000;                                                      // By default, guess that we'll need to search 40,000 more computers
        if (resultsPerHost != 0) hostsToQuery = (int)(resultsNeeded / resultsPerHost); // If we have resultsPerHost, calculate hostsToQuery
        int hostsToQueryPerConnection = hostsToQuery / remainingConnections;           // From that figure out how many computers we need to reach through each connection
        ttl = calculateNewTTL(                    // Set the TTL to reach that many computers
            hostsToQueryPerConnection,            // The number of computers we'd like this search to reach
            mc.getNumIntraUltrapeerConnections(), // The number of ultrapeers the remote computer we're going to send the query to is connected to, it told us with "X-Degree" in the handshake
            mc.headers().getMaxTTL());            // The largest TTL the remote computer wants to see, it told us with "X-Max-TTL" in the handshake

        /*
         * If we're sending the query down a probe connection and we've
         * already used that connection, or that connection doesn't have
         * a hit for the query, send it at TTL = 2. In these cases,
         * sending the query at TTL=1 is pointless because we've either
         * already sent this query, or the Ultrapeer doesn't have a
         * match anyway
         */

        // If calculateNewTTL gave us 1, and either this is the probe connection or an ultrapeer that supporst query routing, move the TTL up to 2
        if (ttl == 1 && ((mc.isUltrapeerQueryRoutingConnection() && !mc.shouldForwardQuery(QUERY)) || probeConnection)) ttl = 2;

        //zootella
        System.out.println("Chose TTL for regular query: " + ttl);

        // Make a copy of our query packet with the TTL we chose for it
        QueryRequest query = createQuery(QUERY, ttl);

        /*
         * send out the query on the network, returning the number of new
         * hosts theoretically reached
         */

        // Send the query to the chosen remote ultrapeer
        return sendQueryToHost(query, mc, this); // Return the number of computers we estimate the query packet will reach
	}

    /**
     * Send a given query packet to a given remote computer.
     * 
     * Leads to a call to mc.send(query) to actually send the remote computer the packet.
     * Adds the ManagedConnection object to our QUERIED_PROBE_CONNECTIONS or QUERIED_CONNECTIONS list so we don't search it again.
     * ProbeQuery.sendProbe() and sendQuery() above call this method.
     * 
     * @param query   A query packet
     * @param mc      The remote comptuer to send it to
     * @param handler The QueryHandler object that represents the search
     * @return        The number of computers we estimate the query packet will reach
     */
    static int sendQueryToHost(QueryRequest query, ManagedConnection mc, QueryHandler handler) {

        /*
         * send the query directly along the connection, but if the query didn't
         * go through send back 0....
         */

        // Send the query packet to the remote computer
        if (!_messageRouter.originateQuery(query, mc)) return 0; // Returns false if not sent, return 0 computers reached

        // Get the TTL we assigned this query
        byte ttl = query.getTTL();

        /*
         * add the reply handler to the list of queried hosts if it's not
         * a TTL = 1 query or the connection does not support probe queries
         * 
         * adds the connection to the list of probe connections if it's
         * a TTL = 1 query to a connection that supports probe extensions,
         * otherwise add it to the list of connections we've queried
         */

        // It's a TTL 1 query and the remote computer said "X-Ext-Probes: 0.1"
        if (ttl == 1 && mc.supportsProbeQueries()) {

            // Add the computer we're sending it to in our list of those we've sent the probe query
            handler.QUERIED_PROBE_CONNECTIONS.add(mc);

        // It's not a probe query
        } else {

            // Add the computer we're sending it to in our list of them we've searched
            handler.QUERIED_CONNECTIONS.add(mc);
            if (LOG.isTraceEnabled()) LOG.trace("QUERIED_CONNECTIONS.size() = " + handler.QUERIED_CONNECTIONS.size());
        }

        // Make a note
        if (LOG.isTraceEnabled()) LOG.trace("Querying host " + mc.getAddress() + " with ttl " + query.getTTL());

        // Set the time we'll next allow ourselves to send our query packet to another computer
        handler._nextQueryTime = System.currentTimeMillis() + (ttl * handler._timeToWaitPerHop); // Wait longer if the packet will hop farther

        // Have calculateNewHosts() figure out how many computers that reached, and return that number
        return calculateNewHosts(mc, ttl);
    }

	/**
     * Pick the TTL we'll send our query packet to an ultrapeer we're connected to.
     * Only sendQuery(List) calls this.
     * 
     * Loops, staring with a TTL of 1 and trying larger and larger TTLs.
     * Calculates how many computers our query will reach if we send it out with that TTL.
     * Returns the first TTL we find that's big enough.
     * 
     * @param hostToQueryPerConnection The number of computers we'd like this search to reach
     * @param degree                   The number of ultrapeers the remote computer is connected to, it told us "X-Degree" in the handshake
     * @param maxTTL                   The largest TTL the remote computer wants to see, it told us "X-Max-TTL" in the handshake
     * @return                         The TTL to use, like 1, 2, 3, 4, or 5
	 */
	private static byte calculateNewTTL(int hostsToQueryPerConnection, int degree, byte maxTTL) {

        // If the remote computer allows TTLs higher than 6, move it down to 6
        if (maxTTL > MAX_QUERY_TTL) maxTTL = MAX_QUERY_TTL;

        /*
         * not the most efficient algorithm -- should use Math.log, but
         * that's ok
         */

        // Try a TTL of 1, then 2, then 3, up to 5, until we make it big enough
        for (byte i = 1; i < MAX_QUERY_TTL; i++) {

            /*
             * biased towards lower TTLs since the horizon expands so
             * quickly
             */

            // Calculate how many computers the TTL would make our packet reach, and multiply that by 16
            int hosts = (int)(
                16.0 *                         // (2) Multiply that by 16 (do)
                calculateNewHosts(degree, i)); // (1) The number of computers our search will reach if we give it a TTL of i

            // The TTL we chose, i, is big enough
            if (hosts >= hostsToQueryPerConnection) {

                // If i is larger than the computer's maximum, return the computer's maximum instead
                if (i > maxTTL) return maxTTL;

                // Return the TTL we found that's big enough
                return i;
            }
        }

        // Even a TTL of 5 didn't do it, return the maximum the remote computer will allow
        return maxTTL;
	}

	/**
     * Calculate how many computers a query with the given TTL would reach if we send it to the given remote computer.
     * Looks at the "X-Degree" header the remote computer told us in the handshake to find out how many other ultrapeers it connects to.
     * Assumes all those ultrapeers also connect to that many more ultrapeers.
     * 
     * @param conn The remote computer we're going to send the query packet to
     * @param      A TTL we're thinking of using for our query packet
     * @return     The maximum number of computers that search will reach
	 */
	private static int calculateNewHosts(Connection conn, byte ttl) {

        // Calculate how many computers a query packet with the given TTL would reach when sent to the given remote computer
        return calculateNewHosts(
            conn.getNumIntraUltrapeerConnections(), // In the handshake, the remote computer said "X-Degree", telling us how many ultrapeers it connects to
            ttl);                                   // The TTL of the query we're trying
	}

	/**
     * Calculate how many computers a query with the given TTL would reach in a network where ultrapeers connect to degree number of other ultrapeers.
     * LimeWire ultrapeers connect to 32 other ultrapeers.
     * Given that degree number, here are some common numbers this function returns:
     * 
     * reached = calculateNewHosts(32, (byte)1); //     1
     * reached = calculateNewHosts(32, (byte)2); //    32
     * reached = calculateNewHosts(32, (byte)3); //   993
     * reached = calculateNewHosts(32, (byte)4); // 30784
     * 
     * Only calculateNewHosts() and calculateNewTTL above call this.
     * 
     * Calculate the number of new hosts that would be added to the
     * theoretical horizon if a query with the given ttl were sent to
     * a node with the given degree.  This is not precise because we're
     * assuming that the nodes connected to the node in question also
     * have the same degree, but there's not much we can do about it!
     * 
     * @param degree The number of ultrapeers the remote computer we're about to search told us it's connected to, like 32
     * @param        A TTL we're thinking of using for our query packet to it
     * @return       The maximum number of computers that search will reach
	 */
	private static int calculateNewHosts(int degree, byte ttl) {

        // Calculate degree ^ (ttl - 1), looping to add the number of computers reached in each expanding hop
		double newHosts = 0;
		for ( ; ttl > 0; ttl--) newHosts += Math.pow((degree - 1), ttl - 1);
		return (int)newHosts;
	}

	/**
     * Determine if this search has enough hits.
     * 
     * Calls RESULT_COUNTER.getNumResults() to find out how many hits we've routed back to the computer that wants them.
     * Compares this number to _numResultsReportedByLeaf, the number the leaf wants from us, and RESULTS, the goal when we created this QueryHandler object.
     * Also makes sure this search doesn't go on for more than 3 minutes 20 seconds.
     * 
     * @return True if it has and we can stop, false if it hasn't yet and we should keep going
	 */
	public boolean hasEnoughResults() {

        // If the search hasn't started yet, return false, we should keep going
		if (_queryStartTime == 0) return false;

        /*
         * NOTE: as agreed, _numResultsReportedByLeaf is the number of results
         * the leaf has received/consumed by a filter DIVIDED by 4 (4 being the
         * number of UPs connection it maintains).  That is why we don't divide
         * it here or anything.  We aren't sure if this mixes well with
         * BearShare's use but oh well....
         */

        /*
         * if leaf guidance is in effect, we have different criteria.
         */

        // We're searching for one of our leaves, and its told us how many hits it's gotten with a QueryStatusResponse vendor message
        if (_numResultsReportedByLeaf > 0) {

            // If we've already given the leaf 75 hits, stop searching
            if (RESULT_COUNTER.getNumResults() >= MAXIMUM_ROUTED_FOR_LEAVES) return true;

            // If the leaf says it has more hits than our hit goal the constructor set, stop searching
            if (_numResultsReportedByLeaf > RESULTS) return true;

        // We're searching for ourselves, or the leaf hasn't told us how many hits it has yet, and we've reached our goal
        } else if (RESULT_COUNTER.getNumResults() >= RESULTS) {

            /*
             * leaf guidance is not in effect or we are doing our own query
             */

            // Stop searching
            return true;
        }

        /*
         * if our theoretical horizon has gotten too high, consider
         * it enough results
         * precisely what this number should be is somewhat hard to determine
         * because, while connection have a specfic degree, the degree of
         * the connections on subsequent hops cannot be determined
         */

        // If our estimation of the number of computers our search has reached is larger than 110,000, stop searching
		if (_theoreticalHostsQueried > 110000) return true;

        /*
         * return true if we've been querying for longer than the specified
         * maximum
         */

        // If this search has been running for more than 200 seconds, return true to stop
        int queryLength = (int)(System.currentTimeMillis() - _queryStartTime);
        if (queryLength > MAX_QUERY_TIME) return true;

        // We don't have enough hits yet and we're still within the allowed time, return false to keep searching
		return false;
	}

    /**
     * Save the number of hits the leaf we've been running this search for tells us it has received.
     * 
     * We've been running a search for one of our leaves.
     * The leaf sent us a BEAR 12 1 QueryStatusResponse vendor message telling us how many hits it has.
     * MessageRouter.handleQueryStatus() called QueryDispatcher.updateLeafResultsForQuery(), which called this method.
     * If numResults is more than before, sets _numResultsReportedByLeaf.
     * 
     * @param numResults The number of results the leaf we've been running this search for says it has
     */
    public void updateLeafResults(int numResults) {

        // If this is more than the last count it gave us, save it
        if (numResults > _numResultsReportedByLeaf) _numResultsReportedByLeaf = numResults;
    }

    /**
     * Get the number of hits the leaf we've been running this search for has told us that it has received.
     * 
     * We've been running a search for one of our leaves.
     * The leaf sends us BEAR 12 1 QueryStatusResponse vendor messages telling us how many hits it has.
     * We save this number in _numResultsReportedByLeaf.
     * This method returns the value.
     * 
     * @return The number of results the leaf we've been running this search for says it has
     */
    public int getNumResultsReportedByLeaf() {

        // Return the number updateLeafResults() keeps saving
        return _numResultsReportedByLeaf;
    }

    /**
     * Get the ReplyHandler this QueryHandler holds.
     * Returns the ManagedConnection, UDPReplyHandler, or ForMeReplyHandler object that represents the computer that we're searching for and sending response packets to.
     * When you made this QueryHandler object, you passed a factory method this ReplyHandler object.
     * 
     * @return The ReplyHandler object that represents the remote computer we're searching for
     */
    public ReplyHandler getReplyHandler() {

        // Return the object we saved
        return REPLY_HANDLER;
    }

    /**
     * Find out how long this search is waiting between sending out query packets.
     * Returns the number it waits per hop.
     * For instance, if it most recently sent out a TTL 2 query packet, it will wait 2 * this number before sending another query packet.
     * 
     * @return The time in milliseconds
     */
    public long getTimeToWaitPerHop() {

        // Return the time that sendQuery() has been using and adjusting
        return _timeToWaitPerHop;
    }

    /**
     * Express this QueryHandler as text.
     * Overrides Object.toString().
     * 
     * @return A String
     */
	public String toString() {

        // Compose and return text
		return "QueryHandler: QUERY: "+QUERY; // Call QueryRequest.toString() to get the query packet to describe itself as text
	}

    /**
     * Get the GUID that identifies this search.
     * This is the message GUID of the query packet this QueryHandler keeps.
     * It's the query packet that began the search.
     * All the other packets used in the same search have this as their message GUID also.
     * We can use look up this GUID in our RouteTable for queries to send query hit packets back to the computer that wants them.
     * 
     * @return The GUID of the query packet this QueryHandler object is keeping
     */
    public GUID getGUID() {

        // Copy the GUID from the query packet, and return it
        return new GUID(QUERY.getGUID());
    }
}
