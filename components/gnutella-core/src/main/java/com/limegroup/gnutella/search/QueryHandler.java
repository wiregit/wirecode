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
	private final int RESULTS = 150;

	/**
	 * Constant for the query quid.
	 */
	private final byte[] GUID;

	/**
	 * Constamnt for the query string.
	 */
	private final String QUERY;

	/**
	 * Constant for the xml query string.
	 */
	private final String XML_QUERY;

	/**
	 * Constant for the types of urns to request -- any type for these
	 * queries.
	 */
	private static final Set URN_TYPES;

	/**
	 * Constant for the urns to request -- an empty set for the new
	 * queries issued by this factory.
	 */
	private static final Set QUERY_URNS;

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
	 * Variable for the number of times this query has attempted to go out
	 * but did not because it did not see any new connections to query down.
	 */
	//private int _timesNoNewQuerySent = 0;

	private long _queryStartTime = 0;

	// statically initialize an unmodifiable set of urn types so that
	// they'll be available for all instances
	static {
		Set urnTypes  = new HashSet();
		urnTypes.add(UrnType.ANY_TYPE);
		URN_TYPES = Collections.unmodifiableSet(urnTypes);
		QUERY_URNS = Collections.unmodifiableSet(new HashSet());
	}

	/**
	 * Private constructor to ensure that only this class creates new
	 * <tt>QueryFactory</tt> instances.
	 */
	private QueryHandler(byte[] guid, String query, String xmlQuery) {
		GUID = guid;
		QUERY = query;
		XML_QUERY = xmlQuery;
	}

	/**
	 * Factory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given set of query criteria.
	 *
	 * @param guid the guid for all <tt>QueryRequest</tt> instances  
	 *  generated from this query handler
	 * @param query the query string
	 * @param xmlQuery the xml query string
	 */
	public static QueryHandler createHandler(byte[] guid, String query, 
											 String xmlQuery) {
		return new QueryHandler(guid, query, xmlQuery);
	}


	/**
	 * Factory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @param guid the <tt>QueryRequest</tt> instance containing data
	 *  for this set of queries
	 */
	public static QueryHandler createHandler(QueryRequest query) {
		return new QueryHandler(query.getGUID(), query.getQuery(), 
								query.getRichQuery());
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
		return new QueryRequest(GUID, ttl, 0, QUERY, XML_QUERY, false, 
								URN_TYPES, QUERY_URNS, null,
								!RouterService.acceptedIncomingConnection());
	}

	/**
	 * Accessor for the guid for this query.
	 *
	 * @return the guid for this query
	 */
	public byte[] getGUID() {
		return GUID;
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

		int results = _resultCounter.getNumResults();

		long sysTime = System.currentTimeMillis();
		if(sysTime < _nextQueryTime) return;

		if(_queryStartTime == 0) {
			_queryStartTime = sysTime;
			sendProbeQuery();
			return;
		}

		List list = CONNECTION_MANAGER.getInitializedConnections2();
		int length = list.size();
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
			
			System.out.println("QueryHandler::sendQuery::ttl: "+ttl+" "+this+
							   " hostToQuery: "+hostsToQueryPerConnection); 
			QueryRequest query = createQuery(ttl);


			// send out the query on the network
			MESSAGE_ROUTER.sendQueryRequest(query, mc, null);

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
		System.out.println("QueryHandler::sendProbeQuery"); 
		List connections = CONNECTION_MANAGER.getInitializedConnections();

		byte ttl = 2;
		QueryRequest query = createQuery(ttl);
		int length = connections.size();
		for(int i=0; i<3; i++) {
			ManagedConnection mc = (ManagedConnection)connections.get(i);

			MESSAGE_ROUTER.sendQueryRequest(query, mc, null);

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
		if(hostsToQueryPerConnection < 5)     return 1;
		if(hostsToQueryPerConnection < 100)   return 2;
		if(hostsToQueryPerConnection < 300)   return 3;
		if(hostsToQueryPerConnection < 700)   return 4;
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
		//System.out.println("QueryHandler::hasEnoughResults::"+
		//			   _resultCounter.getNumResults());
		
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


	/**
	 * Creates a new <tt>QueryRequest</tt> instsnce, dynamically setting the
	 * TTL based on a variety of factors. 
	 *
	 * @param queriedConnections the number of connections that have already
	 *  been queried
	 * @param qh the <tt>QueryHandler</tt> instance that generates
	 *  queries for this dynamic query 
	 * @param remainingConnections the number of connections that have yet
	 *  to be queried
	 * @param results the number of results received so far for this query
	 */
	//private QueryRequest createQuery(int queriedConnections, 
	//							 int remainingConnections) {
	//return createQuery((byte)2);		
	//}

	// overrides Object.toString
	public String toString() {
		return "QueryHandler: QUERY: "+QUERY+" XML_QUERY: "+XML_QUERY;
	}
}
