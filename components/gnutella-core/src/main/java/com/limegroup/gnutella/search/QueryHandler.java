
package com.limegroup.gnutella.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inspection.Inspectable;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ConnectionManager;
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
 * This class is a factory for creating <tt>QueryRequest</tt> instances
 * for dynamic queries.  Dynamic queries adjust to the varying conditions of
 * a query, such as the number of results received, the number of nodes
 * hit or theoretically hit, etc.  This class makes it convenient to 
 * rapidly generate <tt>QueryRequest</tt>s with similar characteristics, 
 * such as guids, the query itself, the xml query, etc, but with customized
 * settings, such as the TTL.
 */
public final class QueryHandler implements Inspectable {
    
    private static final Log LOG = LogFactory.getLog(QueryHandler.class);

	/**
	 * Constant for the number of results to look for.
	 */
	private final int RESULTS;

    /**
     * Constant for the max TTL for a query.
     */
    public static final byte MAX_QUERY_TTL = (byte) 6;

	/**
	 * The number of results to try to get if we're an Ultrapeer originating
	 * the query.
	 */
	public static final int ULTRAPEER_RESULTS = 150;

    /**
     * Ultrapeers seem to get less results - lets give them a little boost.
     */
    public static final double UP_RESULT_BUMP = 1.15;


	/**
	 * The number of results to try to get if the query came from an old
	 * leaf -- they are connected to 2 other Ultrapeers that may or may
	 * not use this algorithm.
	 */
	private static final int OLD_LEAF_RESULTS = 20;

	/**
	 * The number of results to try to get for new leaves -- they only 
	 * maintain 2 connections and don't generate as much overall traffic,
	 * so give them a little more.
	 */
	private static final int NEW_LEAF_RESULTS = 38;

	/**
	 * The number of results to try to get for queries by hash -- really
	 * small since you need relatively few exact matches.
	 */
	private static final int HASH_QUERY_RESULTS = 10;

    /**
     * If Leaf Guidance is in effect, the maximum number of hits to route.
     */
    private static final int MAXIMUM_ROUTED_FOR_LEAVES = 75;

    /**
     * The number of milliseconds to wait per query hop.  So, if we send
     * out a TTL=3 query, we will then wait TTL*_timeToWaitPerHop
     * milliseconds.  As the query continues and we gather more data
     * regarding the popularity of the file, this number may decrease.
     */
    private volatile long _timeToWaitPerHop = 2400;

    /**
     * Variable for the number of milliseconds to shave off of the time
     * to wait per hop after a certain point in the query.  As the query
     * continues, the time to shave may increase as well.
     */
    private volatile long _timeToDecreasePerHop = 10;

    /**
     * Variable for the number of times we've decremented the per hop wait
     * time.  This is used to determine how much more we should decrement
     * it on this pass.
     */
    private volatile int _numDecrements = 0;

    /**
     * Constant for the maximum number of milliseconds the entire query
     * can last.  The query expires when this limit is reached.
     */
    public static final int MAX_QUERY_TIME = 200 * 1000;
    
    /** List of times since start of query that results were updated */
    private final List<Long> times = new ArrayList<Long>();
    
    /** Number of results reported each update */
    private final List<Integer> results = new ArrayList<Integer>();


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
     * Variable for the number of results the leaf reports it has.
     */
    private volatile int _numResultsReportedByLeaf = 0;

	/**
	 * Variable for the next time after which a query should be sent.
	 */
	private volatile long _nextQueryTime = 0;

	/**
	 * The theoretical number of hosts that have been reached by this query.
	 */
	private volatile int _theoreticalHostsQueried = 1;

	/**
	 * Constant for the <tt>ResultCounter</tt> for this query -- used
	 * to access the number of replies returned.
	 */
	private final ResultCounter RESULT_COUNTER;

	/**
	 * Constant list of connections that have already been queried.
	 */
	private final List<ManagedConnection> QUERIED_CONNECTIONS = new ArrayList<ManagedConnection>();

    /**
     * <tt>List</tt> of TTL=1 probe connections that we've already used.
     */
    private final List<ManagedConnection> QUERIED_PROBE_CONNECTIONS = new ArrayList<ManagedConnection>();

	/**
	 * The time the query started.
	 */
	private volatile long _queryStartTime = 0;

    /**
     * The current time, taken each time the query is initiated again.
     */
    private volatile long _curTime = 0;

	/**
	 * <tt>ReplyHandler</tt> for replies received for this query.
	 */
	private final ReplyHandler REPLY_HANDLER;

	/**
	 * Constant for the <tt>QueryRequest</tt> used to build new queries.
	 */
	final QueryRequest QUERY;

    /**
     * Boolean for whether or not the query has been forwarded to leaves of 
     * this ultrapeer.
     */
    private volatile boolean _forwardedToLeaves = false;


    /**
     * Boolean for whether or not we've sent the probe query.
     */
    private boolean _probeQuerySent;

    /**
     * used to preference which connections to use when searching
     * if the search comes from a leaf with a certain locale preference
     * then those connections (of this ultrapeer) which match the 
     * locale will be used before the other connections.
     */
    private final String _prefLocale;

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
     * @param counter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
	 */
	private QueryHandler(QueryRequest query, int results, ReplyHandler handler,
                         ResultCounter counter) {
        if( query == null )
            throw new IllegalArgumentException("null query");
        if( handler == null )
            throw new IllegalArgumentException("null reply handler");
        if( counter == null )
            throw new IllegalArgumentException("null result counter");
            
		boolean isHashQuery = !query.getQueryUrns().isEmpty();
		QUERY = query;
		if(isHashQuery) {
			RESULTS = HASH_QUERY_RESULTS;
		} else {
			RESULTS = results;
		}

		REPLY_HANDLER = handler;
        RESULT_COUNTER = counter;
        _prefLocale = handler.getLocalePref();
	}


	/**
	 * Factory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @param guid the <tt>QueryRequest</tt> instance containing data
	 *  for this set of queries
	 * @param handler the <tt>ReplyHandler</tt> for routing the replies
     * @param counter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
	 * @return the <tt>QueryHandler</tt> instance for this query
	 */
	public static QueryHandler createHandler(QueryRequest query, 
											 ReplyHandler handler,
                                             ResultCounter counter) {	
		return new QueryHandler(query, ULTRAPEER_RESULTS, handler, counter);
	}

	/**
	 * Factory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.  Used by supernodes to run
     * their own queries (ties up to ForMeReplyHandler.instance()).
	 *
	 * @param guid the <tt>QueryRequest</tt> instance containing data
	 *  for this set of queries
     * @param counter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
	 * @return the <tt>QueryHandler</tt> instance for this query
	 */
	public static QueryHandler createHandlerForMe(QueryRequest query, 
                                                  ResultCounter counter) {	
        // because UPs seem to get less results, give them more than usual
		return new QueryHandler(query, (int)(ULTRAPEER_RESULTS * UP_RESULT_BUMP),
                                ForMeReplyHandler.instance(), counter);
	}

	/**
	 * Factory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @param guid the <tt>QueryRequest</tt> instance containing data
	 *  for this set of queries
	 * @param handler the <tt>ReplyHandler</tt> for routing the replies
     * @param counter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
	 * @return the <tt>QueryHandler</tt> instance for this query
	 */
	public static QueryHandler createHandlerForOldLeaf(QueryRequest query, 
													   ReplyHandler handler,
                                                       ResultCounter counter) {	
		return new QueryHandler(query, OLD_LEAF_RESULTS, handler, counter);
	}

	/**
	 * Factory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @param guid the <tt>QueryRequest</tt> instance containing data
	 *  for this set of queries
	 * @param handler the <tt>ReplyHandler</tt> for routing the replies
     * @param counter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
	 * @return the <tt>QueryHandler</tt> instance for this query
	 */
	public static QueryHandler createHandlerForNewLeaf(QueryRequest query, 
													   ReplyHandler handler,
                                                       ResultCounter counter) {		
		return new QueryHandler(query, NEW_LEAF_RESULTS, handler, counter);
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
	 * @throw NullPointerException if the <tt>query</tt> argument is 
	 *    <tt>null</tt>
	 */
	public static QueryRequest createQuery(QueryRequest query, byte ttl) {
		if(ttl < 1 || ttl > MAX_QUERY_TTL) 
			throw new IllegalArgumentException("ttl too high: "+ttl);
		if(query == null) {
			throw new NullPointerException("null query");
		}

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
     * Convenience method for creating a new query with the given TTL
     * with this <tt>QueryHandler</tt>.
     *
     * @param ttl the time to live for the new query
     */
    QueryRequest createQuery(byte ttl) {
        return createQuery(QUERY, ttl);
    }


	
	/**
	 * Sends the query to the current connections.  If the query is not
	 * yet ready to be processed, this returns immediately.
	 */
	public void sendQuery() {
		if(hasEnoughResults()) return;

		_curTime = System.currentTimeMillis();
		if(_curTime < _nextQueryTime) return;

        if (LOG.isTraceEnabled())
            LOG.trace("Query = " + QUERY.getQuery() +
                      ", numHostsQueried: " + _theoreticalHostsQueried);

		if(_queryStartTime == 0) {
			_queryStartTime = _curTime;
        }
            
        // handle 3 query cases

        // 1) If we haven't sent the query to our leaves, send it
        if(!_forwardedToLeaves) {

            _forwardedToLeaves = true;
            QueryRouteTable qrt = 
                RouterService.getMessageRouter().getQueryRouteTable();

            QueryRequest query = createQuery(QUERY, (byte)1);

            _theoreticalHostsQueried += 25;
                
            // send the query to our leaves if there's a hit and wait,
            // otherwise we'll move on to the probe
            if(qrt != null && qrt.contains(query)) {
                RouterService.getMessageRouter().
                    forwardQueryRequestToLeaves(query, 
                                                REPLY_HANDLER); 
                _nextQueryTime = 
                    System.currentTimeMillis() + _timeToWaitPerHop;
                return;
            }
        }
        
        // 2) If we haven't sent the probe query, send it
        if(!_probeQuerySent) {
            ProbeQuery pq = 
                new ProbeQuery(_connectionManager.getInitializedConnections(),
                               this);
            long timeToWait = pq.getTimeToWait();            
            _theoreticalHostsQueried += pq.sendProbe();
            _nextQueryTime = 
                System.currentTimeMillis() + timeToWait;
            _probeQuerySent = true;
            return;
        }

        // 3) If we haven't yet satisfied the query, keep trying
        else {
            // Otherwise, just send a normal query -- make a copy of the 
            // connections because we'll be modifying it.
            int newHosts = 
                sendQuery(
                    new ArrayList<ManagedConnection>(
                            _connectionManager.getInitializedConnections()));
            if(newHosts == 0) {
                // if we didn't query any new hosts, wait awhile for new
                // connections to potentially appear
                _nextQueryTime = System.currentTimeMillis() + 6000;
            }   
            _theoreticalHostsQueried += newHosts;

            // if we've already queried quite a few hosts, not gotten
            // many results, and have been querying for awhile, start
            // decreasing the per-hop wait time
            if(_timeToWaitPerHop > 100 &&
               (System.currentTimeMillis() - _queryStartTime) > 6000) {
                _timeToWaitPerHop -= _timeToDecreasePerHop;

                int resultFactor =
                    Math.max(1, 
                        (RESULTS/2)-(30*RESULT_COUNTER.getNumResults()));

                int decrementFactor = Math.max(1, (_numDecrements/6));

                // the current decrease is weighted based on the number
                // of results returned and on the number of connections
                // we've tried -- the fewer results and the more 
                // connections, the more the decrease
                int currentDecrease = resultFactor * decrementFactor;

                currentDecrease = 
                    Math.max(5, currentDecrease);
                _timeToDecreasePerHop += currentDecrease; 

                _numDecrements++;
                if(_timeToWaitPerHop < 100) {
                    _timeToWaitPerHop = 100;
                }
            }
        }
    }

    /**
     * Sends a query to one of the specified <tt>List</tt> of connections.  
     * This is the heart of the dynamic query.  We dynamically calculate the
     * appropriate TTL to use based on our current estimate of how widely the
     * file is distributed, how many connections we have, etc.  This is static
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
    private int sendQuery(List<? extends ManagedConnection> ultrapeersAll) {

        //we want to try to use all connections in ultrapeersLocale first.
        List<? extends ManagedConnection> ultrapeers = // method returns a copy
            _connectionManager.getInitializedConnectionsMatchLocale
            (_prefLocale);
            
        QUERIED_CONNECTIONS.retainAll(ultrapeersAll);
        QUERIED_PROBE_CONNECTIONS.retainAll(ultrapeersAll);
        
        //if we did get a list of connections that matches the locale
        //of the query
        if(!ultrapeers.isEmpty()) {
            ultrapeers.removeAll(QUERIED_CONNECTIONS);
            ultrapeers.removeAll(QUERIED_PROBE_CONNECTIONS);
            //at this point ultrapeers could become empty
        }
                
        if(ultrapeers.isEmpty()) { 
            ultrapeers = ultrapeersAll;
            // now, remove any connections we've used from our current list
            // of connections to try
            ultrapeers.removeAll(QUERIED_CONNECTIONS);
            ultrapeers.removeAll(QUERIED_PROBE_CONNECTIONS);
        }

		int length = ultrapeers.size();
        if (LOG.isTraceEnabled())
            LOG.trace("potential querier size: " + length);
        byte ttl = 0;
        ManagedConnection mc = null;

        // add randomization to who we send our queries to
        Collections.shuffle(ultrapeers);

        // weed out all connections that aren't yet stable
        for(int i=0; i<length; i++) {
			ManagedConnection curConnection = ultrapeers.get(i);			

			// if the connection hasn't been up for long, don't use it,
            // as the replies will never make it back to us if the
            // connection is dropped, wasting bandwidth
            if(!curConnection.isStable(_curTime)) continue;
            mc = curConnection;
            break;
        }

        int remainingConnections = 
            Math.max(length+QUERIED_PROBE_CONNECTIONS.size(), 0);

        // return if we don't have any connections to query at this time
        if(remainingConnections == 0) return 0;

        // pretend we have fewer connections than we do in case we
        // lose some
        if(remainingConnections > 4) remainingConnections -= 4;

        boolean probeConnection = false;
        
        // mc can still be null if the list of connections was empty.
        if(mc == null) {
            // if we have no connections to query, simply return for now
            if(QUERIED_PROBE_CONNECTIONS.isEmpty()) {
                return 0;
            }
            
            // we actually remove this from the list to make sure that
            // QUERIED_CONNECTIONS and QUERIED_PROBE_CONNECTIONS do
            // not have any of the same entries, as this connection
            // will be added to QUERIED_CONNECTIONS
            mc = QUERIED_PROBE_CONNECTIONS.remove(0);
            probeConnection = true;
        }
        
        int results = (_numResultsReportedByLeaf > 0 ? 
                       _numResultsReportedByLeaf : 
                       RESULT_COUNTER.getNumResults());
        double resultsPerHost = 
            (double)results/(double)_theoreticalHostsQueried;
			
        int resultsNeeded = RESULTS - results;
        int hostsToQuery = 40000;
        if(resultsPerHost != 0) {
            hostsToQuery = (int)(resultsNeeded/resultsPerHost);
        }
                
        
        int hostsToQueryPerConnection = 
            hostsToQuery/remainingConnections;;
        
        ttl = calculateNewTTL(hostsToQueryPerConnection, 
                              mc.getNumIntraUltrapeerConnections(),
                              mc.headers().getMaxTTL());
               

        // If we're sending the query down a probe connection and we've
        // already used that connection, or that connection doesn't have
        // a hit for the query, send it at TTL=2.  In these cases, 
        // sending the query at TTL=1 is pointless because we've either
        // already sent this query, or the Ultrapeer doesn't have a 
        // match anyway
        if(ttl == 1 && 
           ((mc.isUltrapeerQueryRoutingConnection() &&
            !mc.shouldForwardQuery(QUERY)) || probeConnection)) {
            ttl = 2;
        }
        QueryRequest query = createQuery(QUERY, ttl);

        // send out the query on the network, returning the number of new
        // hosts theoretically reached
        return sendQueryToHost(query, mc, this);        
	}
    

    /**
     * Sends a query to the specified host.
     *
     * @param query the <tt>QueryRequest</tt> to send
     * @param mc the <tt>ManagedConnection</tt> to send the query to
     * @param handler the <tt>QueryHandler</tt> 
     * @return the number of new hosts theoretically hit by this query
     */
    static int sendQueryToHost(QueryRequest query, 
                               ManagedConnection mc, 
                               QueryHandler handler) {
        
        // send the query directly along the connection, but if the query didn't
        // go through send back 0....
        if (!_messageRouter.originateQuery(query, mc)) return 0;
        
        byte ttl = query.getTTL();

        // add the reply handler to the list of queried hosts if it's not
        // a TTL=1 query or the connection does not support probe queries

        // adds the connection to the list of probe connections if it's
        // a TTL=1 query to a connection that supports probe extensions,
        // otherwise add it to the list of connections we've queried
        if(ttl == 1 && mc.supportsProbeQueries()) {
            handler.QUERIED_PROBE_CONNECTIONS.add(mc);
        } else {
            handler.QUERIED_CONNECTIONS.add(mc);
            if (LOG.isTraceEnabled())
                LOG.trace("QUERIED_CONNECTIONS.size() = " +
                          handler.QUERIED_CONNECTIONS.size());
        }

        if (LOG.isTraceEnabled())
            LOG.trace("Querying host " + mc.getAddress() + " with ttl " +
                      query.getTTL());
        
        handler._nextQueryTime = System.currentTimeMillis() + 
            (ttl * handler._timeToWaitPerHop);

        return calculateNewHosts(mc, ttl);
    }

	/**
	 * Calculates the new TTL to use based on the number of hosts per connection
	 * we still need to query.
	 * 
	 * @param hostsToQueryPerConnection the number of hosts we should reach on
	 *  each remaining connections, to the best of our knowledge
     * @param degree the out-degree of the next connection
     * @param maxTTL the maximum TTL the connection will allow
     * @return the TTL to use for the next connection
	 */
	private static byte 
        calculateNewTTL(int hostsToQueryPerConnection, int degree, 
                        byte maxTTL) {

        if (maxTTL > MAX_QUERY_TTL) maxTTL = MAX_QUERY_TTL;

        // not the most efficient algorithm -- should use Math.log, but
        // that's ok
        for(byte i=1; i<MAX_QUERY_TTL; i++) {

            // biased towards lower TTLs since the horizon expands so
            // quickly
            int hosts = (int)(16.0*calculateNewHosts(degree, i));            
            if(hosts >= hostsToQueryPerConnection) {
                if(i > maxTTL) return maxTTL;
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

        // ----------------
        // NOTE: as agreed, _numResultsReportedByLeaf is the number of results
        // the leaf has received/consumed by a filter DIVIDED by 4 (4 being the
        // number of UPs connection it maintains).  That is why we don't divide
        // it here or anything.  We aren't sure if this mixes well with
        // BearShare's use but oh well....
        // ----------------
        // if leaf guidance is in effect, we have different criteria.
        if (_numResultsReportedByLeaf > 0) {
            // we shouldn't route too much regardless of what the leaf says
            if (RESULT_COUNTER.getNumResults() >= MAXIMUM_ROUTED_FOR_LEAVES)
                return true;
            // if the leaf is happy, so are we....
            if (_numResultsReportedByLeaf > RESULTS)
                return true;
        }
        // leaf guidance is not in effect or we are doing our own query
        else if (RESULT_COUNTER.getNumResults() >= RESULTS)
            return true;

        // if our theoretical horizon has gotten too high, consider
        // it enough results
        // precisely what this number should be is somewhat hard to determine
        // because, while connection have a specfic degree, the degree of 
        // the connections on subsequent hops cannot be determined
		if(_theoreticalHostsQueried > 110000) {
            return true;
        }

		// return true if we've been querying for longer than the specified 
		// maximum
		int queryLength = (int)(System.currentTimeMillis() - _queryStartTime);
		if(queryLength > MAX_QUERY_TIME) {
            return true;
        }

		return false;
	}

    /**
     * Use this to modify the number of results as reported by the leaf you are
     * querying for.
     */
    public void updateLeafResults(int numResults) {
        if (numResults > _numResultsReportedByLeaf) {
            // record up to the first 20 updates
            if (times.size() < 20) {
                times.add(System.currentTimeMillis() - _queryStartTime);
                results.add(numResults);
            }
            _numResultsReportedByLeaf = numResults;
        }
    }

    /**
     * Returns the number of results as reported by the leaf.  At least 0.
     */
    public int getNumResultsReportedByLeaf() {
        return _numResultsReportedByLeaf;
    }

    /**
     * Accessor for the <tt>ReplyHandler</tt> instance for the connection
     * issuing this request.
     *
     * @return the <tt>ReplyHandler</tt> for the connection issuing this 
     *  request
     */
    public ReplyHandler getReplyHandler() {
        return REPLY_HANDLER;
    }
    
    /**
     * Accessor for the time to wait per hop, in milliseconds,
     * for this QueryHandler.
     *
     * @return the time to wait per hop in milliseconds for this
     *  QueryHandler
     */
    public long getTimeToWaitPerHop() {
        return _timeToWaitPerHop;
    }

	// overrides Object.toString
	public String toString() {
		return "QueryHandler: QUERY: "+QUERY;
	}

    /** @return simply returns the guid of the query this is handling.
     */
    public GUID getGUID() {
        return new GUID(QUERY.getGUID());
    }
    
    public Object inspect() {
        Map<String, Object> ret = new HashMap<String,Object>();
        ret.put("ver",1);
        ret.put("times", times);
        ret.put("res", results);
        ret.put("twh", _timeToWaitPerHop);
        ret.put("tdh", _timeToDecreasePerHop);
        ret.put("dec", _numDecrements);
        ret.put("nqt", _nextQueryTime);
        ret.put("qst", _queryStartTime);
        ret.put("ct", _curTime);
        ret.put("pqs", _probeQuerySent);
        ret.put("ftw", _forwardedToLeaves);
        return ret;
    }

}



