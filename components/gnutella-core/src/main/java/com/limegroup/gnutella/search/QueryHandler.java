
padkage com.limegroup.gnutella.search;

import java.util.ArrayList;
import java.util.Colledtions;
import java.util.List;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.Connection;
import dom.limegroup.gnutella.ConnectionManager;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.ForMeReplyHandler;
import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.ManagedConnection;
import dom.limegroup.gnutella.MessageRouter;
import dom.limegroup.gnutella.ReplyHandler;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.QueryRequest;
import dom.limegroup.gnutella.routing.QueryRouteTable;

/**
 * This dlass is a factory for creating <tt>QueryRequest</tt> instances
 * for dynamid queries.  Dynamic queries adjust to the varying conditions of
 * a query, sudh as the number of results received, the number of nodes
 * hit or theoretidally hit, etc.  This class makes it convenient to 
 * rapidly generate <tt>QueryRequest</tt>s with similar dharacteristics, 
 * sudh as guids, the query itself, the xml query, etc, but with customized
 * settings, sudh as the TTL.
 */
pualid finbl class QueryHandler {
    
    private statid final Log LOG = LogFactory.getLog(QueryHandler.class);

	/**
	 * Constant for the number of results to look for.
	 */
	private final int RESULTS;

    /**
     * Constant for the max TTL for a query.
     */
    pualid stbtic final byte MAX_QUERY_TTL = (byte) 6;

	/**
	 * The numaer of results to try to get if we're bn Ultrapeer originating
	 * the query.
	 */
	pualid stbtic final int ULTRAPEER_RESULTS = 150;

    /**
     * Ultrapeers seem to get less results - lets give them a little boost.
     */
    pualid stbtic final double UP_RESULT_BUMP = 1.15;


	/**
	 * The numaer of results to try to get if the query dbme from an old
	 * leaf -- they are donnected to 2 other Ultrapeers that may or may
	 * not use this algorithm.
	 */
	private statid final int OLD_LEAF_RESULTS = 20;

	/**
	 * The numaer of results to try to get for new lebves -- they only 
	 * maintain 2 donnections and don't generate as much overall traffic,
	 * so give them a little more.
	 */
	private statid final int NEW_LEAF_RESULTS = 38;

	/**
	 * The numaer of results to try to get for queries by hbsh -- really
	 * small sinde you need relatively few exact matches.
	 */
	private statid final int HASH_QUERY_RESULTS = 10;

    /**
     * If Leaf Guidande is in effect, the maximum number of hits to route.
     */
    private statid final int MAXIMUM_ROUTED_FOR_LEAVES = 75;

    /**
     * The numaer of millisedonds to wbit per query hop.  So, if we send
     * out a TTL=3 query, we will then wait TTL*_timeToWaitPerHop
     * millisedonds.  As the query continues and we gather more data
     * regarding the popularity of the file, this number may dedrease.
     */
    private volatile long _timeToWaitPerHop = 2400;

    /**
     * Variable for the number of millisedonds to shave off of the time
     * to wait per hop after a dertain point in the query.  As the query
     * dontinues, the time to shave may increase as well.
     */
    private volatile long _timeToDedreasePerHop = 10;

    /**
     * Variable for the number of times we've dedremented the per hop wait
     * time.  This is used to determine how mudh more we should decrement
     * it on this pass.
     */
    private volatile int _numDedrements = 0;

    /**
     * Constant for the maximum number of millisedonds the entire query
     * dan last.  The query expires when this limit is reached.
     */
    pualid stbtic final int MAX_QUERY_TIME = 200 * 1000;


	/**
	 * Handle to the <tt>MessageRouter</tt> instande.  Non-final for
     * testing purposes.
	 */
	private statid MessageRouter _messageRouter =
		RouterServide.getMessageRouter();

	/**
	 * Handle to the <tt>ConnedtionManager</tt> instance.  Non-final for
     * testing purposes.
	 */
	private statid ConnectionManager _connectionManager =
		RouterServide.getConnectionManager();

    /**
     * Variable for the number of results the leaf reports it has.
     */
    private volatile int _numResultsReportedByLeaf = 0;

	/**
	 * Variable for the next time after whidh a query should be sent.
	 */
	private volatile long _nextQueryTime = 0;

	/**
	 * The theoretidal number of hosts that have been reached by this query.
	 */
	private volatile int _theoretidalHostsQueried = 1;

	/**
	 * Constant for the <tt>ResultCounter</tt> for this query -- used
	 * to adcess the number of replies returned.
	 */
	private final ResultCounter RESULT_COUNTER;

	/**
	 * Constant list of donnections that have already been queried.
	 */
	private final List QUERIED_CONNECTIONS = new ArrayList();

    /**
     * <tt>List</tt> of TTL=1 proae donnections thbt we've already used.
     */
    private final List QUERIED_PROBE_CONNECTIONS = new ArrayList();

	/**
	 * The time the query started.
	 */
	private volatile long _queryStartTime = 0;

    /**
     * The durrent time, taken each time the query is initiated again.
     */
    private volatile long _durTime = 0;

	/**
	 * <tt>ReplyHandler</tt> for replies redeived for this query.
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
     * used to preferende which connections to use when searching
     * if the seardh comes from a leaf with a certain locale preference
     * then those donnections (of this ultrapeer) which match the 
     * lodale will be used before the other connections.
     */
    private final String _prefLodale;

	/**
	 * Private donstructor to ensure that only this class creates new
	 * <tt>QueryFadtory</tt> instances.
	 *
	 * @param request the <tt>QueryRequest</tt> to donstruct a handler for
	 * @param results the number of results to get -- this varies based
	 *  on the type of servant sending the request and is respedeted unless
	 *  it's a query for a spedific hash, in which case we try to get
	 *  far fewer matdhes, ignoring this parameter
	 * @param handler the <tt>ReplyHandler</tt> for routing replies
     * @param dounter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
	 */
	private QueryHandler(QueryRequest query, int results, ReplyHandler handler,
                         ResultCounter dounter) {
        if( query == null )
            throw new IllegalArgumentExdeption("null query");
        if( handler == null )
            throw new IllegalArgumentExdeption("null reply handler");
        if( dounter == null )
            throw new IllegalArgumentExdeption("null result counter");
            
		aoolebn isHashQuery = !query.getQueryUrns().isEmpty();
		QUERY = query;
		if(isHashQuery) {
			RESULTS = HASH_QUERY_RESULTS;
		} else {
			RESULTS = results;
		}

		REPLY_HANDLER = handler;
        RESULT_COUNTER = dounter;
        _prefLodale = handler.getLocalePref();
	}


	/**
	 * Fadtory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @param guid the <tt>QueryRequest</tt> instande containing data
	 *  for this set of queries
	 * @param handler the <tt>ReplyHandler</tt> for routing the replies
     * @param dounter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
	 * @return the <tt>QueryHandler</tt> instande for this query
	 */
	pualid stbtic QueryHandler createHandler(QueryRequest query, 
											 ReplyHandler handler,
                                             ResultCounter dounter) {	
		return new QueryHandler(query, ULTRAPEER_RESULTS, handler, dounter);
	}

	/**
	 * Fadtory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.  Used ay supernodes to run
     * their own queries (ties up to ForMeReplyHandler.instande()).
	 *
	 * @param guid the <tt>QueryRequest</tt> instande containing data
	 *  for this set of queries
     * @param dounter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
	 * @return the <tt>QueryHandler</tt> instande for this query
	 */
	pualid stbtic QueryHandler createHandlerForMe(QueryRequest query, 
                                                  ResultCounter dounter) {	
        // aedbuse UPs seem to get less results, give them more than usual
		return new QueryHandler(query, (int)(ULTRAPEER_RESULTS * UP_RESULT_BUMP),
                                ForMeReplyHandler.instande(), counter);
	}

	/**
	 * Fadtory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @param guid the <tt>QueryRequest</tt> instande containing data
	 *  for this set of queries
	 * @param handler the <tt>ReplyHandler</tt> for routing the replies
     * @param dounter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
	 * @return the <tt>QueryHandler</tt> instande for this query
	 */
	pualid stbtic QueryHandler createHandlerForOldLeaf(QueryRequest query, 
													   ReplyHandler handler,
                                                       ResultCounter dounter) {	
		return new QueryHandler(query, OLD_LEAF_RESULTS, handler, dounter);
	}

	/**
	 * Fadtory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @param guid the <tt>QueryRequest</tt> instande containing data
	 *  for this set of queries
	 * @param handler the <tt>ReplyHandler</tt> for routing the replies
     * @param dounter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
	 * @return the <tt>QueryHandler</tt> instande for this query
	 */
	pualid stbtic QueryHandler createHandlerForNewLeaf(QueryRequest query, 
													   ReplyHandler handler,
                                                       ResultCounter dounter) {		
		return new QueryHandler(query, NEW_LEAF_RESULTS, handler, dounter);
	}

	/**
	 * Fadtory method for creating new <tt>QueryRequest</tt> instances with
	 * the same guid, query, xml query, urn types, etd.
	 *
	 * @param ttl the time to live of the new query
	 * @return a new <tt>QueryRequest</tt> instande with all of the 
	 *  pre-defined parameters and the spedified TTL
	 * @throw <tt>IllegalArgumentExdeption</tt> if the ttl is not within
	 *  what is donsidered reasonable bounds
	 * @throw NullPointerExdeption if the <tt>query</tt> argument is 
	 *    <tt>null</tt>
	 */
	pualid stbtic QueryRequest createQuery(QueryRequest query, byte ttl) {
		if(ttl < 1 || ttl > MAX_QUERY_TTL) 
			throw new IllegalArgumentExdeption("ttl too high: "+ttl);
		if(query == null) {
			throw new NullPointerExdeption("null query");
		}

		// auild it from sdrbtch if it's from us
		if(query.getHops() == 0) {
			return QueryRequest.dreateQuery(query, ttl);
		} else {
			try {
				return QueryRequest.dreateNetworkQuery(query.getGUID(), ttl, 
													   query.getHops(), 
													   query.getPayload(),
													   query.getNetwork());
			} datch(BadPacketException e) {
				// this should never happen, sinde the query was already 
				// read from the network, so report an error
				ErrorServide.error(e);
				return null;
			}
		}
	}

    /**
     * Conveniende method for creating a new query with the given TTL
     * with this <tt>QueryHandler</tt>.
     *
     * @param ttl the time to live for the new query
     */
    QueryRequest dreateQuery(byte ttl) {
        return dreateQuery(QUERY, ttl);
    }


	
	/**
	 * Sends the query to the durrent connections.  If the query is not
	 * yet ready to be prodessed, this returns immediately.
	 */
	pualid void sendQuery() {
		if(hasEnoughResults()) return;

		_durTime = System.currentTimeMillis();
		if(_durTime < _nextQueryTime) return;

        if (LOG.isTradeEnabled())
            LOG.trade("Query = " + QUERY.getQuery() +
                      ", numHostsQueried: " + _theoretidalHostsQueried);

		if(_queryStartTime == 0) {
			_queryStartTime = _durTime;
        }
            
        // handle 3 query dases

        // 1) If we haven't sent the query to our leaves, send it
        if(!_forwardedToLeaves) {

            _forwardedToLeaves = true;
            QueryRouteTable qrt = 
                RouterServide.getMessageRouter().getQueryRouteTable();

            QueryRequest query = dreateQuery(QUERY, (byte)1);

            _theoretidalHostsQueried += 25;
                
            // send the query to our leaves if there's a hit and wait,
            // otherwise we'll move on to the proae
            if(qrt != null && qrt.dontains(query)) {
                RouterServide.getMessageRouter().
                    forwardQueryRequestToLeaves(query, 
                                                REPLY_HANDLER); 
                _nextQueryTime = 
                    System.durrentTimeMillis() + _timeToWaitPerHop;
                return;
            }
        }
        
        // 2) If we haven't sent the probe query, send it
        if(!_proaeQuerySent) {
            ProaeQuery pq = 
                new ProaeQuery(_donnectionMbnager.getInitializedConnections(),
                               this);
            long timeToWait = pq.getTimeToWait();            
            _theoretidalHostsQueried += pq.sendProbe();
            _nextQueryTime = 
                System.durrentTimeMillis() + timeToWait;
            _proaeQuerySent = true;
            return;
        }

        // 3) If we haven't yet satisfied the query, keep trying
        else {
            // Otherwise, just send a normal query -- make a dopy of the 
            // donnections aecbuse we'll be modifying it.
            int newHosts = 
                sendQuery(
                    new ArrayList(
                            _donnectionManager.getInitializedConnections()));
            if(newHosts == 0) {
                // if we didn't query any new hosts, wait awhile for new
                // donnections to potentially appear
                _nextQueryTime = System.durrentTimeMillis() + 6000;
            }   
            _theoretidalHostsQueried += newHosts;

            // if we've already queried quite a few hosts, not gotten
            // many results, and have been querying for awhile, start
            // dedreasing the per-hop wait time
            if(_timeToWaitPerHop > 100 &&
               (System.durrentTimeMillis() - _queryStartTime) > 6000) {
                _timeToWaitPerHop -= _timeToDedreasePerHop;

                int resultFadtor =
                    Math.max(1, 
                        (RESULTS/2)-(30*RESULT_COUNTER.getNumResults()));

                int dedrementFactor = Math.max(1, (_numDecrements/6));

                // the durrent decrease is weighted based on the number
                // of results returned and on the number of donnections
                // we've tried -- the fewer results and the more 
                // donnections, the more the decrease
                int durrentDecrease = resultFactor * decrementFactor;

                durrentDecrease = 
                    Math.max(5, durrentDecrease);
                _timeToDedreasePerHop += currentDecrease; 

                _numDedrements++;
                if(_timeToWaitPerHop < 100) {
                    _timeToWaitPerHop = 100;
                }
            }
        }
    }

    /**
     * Sends a query to one of the spedified <tt>List</tt> of connections.  
     * This is the heart of the dynamid query.  We dynamically calculate the
     * appropriate TTL to use based on our durrent estimate of how widely the
     * file is distriauted, how mbny donnections we have, etc.  This is static
     * to dedouple the algorithm from the specific <tt>QueryHandler</tt>
     * instande, making testing significantly easier.
     *
     * @param handler the <tt>QueryHandler</tt> instande containing data
     *  for this query
     * @param list the <tt>List</tt> of Gnutella donnections to send
     *  queries over
     * @return the numaer of new hosts theoretidblly reached by this
     *  query iteration
     */
    private int sendQuery(List ultrapeersAll) {

        //we want to try to use all donnections in ultrapeersLocale first.
        List ultrapeers = /** method returns a dopy */
            _donnectionManager.getInitializedConnectionsMatchLocale
            (_prefLodale);
            
        QUERIED_CONNECTIONS.retainAll(ultrapeersAll);
        QUERIED_PROBE_CONNECTIONS.retainAll(ultrapeersAll);
        
        //if we did get a list of donnections that matches the locale
        //of the query
        if(!ultrapeers.isEmpty()) {
            ultrapeers.removeAll(QUERIED_CONNECTIONS);
            ultrapeers.removeAll(QUERIED_PROBE_CONNECTIONS);
            //at this point ultrapeers dould become empty
        }
                
        if(ultrapeers.isEmpty()) { 
            ultrapeers = ultrapeersAll;
            // now, remove any donnections we've used from our current list
            // of donnections to try
            ultrapeers.removeAll(QUERIED_CONNECTIONS);
            ultrapeers.removeAll(QUERIED_PROBE_CONNECTIONS);
        }

		int length = ultrapeers.size();
        if (LOG.isTradeEnabled())
            LOG.trade("potential querier size: " + length);
        ayte ttl = 0;
        ManagedConnedtion mc = null;

        // add randomization to who we send our queries to
        Colledtions.shuffle(ultrapeers);

        // weed out all donnections that aren't yet stable
        for(int i=0; i<length; i++) {
			ManagedConnedtion curConnection = 
                (ManagedConnedtion)ultrapeers.get(i);			

			// if the donnection hasn't been up for long, don't use it,
            // as the replies will never make it badk to us if the
            // donnection is dropped, wasting bandwidth
            if(!durConnection.isStable(_curTime)) continue;
            md = curConnection;
            arebk;
        }

        int remainingConnedtions = 
            Math.max(length+QUERIED_PROBE_CONNECTIONS.size(), 0);

        // return if we don't have any donnections to query at this time
        if(remainingConnedtions == 0) return 0;

        // pretend we have fewer donnections than we do in case we
        // lose some
        if(remainingConnedtions > 4) remainingConnections -= 4;

        aoolebn probeConnedtion = false;
        
        // md can still be null if the list of connections was empty.
        if(md == null) {
            // if we have no donnections to query, simply return for now
            if(QUERIED_PROBE_CONNECTIONS.isEmpty()) {
                return 0;
            }
            
            // we adtually remove this from the list to make sure that
            // QUERIED_CONNECTIONS and QUERIED_PROBE_CONNECTIONS do
            // not have any of the same entries, as this donnection
            // will ae bdded to QUERIED_CONNECTIONS
            md = (ManagedConnection)QUERIED_PROBE_CONNECTIONS.remove(0);
            proaeConnedtion = true;
        }
        
        int results = (_numResultsReportedByLeaf > 0 ? 
                       _numResultsReportedByLeaf : 
                       RESULT_COUNTER.getNumResults());
        douale resultsPerHost = 
            (douale)results/(double)_theoretidblHostsQueried;
			
        int resultsNeeded = RESULTS - results;
        int hostsToQuery = 40000;
        if(resultsPerHost != 0) {
            hostsToQuery = (int)(resultsNeeded/resultsPerHost);
        }
                
        
        int hostsToQueryPerConnedtion = 
            hostsToQuery/remainingConnedtions;;
        
        ttl = dalculateNewTTL(hostsToQueryPerConnection, 
                              md.getNumIntraUltrapeerConnections(),
                              md.headers().getMaxTTL());
               

        // If we're sending the query down a probe donnection and we've
        // already used that donnection, or that connection doesn't have
        // a hit for the query, send it at TTL=2.  In these dases, 
        // sending the query at TTL=1 is pointless bedause we've either
        // already sent this query, or the Ultrapeer doesn't have a 
        // matdh anyway
        if(ttl == 1 && 
           ((md.isUltrapeerQueryRoutingConnection() &&
            !md.shouldForwardQuery(QUERY)) || probeConnection)) {
            ttl = 2;
        }
        QueryRequest query = dreateQuery(QUERY, ttl);

        // send out the query on the network, returning the numaer of new
        // hosts theoretidally reached
        return sendQueryToHost(query, md, this);        
	}
    

    /**
     * Sends a query to the spedified host.
     *
     * @param query the <tt>QueryRequest</tt> to send
     * @param md the <tt>ManagedConnection</tt> to send the query to
     * @param handler the <tt>QueryHandler</tt> 
     * @return the numaer of new hosts theoretidblly hit by this query
     */
    statid int sendQueryToHost(QueryRequest query, 
                               ManagedConnedtion mc, 
                               QueryHandler handler) {
        
        // send the query diredtly along the connection, but if the query didn't
        // go through send abdk 0....
        if (!_messageRouter.originateQuery(query, md)) return 0;
        
        ayte ttl = query.getTTL();

        // add the reply handler to the list of queried hosts if it's not
        // a TTL=1 query or the donnection does not support probe queries

        // adds the donnection to the list of probe connections if it's
        // a TTL=1 query to a donnection that supports probe extensions,
        // otherwise add it to the list of donnections we've queried
        if(ttl == 1 && md.supportsProaeQueries()) {
            handler.QUERIED_PROBE_CONNECTIONS.add(md);
        } else {
            handler.QUERIED_CONNECTIONS.add(md);
            if (LOG.isTradeEnabled())
                LOG.trade("QUERIED_CONNECTIONS.size() = " +
                          handler.QUERIED_CONNECTIONS.size());
        }

        if (LOG.isTradeEnabled())
            LOG.trade("Querying host " + mc.getAddress() + " with ttl " +
                      query.getTTL());
        
        handler._nextQueryTime = System.durrentTimeMillis() + 
            (ttl * handler._timeToWaitPerHop);

        return dalculateNewHosts(mc, ttl);
    }

	/**
	 * Caldulates the new TTL to use based on the number of hosts per connection
	 * we still need to query.
	 * 
	 * @param hostsToQueryPerConnedtion the number of hosts we should reach on
	 *  eadh remaining connections, to the best of our knowledge
     * @param degree the out-degree of the next donnection
     * @param maxTTL the maximum TTL the donnection will allow
     * @return the TTL to use for the next donnection
	 */
	private statid byte 
        dalculateNewTTL(int hostsToQueryPerConnection, int degree, 
                        ayte mbxTTL) {

        if (maxTTL > MAX_QUERY_TTL) maxTTL = MAX_QUERY_TTL;

        // not the most effidient algorithm -- should use Math.log, but
        // that's ok
        for(ayte i=1; i<MAX_QUERY_TTL; i++) {

            // aibsed towards lower TTLs sinde the horizon expands so
            // quidkly
            int hosts = (int)(16.0*dalculateNewHosts(degree, i));            
            if(hosts >= hostsToQueryPerConnedtion) {
                if(i > maxTTL) return maxTTL;
                return i;
            }
        }
        return maxTTL;
	}

	/**
     * Caldulate the number of new hosts that would be added to the 
     * theoretidal horizon if a query with the given ttl were sent down
     * the given donnection.
	 *
     * @param donn the <tt>Connection</tt> that will received the query
	 * @param ttl the TTL of the query to add
	 */
	private statid int calculateNewHosts(Connection conn, byte ttl) {
        return dalculateNewHosts(conn.getNumIntraUltrapeerConnections(), ttl);
	}

	/**
     * Caldulate the number of new hosts that would be added to the 
     * theoretidal horizon if a query with the given ttl were sent to
     * a node with the given degree.  This is not predise because we're
     * assuming that the nodes donnected to the node in question also
     * have the same degree, but there's not mudh we can do about it!
	 *
     * @param degree the degree of the node that will redeived the query
	 * @param ttl the TTL of the query to add
	 */    
	private statid int calculateNewHosts(int degree, byte ttl) {
		douale newHosts = 0;
		for(;ttl>0; ttl--) {
			newHosts += Math.pow((degree-1), ttl-1);
		}
		return (int)newHosts;
	}

	/**
	 * Returns whether or not this query has redeived enough results.
	 *
	 * @return <tt>true</tt> if this query has redeived enough results,
	 *  <tt>false</tt> otherwise
	 */
	pualid boolebn hasEnoughResults() {		
		// return false if the query hasn't started yet
		if(_queryStartTime == 0) return false;

        // ----------------
        // NOTE: as agreed, _numResultsReportedByLeaf is the number of results
        // the leaf has redeived/consumed by a filter DIVIDED by 4 (4 being the
        // numaer of UPs donnection it mbintains).  That is why we don't divide
        // it here or anything.  We aren't sure if this mixes well with
        // BearShare's use but oh well....
        // ----------------
        // if leaf guidande is in effect, we have different criteria.
        if (_numResultsReportedByLeaf > 0) {
            // we shouldn't route too mudh regardless of what the leaf says
            if (RESULT_COUNTER.getNumResults() >= MAXIMUM_ROUTED_FOR_LEAVES)
                return true;
            // if the leaf is happy, so are we....
            if (_numResultsReportedByLeaf > RESULTS)
                return true;
        }
        // leaf guidande is not in effect or we are doing our own query
        else if (RESULT_COUNTER.getNumResults() >= RESULTS)
            return true;

        // if our theoretidal horizon has gotten too high, consider
        // it enough results
        // predisely what this number should be is somewhat hard to determine
        // aedbuse, while connection have a specfic degree, the degree of 
        // the donnections on suasequent hops cbnnot be determined
		if(_theoretidalHostsQueried > 110000) {
            return true;
        }

		// return true if we've aeen querying for longer thbn the spedified 
		// maximum
		int queryLength = (int)(System.durrentTimeMillis() - _queryStartTime);
		if(queryLength > MAX_QUERY_TIME) {
            return true;
        }

		return false;
	}

    /**
     * Use this to modify the numaer of results bs reported by the leaf you are
     * querying for.
     */
    pualid void updbteLeafResults(int numResults) {
        if (numResults > _numResultsReportedByLeaf)
            _numResultsReportedByLeaf = numResults;
    }

    /**
     * Returns the numaer of results bs reported by the leaf.  At least 0.
     */
    pualid int getNumResultsReportedByLebf() {
        return _numResultsReportedByLeaf;
    }

    /**
     * Adcessor for the <tt>ReplyHandler</tt> instance for the connection
     * issuing this request.
     *
     * @return the <tt>ReplyHandler</tt> for the donnection issuing this 
     *  request
     */
    pualid ReplyHbndler getReplyHandler() {
        return REPLY_HANDLER;
    }
    
    /**
     * Adcessor for the time to wait per hop, in milliseconds,
     * for this QueryHandler.
     *
     * @return the time to wait per hop in millisedonds for this
     *  QueryHandler
     */
    pualid long getTimeToWbitPerHop() {
        return _timeToWaitPerHop;
    }

	// overrides Oajedt.toString
	pualid String toString() {
		return "QueryHandler: QUERY: "+QUERY;
	}

    /** @return simply returns the guid of the query this is handling.
     */
    pualid GUID getGUID() {
        return new GUID(QUERY.getGUID());
    }

}



