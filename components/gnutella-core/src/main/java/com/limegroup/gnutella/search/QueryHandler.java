package com.limegroup.gnutella.search;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;

/**
 * This class is a factory for creating <tt>QueryRequest</tt> instances
 * for dynamic queries.  Dynamic queries adjust to the varying conditions of
 * a query, such as the number of results received, the number of nodes
 * hit or theoretically hit, etc.  This class makes it convenient to 
 * rapidly generate <tt>QueryRequest</tt>s with similar characteristics, 
 * such as guids, the query itself, the xml query, etc, but with customized
 * settings, such as the TTL.
 */
public final class QueryHandler {

	/**
	 * Constant for the number of results to look for.
	 */
	private final int RESULTS;

	/**
	 * The number of results to try to get if we're an Ultrapeer originating
	 * the query.
	 */
	private static final int ULTRAPEER_RESULTS = 160;

	/**
	 * The number of results to try to get if the query came from an old
	 * leaf -- they are connected to 2 other Ultrapeers that may or may
	 * not use this algorithm.
	 */
	private static final int OLD_LEAF_RESULTS = 50;

	/**
	 * The number of results to try to get for new leaves -- they only 
	 * maintain 2 connections and don't generate as much overall traffic,
	 * so give them a little more.
	 */
	private static final int NEW_LEAF_RESULTS = 80;

	/**
	 * The number of results to try to get for queries by hash -- really
	 * small since you need relatively few exact matches.
	 */
	private static final int HASH_QUERY_RESULTS = 10;


	/**
	 * Constant handle to the <tt>MessageRouter</tt> instance.
	 */
	private static final MessageRouter MESSAGE_ROUTER =
		RouterService.getMessageRouter();

	/**
	 * Constant handle to the <tt>ConnectionManager</tt> instance.
	 */
	private static final ConnectionManager CONNECTION_MANAGER =
		RouterService.getConnectionManager();

	/**
	 * Variable for the number of hosts that have been queried.
	 */
	private int _hostsQueried = 0;

	/**
	 * Variable for the next time after which a query should be sent.
	 */
	private long _nextQueryTime = 0;

	/**
	 * The theoretical number of hosts that have been reached by this query.
	 */
	private int _theoreticalHostsQueried = 0;

	/**
	 * Variable for the <tt>ResultCounter</tt> for this query -- used
	 * to access the number of replies returned.
	 */
	private ResultCounter _resultCounter;

	/**
	 * Constant set of <tt>ReplyHandler</tt>s that have already been queried.
	 */
	private final Set QUERIED_REPLY_HANDLERS = new HashSet();

	/**
	 * The time the query started.
	 */
	private long _queryStartTime = 0;

	/**
	 * <tt>ReplyHandler</tt> for replies received for this query.
	 */
	private final ReplyHandler REPLY_HANDLER;

	/**
	 * Constant for the <tt>QueryRequest</tt> used to build new queries.
	 */
	private final QueryRequest QUERY;


	/**
	 * Private constructor to ensure that only this class creates new
	 * <tt>QueryFactory</tt> instances.
	 *
	 * @param request the <tt>QueryRequest</tt> to construct a handler for
	 * @param results the number of results to get -- this varies based
	 *  on the type of servant sending the request and is respeceted unless
	 *  it's a query for a specific hash, in which case we try to get
	 *  far fewer matches, ignoring this parameter
	 * @param handler the <tt>ReplyHandler</tt> for routing replies
	 */
	private QueryHandler(QueryRequest query, int results, ReplyHandler handler) {
		boolean isHashQuery = !query.getQueryUrns().isEmpty();
		QUERY = query;
		if(isHashQuery) {
			RESULTS = HASH_QUERY_RESULTS;
		} else {
			RESULTS = results;
		}

		REPLY_HANDLER = handler;
	}


	/**
	 * Factory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @param guid the <tt>QueryRequest</tt> instance containing data
	 *  for this set of queries
	 * @param handler the <tt>ReplyHandler</tt> for routing the replies
	 * @return the <tt>QueryHandler</tt> instance for this query
	 */
	public static QueryHandler createHandler(QueryRequest query, 
											 ReplyHandler handler) {	
		return new QueryHandler(query, ULTRAPEER_RESULTS, handler);
	}

	/**
	 * Factory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @param guid the <tt>QueryRequest</tt> instance containing data
	 *  for this set of queries
	 * @param handler the <tt>ReplyHandler</tt> for routing the replies
	 * @return the <tt>QueryHandler</tt> instance for this query
	 */
	public static QueryHandler createHandlerForOldLeaf(QueryRequest query, 
													   ReplyHandler handler) {	
		return new QueryHandler(query, OLD_LEAF_RESULTS, handler);
	}

	/**
	 * Factory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @param guid the <tt>QueryRequest</tt> instance containing data
	 *  for this set of queries
	 * @param handler the <tt>ReplyHandler</tt> for routing the replies
	 * @return the <tt>QueryHandler</tt> instance for this query
	 */
	public static QueryHandler createHandlerForNewLeaf(QueryRequest query, 
													   ReplyHandler handler) {		
		return new QueryHandler(query, NEW_LEAF_RESULTS, handler);
	}

	/**
	 * Factory method for creating new <tt>QueryRequest</tt> instances with
	 * the same guid, query, xml query, urn types, etc.
	 *
	 * @param ttl the time to live of the new query
	 * @return a new <tt>QueryRequest</tt> instance with all of the 
	 *  pre-defined parameters and the specified TTL
	 * @throw <tt>IllegalArgumentException</tt> if the ttl is not within
	 *  what is considered reasonable bounds
	 */
	public QueryRequest createQuery(byte ttl) {
		if(ttl < 1 || ttl > 6) 
			throw new IllegalArgumentException("ttl too high: "+ttl);

		// build it from scratch if it's from us
		if(QUERY.getHops() == 0) {
			return QueryRequest.createQuery(QUERY, ttl);
		} else {
			try {
				return QueryRequest.createNetworkQuery(QUERY.getGUID(), ttl, 
													   QUERY.getHops(), 
													   QUERY.getPayload(),
													   QUERY.getNetwork());
			} catch(BadPacketException e) {
				// this should never happen, since the query was already 
				// read from the network, so report an error
				ErrorService.error(e);
				return null;
			}
		}
	}


	/**
	 * Sets the <tt>ResultCounter</tt> for this query.
	 *
	 * @param entry the <tt>ResultCounter</tt> to add
	 */
	public void setResultCounter(ResultCounter entry) {
		if(entry == null) {
			throw new NullPointerException("null route table entry");
		}
		_resultCounter = entry;
	}
	
	/**
	 * Sends the query to the current connections.  If the query is not
	 * yet ready to be processed, this returns immediately.
	 *
	 * @throws <tt>NullPointerException</tt> if the route table entry
	 *  is <tt>null</tt>
	 */
	public void sendQuery() {
		// do not allow the route table entry to be null
		if(_resultCounter == null) {
			throw new NullPointerException("null route table entry");
		}
		if(hasEnoughResults()) return;

		long sysTime = System.currentTimeMillis();
		if(sysTime < _nextQueryTime) return;

		List list = CONNECTION_MANAGER.getInitializedConnections2();
		int length = list.size();
		if(_queryStartTime == 0) {
			_queryStartTime = sysTime;
			sendProbeQuery();
			return;
		}

		for(int i=0; i<length; i++) {
			ManagedConnection mc = (ManagedConnection)list.get(i);			
			
			// if we've already queried this host, go to the next one
			if(QUERIED_REPLY_HANDLERS.contains(mc)) continue;
			
			int hostsQueried = QUERIED_REPLY_HANDLERS.size();
			
			// assume there's minimal overlap between the connections
			// we queried before and the new ones -- pretend we have
			// fewer connections than we do
			int remainingConnections = length - hostsQueried - 2;
			remainingConnections = Math.max(remainingConnections, 1);
			
			int results = _resultCounter.getNumResults();
			double resultsPerHost = 
				(double)results/(double)_theoreticalHostsQueried;
			
			int resultsNeeded = RESULTS - results;
			
			int hostsToQuery = 100000;
			if(resultsPerHost != 0) {
				hostsToQuery = (int)((double)resultsNeeded/resultsPerHost);
			}
			
			int hostsToQueryPerConnection = 
				hostsToQuery/remainingConnections;
			byte ttl = calculateNewTTL(hostsToQueryPerConnection);
			
			//if(ttl == 4 && remainingConnections > 4) {
			//ttl = 3;
			//}
			QueryRequest query = createQuery(ttl);

 
			// send out the query on the network
			MESSAGE_ROUTER.sendQueryRequest(query, mc, REPLY_HANDLER);

			// add the reply handler to the list of queried hosts
			QUERIED_REPLY_HANDLERS.add(mc);

			// adjust the next query time according to the ttl of what we
			// just sent
			_nextQueryTime = System.currentTimeMillis()+1500;

			adjustTheoreticalHostsQueried(mc, ttl);

			break;
		}	
	}

	/**
	 * Send the initial "probe" query to get an idea of how widely di
	 */
	private void sendProbeQuery() {
		List connections = CONNECTION_MANAGER.getInitializedConnections2();

		byte ttl = 2;
		QueryRequest query = createQuery(ttl);
        int limit = Math.min(2, connections.size());
		for(int i=0; i<limit; i++) {
			ManagedConnection mc = (ManagedConnection)connections.get(i);

			MESSAGE_ROUTER.sendQueryRequest(query, mc, REPLY_HANDLER);

			// add the reply handler to the list of queried hosts
			QUERIED_REPLY_HANDLERS.add(mc);

			adjustTheoreticalHostsQueried(mc, ttl);
		}
		_nextQueryTime = System.currentTimeMillis() + 6000;
	}

	/**
	 * Calculates the new TTL to use based on the number of hosts per connection
	 * we still need to query.
	 * 
	 * @param hostsToQueryPerConnection the number of hosts we should reach on
	 *  each remaining connections, to the best of our knowledge
	 */
	private byte calculateNewTTL(int hostsToQueryPerConnection) {
		// the limits below are based on experimental, not theoretical 
		// observations of results on the network
		if(hostsToQueryPerConnection < 30)    return 1;
		if(hostsToQueryPerConnection < 100)   return 2;
		if(hostsToQueryPerConnection < 1000)  return 3;
		if(hostsToQueryPerConnection < 2000)  return 4;
		else return 5;
	}

	/**
	 * Modifies the number of hosts theoretically reached by this query by
	 * adding a query with the given TTL.
	 *
	 * @param ttl the TTL of the query to add
	 */
	private void adjustTheoreticalHostsQueried(ManagedConnection mc, int ttl) {
		final int intraUltrapeerConnections = 
			mc.getNumIntraUltrapeerConnections();
		double newHosts = 0;
		for(;ttl>0; ttl--) {
			newHosts += Math.pow(intraUltrapeerConnections, ttl-1);
		}
		_theoreticalHostsQueried += (int)newHosts;
	}

	/**
	 * Returns whether or not this query has received enough results.
	 *
	 * @return <tt>true</tt> if this query has received enough results,
	 *  <tt>false</tt> otherwise
	 */
	public boolean hasEnoughResults() {		
		// return false if the query hasn't started yet
		if(_queryStartTime == 0) return false;

		if(_resultCounter.getNumResults() >= RESULTS) return true;
	 
		if(_theoreticalHostsQueried > 160000) return true;

		// return true if we've been querying for longer than the specified 
		// maximum
		int queryLength = (int)(System.currentTimeMillis() - _queryStartTime);
		if(queryLength > 80*1000) return true;

		return false;
	}

	// overrides Object.toString
	public String toString() {
		return "QueryHandler: QUERY: "+QUERY;
	}
}
