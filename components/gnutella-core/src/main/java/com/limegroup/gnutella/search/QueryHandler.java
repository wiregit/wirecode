
package com.limegroup.gnutella.search;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.routing.*;
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
     * TTL of the probe query.
     */
    private static byte PROBE_TTL = (byte)2;

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
	 * Constant for the <tt>ResultCounter</tt> for this query -- used
	 * to access the number of replies returned.
	 */
	private final ResultCounter RESULT_COUNTER;

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
     * Boolean for whether or not the probe query has been sent to the
     * desired number of hosts.
     */
    private boolean _probeCompleted = false;


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
		boolean isHashQuery = !query.getQueryUrns().isEmpty();
		QUERY = query;
		if(isHashQuery) {
			RESULTS = HASH_QUERY_RESULTS;
		} else {
			RESULTS = results;
		}

		REPLY_HANDLER = handler;
        RESULT_COUNTER = counter;
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
	 * Sends the query to the current connections.  If the query is not
	 * yet ready to be processed, this returns immediately.
	 *
	 * @throws <tt>NullPointerException</tt> if the route table entry
	 *  is <tt>null</tt>
	 */
	public void sendQuery() {
		if(hasEnoughResults()) return;

		_curTime = System.currentTimeMillis();
		if(_curTime < _nextQueryTime) return;

		if(_queryStartTime == 0) {
			_queryStartTime = _curTime;
        }
            
        if(!_probeCompleted) {
            _theoreticalHostsQueried += 
                sendProbeQuery(this, 
                               _connectionManager.getInitializedConnections2()); 
            if(_probeCompleted) {
                _nextQueryTime = System.currentTimeMillis() + 7000;
            } else {
                // allow time for connections to become established
                _nextQueryTime = System.currentTimeMillis() + 2000;
            }
            return;
		}

        
        _theoreticalHostsQueried += 
            sendQuery(this, _connectionManager.getInitializedConnections2()); 
        _nextQueryTime = System.currentTimeMillis() + 2000;
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
			
			int results = handler.RESULT_COUNTER.getNumResults();
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
            sendQueryToHost(query, mc, handler);

			//RouterService.getMessageRouter().sendQueryRequest(query, mc, 
            //                                                 handler.REPLY_HANDLER);

			// add the reply handler to the list of queried hosts
			//handler.QUERIED_HANDLERS.add(mc);
            newHosts = calculateNewHosts(mc, ttl);
            break;
		}
        return newHosts;
	}
    
    /**
     * Helper class the holds a <tt>ManagedConnection</tt> and a TTL for sending
     * a query to that handler.
     */
    private static class ConnectionTTLPair {
        
        /**
         * Constant for the <tt>ManagedConnection</tt>.
         */
        private final ManagedConnection MC;

        /**
         * Constant for the TTL.
         */
        private final byte TTL;

        /**
         * Creates a new <tt>ConnectionTTLPair</tt> with the specified 
         * <tt>Connection</tt> and TTL.
         *
         * @param mc the <tt>RequestHandler</tt> that the request should be sent
         *  to
         * @param ttl the time to live for the query to send
         */
        ConnectionTTLPair(ManagedConnection mc, byte ttl) {
            MC = mc;
            TTL = ttl;
        }
    }

    /**
     * Helper method that creates the list of nodes to query for the probe.
     * This list will vary in size depending on how popular the content appears
     * to be.
     */
    private static List[] createProbeLists(List connections, QueryRequest query) {
        Iterator iter = connections.iterator();
        
        LinkedList goodConnections = new LinkedList();
        LinkedList badConnections  = new LinkedList();
        LinkedList hitConnections  = new LinkedList();
        while(iter.hasNext()) {
            ManagedConnection mc = (ManagedConnection)iter.next();
            
            if(mc.isGoodUltrapeer() &&
               mc.getQueryRouteState() != null) {
                goodConnections.add(mc);
            } else {
                badConnections.add(mc);
            }
            
            ManagedConnectionQueryInfo qi = mc.getQueryRouteState();

            if(qi.lastReceived.contains(query)) { 
                hitConnections.add(mc);
            }
        }

        // final list of connections to query
        List[] returnLists = new List[2];
        List ttl1List = new LinkedList();
        List ttl2List = new LinkedList();
        returnLists[0] = ttl1List;
        returnLists[1] = ttl2List;        

        // do we have adequate data to determine some measure of the file's popularity?
        boolean adequateData = goodConnections.size() > 8;

        // if we don't have enough data from QRP tables, just send out a traditional probe
        // also, if we don't have an adequate number of QRP tables to access the 
        // popularity of the file, just send out an old-style probe at TTL=2
        if(hitConnections.size() == 0 || !adequateData) {
            return createAggressiveProbe(badConnections, goodConnections, 
                                         hitConnections, returnLists);
        } 


        double popularity = 
            (double)((double)hitConnections.size()/(double)goodConnections.size());

        // if there were no matches, then it's almost definitely a fairly
        // rare file, so send out a more aggressive probe
        if(popularity == 0.0) {
            return createAggressiveProbe(badConnections, goodConnections, 
                                         hitConnections, returnLists);
            
        }
        
        // if the file appears to be very popular, send it to only one host
        if(popularity == 1.0) {
            ttl1List.add(hitConnections.getFirst());
            return returnLists;
        }
        
        // scale the number of hosts to send the query to based on the
        // file's apparent abundance
        int connectionsToUse = (int)((double)hitConnections.size()/popularity);
        
        if(popularity > 0.5) {
            
            // send the query to 4 connections at TTL=1
            ttl1List.addAll(hitConnections.subList(0, 5));
            return returnLists;
        }
                    
        
        return returnLists;        
    }

    /**
     * Helper method that creates lists of TTL=1 and TTL=2 connections to query
     * for an aggressive probe.  This is desired, for example, when the desired
     * file appears to be rare or when there is not enough data to determine
     * the file's popularity.
     *
     * @param badConnections the <tt>List</tt> of old-style connections
     * @param goodConnections the <tt>List</tt> of new connections
     * @param hitConnections the <tt>List</tt> of connections with hits
     * @param returnLists the array of TTL=1 and TTL=2 connections to query
     */
    private static List[] 
        createAggressiveProbe(List badConnections, List goodConnections,
                              List hitConnections, List[] returnLists) {

        if(badConnections.size() < 3) {
            returnLists[1].addAll(badConnections);            
            int listSize = returnLists[0].size() + returnLists[1].size();

            // if we still don't have enough connections, take some from
            // the good list
            if(listSize < 3) {
                int maxIndex = Math.min((3 - listSize), goodConnections.size());
                if(!goodConnections.isEmpty()) {
                    returnLists[1].add(goodConnections.subList(0, maxIndex));
                }
            }
        } else {
            returnLists[1].addAll(badConnections.subList(0, 4));            
        }

        // add any hits there are to the TTL=1 list
        int maxIndex = Math.min(4, hitConnections.size());
        returnLists[0].addAll(hitConnections.subList(0, maxIndex));

        return returnLists;        
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
        // send a TTL=1 query to hosts that have matches in their
        // QRP tables, running one at a time with pauses.
        
        QueryRequest query = createQuery(handler.QUERY, (byte)1);

        RouterService.getMessageRouter().forwardQueryRequestToLeaves(query, handler.REPLY_HANDLER);

        List[] probeLists = createProbeLists(list, query);
        List probeList = probeLists[0];
        Iterator iter = list.iterator();
        int connectionsUsed = 0;
        
        // Holder for the number of Ultrapeer connections we have
        // that use query routing.  This is used to decide how
        // good our sample "probe" has been
        int qrConnections = 0;
        int qrConnectionsUsed = 0;
        while(iter.hasNext()) {
            ManagedConnection mc = (ManagedConnection)iter.next();

            // this could be more fine-grained, since the connection
            // only really needs to support intra-Ultrapeer QRP and
            // to forward TTL=1 probe queries, but this is fine
            if(!mc.isGoodUltrapeer()) continue;

            ManagedConnectionQueryInfo qi = mc.getQueryRouteState();
            if (qi.lastReceived == null) continue;

            // don't consider this as a host used in our sample
            // until we know it has a QRP table
            qrConnections++;
            if(qi.lastReceived.contains(query)) {
                sendQueryToHost(query, mc, handler);
                qrConnectionsUsed++;
            }
        }
		
        query = createQuery(handler.QUERY, PROBE_TTL);
        int newHosts = 0;

        int hostsQueried = 0;
        int i = 0;
        while(hostsQueried<3 && i<list.size()) {
			ManagedConnection mc = (ManagedConnection)list.get(i);

            // if we've already queried this host, go to the next one,
            // or if it's not yet stable
            if(!mc.isStable() || handler.QUERIED_HANDLERS.contains(mc)) {
                // count the index
                i++;
                continue;
            }

            sendQueryToHost(query, mc, handler);

			newHosts += calculateNewHosts(mc, PROBE_TTL);

            hostsQueried++;
            i++;
		}
        if(hostsQueried >= 3) {
            handler._probeCompleted = true;
        }
        return newHosts;
	}

    /**
     * Sends a query to the specified host.
     *
     * @param query the <tt>QueryRequest</tt> to send
     * @param mc the <tt>ManagedConnection</tt> to send the query to
     * @param handler the <tt>QueryHandler</tt> 
     */
    private static void sendQueryToHost(QueryRequest query, ManagedConnection mc, 
                                        QueryHandler handler) {
        RouterService.getMessageRouter().sendQueryRequest(query, mc, 
                                                          handler.REPLY_HANDLER);
        
        // add the reply handler to the list of queried hosts
        handler.QUERIED_HANDLERS.add(mc);
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

		if(RESULT_COUNTER.getNumResults() >= RESULTS) return true;
	 
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

	// overrides Object.toString
	public String toString() {
		return "QueryHandler: QUERY: "+QUERY;
	}
}
