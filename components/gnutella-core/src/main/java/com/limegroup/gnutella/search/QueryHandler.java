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
	 * Handle to the <tt>MessageRouter</tt> instance.  Non-final for
     * testing purposes.
	 */
	private static MessageRouter _messageRouter =
		RouterService.getMessageRouter();

	/**
	 * Handle to the <tt>ConnectionManager</tt> instance.  Non-final for
     * testing purposes.
	 */
	private static ConnectionManager _connectionManager =
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
	private int _theoreticalHostsQueried = 1;

	/**
	 * Variable for the <tt>ResultCounter</tt> for this query -- used
	 * to access the number of replies returned.
	 */
	private ResultCounter _resultCounter;

	/**
	 * Constant set of send handlers that have already been queried.
	 */
	private final Set QUERIED_HANDLERS = new HashSet();

	/**
	 * The time the query started.
	 */
	private long _queryStartTime = 0;

    /**
     * The current time, taken each time the query is initiated again.
     */
    private long _curTime = 0;

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
	public static QueryRequest createQuery(QueryRequest query, byte ttl) {
		if(ttl < 1 || ttl > 6) 
			throw new IllegalArgumentException("ttl too high: "+ttl);

		// build it from scratch if it's from us
		if(query.getHops() == 0) {
			return QueryRequest.createQuery(query, ttl);
		} else {
			try {
				return QueryRequest.createNetworkQuery(query.getGUID(), ttl, 
													   query.getHops(), 
													   query.getPayload(),
													   query.getNetwork());
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

		_curTime = System.currentTimeMillis();
		if(_curTime < _nextQueryTime) return;

		if(_queryStartTime == 0) {
			_queryStartTime = _curTime;
            _theoreticalHostsQueried += 
                sendProbeQuery(this, 
                               _connectionManager.getInitializedConnections2()); 
            _nextQueryTime = System.currentTimeMillis() + 6000;
            return;
		}

        
        _theoreticalHostsQueried += 
            sendQuery(this, _connectionManager.getInitializedConnections2()); 
        _nextQueryTime = System.currentTimeMillis() + 1500;
    }

    /**
     * Runs the query over the given list of connections.  This is static
     * to decouple the algorithm from the specific <tt>QueryHandler</tt>
     * instance, making testing significantly easier.
     *
     * @param handler the <tt>QueryHandler</tt> instance containing data
     *  for this query
     * @param list the <tt>List</tt> of Gnutella connections to send
     *  queries over
     * @return the number of new hosts theoretically reached by this
     *  query iteration
     */
    private static int sendQuery(QueryHandler handler, List list) {
		int length = list.size();
        int newHosts = 0;
        for(int i=0; i<length; i++) {
			ManagedConnection mc = (ManagedConnection)list.get(i);			

			// if the connection hasn't been up for long, don't use it,
            // as the replies will never make it back to us if the
            // connection is dropped, wasting bandwidth
            if(!mc.isStable(handler._curTime)) continue;
                
			// if we've already queried this host, go to the next one
			if(handler.QUERIED_HANDLERS.contains(mc)) continue;
			
			int hostsQueried = handler.QUERIED_HANDLERS.size();
			
			// assume there's minimal overlap between the connections
			// we queried before and the new ones 
            
            // also, pretend we have fewer connections than we do
            // in case they go away
			int remainingConnections = length - hostsQueried - 4;
			remainingConnections = Math.max(remainingConnections, 1);
			
			int results = handler._resultCounter.getNumResults();
			double resultsPerHost = 
				(double)results/(double)handler._theoreticalHostsQueried;
			
			int resultsNeeded = handler.RESULTS - results;
			
			int hostsToQuery = 80000;
			if(resultsPerHost != 0) {
				hostsToQuery = (int)((double)resultsNeeded/resultsPerHost);
			}
			
			int hostsToQueryPerConnection = 
				hostsToQuery/remainingConnections;			
            byte maxTTL = mc.headers().getMaxTTL();

			byte ttl = 
                calculateNewTTL(hostsToQueryPerConnection, 
                                mc.getNumIntraUltrapeerConnections(),
                                mc.headers().getMaxTTL());

			QueryRequest query = createQuery(handler.QUERY, ttl);

 
			// send out the query on the network
			RouterService.getMessageRouter().sendQueryRequest(query, mc, 
                                                              handler.REPLY_HANDLER);

			// add the reply handler to the list of queried hosts
			handler.QUERIED_HANDLERS.add(mc);
            newHosts = calculateNewHosts(mc, ttl);
            break;
		}
        return newHosts;
	}

	/**
	 * Send the initial "probe" query to get an idea of how widely distributed
     * the content is.
     *
     * @param queriedHosts the set of hosts that have already been queried
     * @return the next time to send out the query -- another query should
     *  not be sent before this
	 */
	private static int sendProbeQuery(QueryHandler handler, List list) {
		byte ttl = 2;
		QueryRequest query = createQuery(handler.QUERY, ttl);
        int newHosts = 0;

        int hostsQueried = 0;
        int i = 0;
        while(hostsQueried<3 && i<list.size()) {
			ManagedConnection mc = (ManagedConnection)list.get(i);
            if(!mc.isStable()) {
                // count the index
                i++;
                continue;
            }
			RouterService.getMessageRouter().sendQueryRequest(query, mc, 
                                                              handler.REPLY_HANDLER);

			// add the reply handler to the list of queried hosts
			handler.QUERIED_HANDLERS.add(mc);

			newHosts += calculateNewHosts(mc, ttl);

            hostsQueried++;
            i++;
		}
        return newHosts;
	}

	/**
	 * Calculates the new TTL to use based on the number of hosts per connection
	 * we still need to query.
	 * 
	 * @param hostsToQueryPerConnection the number of hosts we should reach on
	 *  each remaining connections, to the best of our knowledge
     * @param degree the out-degree of the next connection
     * @param maxTTL the maximum TTL the connection will allow
	 */
	private static byte 
        calculateNewTTL(int hostsToQueryPerConnection, int degree, byte maxTTL) {
        
        // not the most efficient algorithm -- should use Math.log, but
        // that's ok
        for(byte i=1; i<6; i++) {

            // biased towards lower TTLs since the horizon expands so
            // quickly
            int hosts = (int)(1.5*calculateNewHosts(degree, i));
            if(hosts >= hostsToQueryPerConnection) {
                return i;
            }
        }
        return maxTTL;
	}

	/**
     * Calculate the number of new hosts that would be added to the 
     * theoretical horizon if a query with the given ttl were sent down
     * the given connection.
	 *
     * @param conn the <tt>Connection</tt> that will received the query
	 * @param ttl the TTL of the query to add
	 */
	private static int calculateNewHosts(Connection conn, byte ttl) {
        return calculateNewHosts(conn.getNumIntraUltrapeerConnections(), ttl);
	}

	/**
     * Calculate the number of new hosts that would be added to the 
     * theoretical horizon if a query with the given ttl were sent to
     * a node with the given degree.  This is not precise because we're
     * assuming that the nodes connected to the node in question also
     * have the same degree, but there's not much we can do about it!
	 *
     * @param degree the degree of the node that will received the query
	 * @param ttl the TTL of the query to add
	 */    
	private static int calculateNewHosts(int degree, byte ttl) {
		double newHosts = 0;
		for(;ttl>0; ttl--) {
			newHosts += Math.pow((degree-1), ttl-1);
		}
		return (int)newHosts;
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
	 
        // if our theoretical horizon has gotten too high, consider
        // it enough results
        // precisely what this number should be is somewhat hard to determine
        // because, while connection have a specfic degree, the degree of 
        // the connections they have cannot be determined
		if(_theoreticalHostsQueried > 100000) return true;

		// return true if we've been querying for longer than the specified 
		// maximum
		int queryLength = (int)(System.currentTimeMillis() - _queryStartTime);
		if(queryLength > 60*1000) return true;

		return false;
	}

	// overrides Object.toString
	public String toString() {
		return "QueryHandler: QUERY: "+QUERY;
	}
}
