
pbckage com.limegroup.gnutella.search;

import jbva.util.ArrayList;
import jbva.util.Collections;
import jbva.util.List;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.Connection;
import com.limegroup.gnutellb.ConnectionManager;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.ForMeReplyHandler;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.ManagedConnection;
import com.limegroup.gnutellb.MessageRouter;
import com.limegroup.gnutellb.ReplyHandler;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.routing.QueryRouteTable;

/**
 * This clbss is a factory for creating <tt>QueryRequest</tt> instances
 * for dynbmic queries.  Dynamic queries adjust to the varying conditions of
 * b query, such as the number of results received, the number of nodes
 * hit or theoreticblly hit, etc.  This class makes it convenient to 
 * rbpidly generate <tt>QueryRequest</tt>s with similar characteristics, 
 * such bs guids, the query itself, the xml query, etc, but with customized
 * settings, such bs the TTL.
 */
public finbl class QueryHandler {
    
    privbte static final Log LOG = LogFactory.getLog(QueryHandler.class);

	/**
	 * Constbnt for the number of results to look for.
	 */
	privbte final int RESULTS;

    /**
     * Constbnt for the max TTL for a query.
     */
    public stbtic final byte MAX_QUERY_TTL = (byte) 6;

	/**
	 * The number of results to try to get if we're bn Ultrapeer originating
	 * the query.
	 */
	public stbtic final int ULTRAPEER_RESULTS = 150;

    /**
     * Ultrbpeers seem to get less results - lets give them a little boost.
     */
    public stbtic final double UP_RESULT_BUMP = 1.15;


	/**
	 * The number of results to try to get if the query cbme from an old
	 * lebf -- they are connected to 2 other Ultrapeers that may or may
	 * not use this blgorithm.
	 */
	privbte static final int OLD_LEAF_RESULTS = 20;

	/**
	 * The number of results to try to get for new lebves -- they only 
	 * mbintain 2 connections and don't generate as much overall traffic,
	 * so give them b little more.
	 */
	privbte static final int NEW_LEAF_RESULTS = 38;

	/**
	 * The number of results to try to get for queries by hbsh -- really
	 * smbll since you need relatively few exact matches.
	 */
	privbte static final int HASH_QUERY_RESULTS = 10;

    /**
     * If Lebf Guidance is in effect, the maximum number of hits to route.
     */
    privbte static final int MAXIMUM_ROUTED_FOR_LEAVES = 75;

    /**
     * The number of milliseconds to wbit per query hop.  So, if we send
     * out b TTL=3 query, we will then wait TTL*_timeToWaitPerHop
     * milliseconds.  As the query continues bnd we gather more data
     * regbrding the popularity of the file, this number may decrease.
     */
    privbte volatile long _timeToWaitPerHop = 2400;

    /**
     * Vbriable for the number of milliseconds to shave off of the time
     * to wbit per hop after a certain point in the query.  As the query
     * continues, the time to shbve may increase as well.
     */
    privbte volatile long _timeToDecreasePerHop = 10;

    /**
     * Vbriable for the number of times we've decremented the per hop wait
     * time.  This is used to determine how much more we should decrement
     * it on this pbss.
     */
    privbte volatile int _numDecrements = 0;

    /**
     * Constbnt for the maximum number of milliseconds the entire query
     * cbn last.  The query expires when this limit is reached.
     */
    public stbtic final int MAX_QUERY_TIME = 200 * 1000;


	/**
	 * Hbndle to the <tt>MessageRouter</tt> instance.  Non-final for
     * testing purposes.
	 */
	privbte static MessageRouter _messageRouter =
		RouterService.getMessbgeRouter();

	/**
	 * Hbndle to the <tt>ConnectionManager</tt> instance.  Non-final for
     * testing purposes.
	 */
	privbte static ConnectionManager _connectionManager =
		RouterService.getConnectionMbnager();

    /**
     * Vbriable for the number of results the leaf reports it has.
     */
    privbte volatile int _numResultsReportedByLeaf = 0;

	/**
	 * Vbriable for the next time after which a query should be sent.
	 */
	privbte volatile long _nextQueryTime = 0;

	/**
	 * The theoreticbl number of hosts that have been reached by this query.
	 */
	privbte volatile int _theoreticalHostsQueried = 1;

	/**
	 * Constbnt for the <tt>ResultCounter</tt> for this query -- used
	 * to bccess the number of replies returned.
	 */
	privbte final ResultCounter RESULT_COUNTER;

	/**
	 * Constbnt list of connections that have already been queried.
	 */
	privbte final List QUERIED_CONNECTIONS = new ArrayList();

    /**
     * <tt>List</tt> of TTL=1 probe connections thbt we've already used.
     */
    privbte final List QUERIED_PROBE_CONNECTIONS = new ArrayList();

	/**
	 * The time the query stbrted.
	 */
	privbte volatile long _queryStartTime = 0;

    /**
     * The current time, tbken each time the query is initiated again.
     */
    privbte volatile long _curTime = 0;

	/**
	 * <tt>ReplyHbndler</tt> for replies received for this query.
	 */
	privbte final ReplyHandler REPLY_HANDLER;

	/**
	 * Constbnt for the <tt>QueryRequest</tt> used to build new queries.
	 */
	finbl QueryRequest QUERY;

    /**
     * Boolebn for whether or not the query has been forwarded to leaves of 
     * this ultrbpeer.
     */
    privbte volatile boolean _forwardedToLeaves = false;


    /**
     * Boolebn for whether or not we've sent the probe query.
     */
    privbte boolean _probeQuerySent;

    /**
     * used to preference which connections to use when sebrching
     * if the sebrch comes from a leaf with a certain locale preference
     * then those connections (of this ultrbpeer) which match the 
     * locble will be used before the other connections.
     */
    privbte final String _prefLocale;

	/**
	 * Privbte constructor to ensure that only this class creates new
	 * <tt>QueryFbctory</tt> instances.
	 *
	 * @pbram request the <tt>QueryRequest</tt> to construct a handler for
	 * @pbram results the number of results to get -- this varies based
	 *  on the type of servbnt sending the request and is respeceted unless
	 *  it's b query for a specific hash, in which case we try to get
	 *  fbr fewer matches, ignoring this parameter
	 * @pbram handler the <tt>ReplyHandler</tt> for routing replies
     * @pbram counter the <tt>ResultCounter</tt> that keeps track of how
     *  mbny results have been returned for this query
	 */
	privbte QueryHandler(QueryRequest query, int results, ReplyHandler handler,
                         ResultCounter counter) {
        if( query == null )
            throw new IllegblArgumentException("null query");
        if( hbndler == null )
            throw new IllegblArgumentException("null reply handler");
        if( counter == null )
            throw new IllegblArgumentException("null result counter");
            
		boolebn isHashQuery = !query.getQueryUrns().isEmpty();
		QUERY = query;
		if(isHbshQuery) {
			RESULTS = HASH_QUERY_RESULTS;
		} else {
			RESULTS = results;
		}

		REPLY_HANDLER = hbndler;
        RESULT_COUNTER = counter;
        _prefLocble = handler.getLocalePref();
	}


	/**
	 * Fbctory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @pbram guid the <tt>QueryRequest</tt> instance containing data
	 *  for this set of queries
	 * @pbram handler the <tt>ReplyHandler</tt> for routing the replies
     * @pbram counter the <tt>ResultCounter</tt> that keeps track of how
     *  mbny results have been returned for this query
	 * @return the <tt>QueryHbndler</tt> instance for this query
	 */
	public stbtic QueryHandler createHandler(QueryRequest query, 
											 ReplyHbndler handler,
                                             ResultCounter counter) {	
		return new QueryHbndler(query, ULTRAPEER_RESULTS, handler, counter);
	}

	/**
	 * Fbctory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.  Used by supernodes to run
     * their own queries (ties up to ForMeReplyHbndler.instance()).
	 *
	 * @pbram guid the <tt>QueryRequest</tt> instance containing data
	 *  for this set of queries
     * @pbram counter the <tt>ResultCounter</tt> that keeps track of how
     *  mbny results have been returned for this query
	 * @return the <tt>QueryHbndler</tt> instance for this query
	 */
	public stbtic QueryHandler createHandlerForMe(QueryRequest query, 
                                                  ResultCounter counter) {	
        // becbuse UPs seem to get less results, give them more than usual
		return new QueryHbndler(query, (int)(ULTRAPEER_RESULTS * UP_RESULT_BUMP),
                                ForMeReplyHbndler.instance(), counter);
	}

	/**
	 * Fbctory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @pbram guid the <tt>QueryRequest</tt> instance containing data
	 *  for this set of queries
	 * @pbram handler the <tt>ReplyHandler</tt> for routing the replies
     * @pbram counter the <tt>ResultCounter</tt> that keeps track of how
     *  mbny results have been returned for this query
	 * @return the <tt>QueryHbndler</tt> instance for this query
	 */
	public stbtic QueryHandler createHandlerForOldLeaf(QueryRequest query, 
													   ReplyHbndler handler,
                                                       ResultCounter counter) {	
		return new QueryHbndler(query, OLD_LEAF_RESULTS, handler, counter);
	}

	/**
	 * Fbctory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given <tt>QueryRequest</tt>.
	 *
	 * @pbram guid the <tt>QueryRequest</tt> instance containing data
	 *  for this set of queries
	 * @pbram handler the <tt>ReplyHandler</tt> for routing the replies
     * @pbram counter the <tt>ResultCounter</tt> that keeps track of how
     *  mbny results have been returned for this query
	 * @return the <tt>QueryHbndler</tt> instance for this query
	 */
	public stbtic QueryHandler createHandlerForNewLeaf(QueryRequest query, 
													   ReplyHbndler handler,
                                                       ResultCounter counter) {		
		return new QueryHbndler(query, NEW_LEAF_RESULTS, handler, counter);
	}

	/**
	 * Fbctory method for creating new <tt>QueryRequest</tt> instances with
	 * the sbme guid, query, xml query, urn types, etc.
	 *
	 * @pbram ttl the time to live of the new query
	 * @return b new <tt>QueryRequest</tt> instance with all of the 
	 *  pre-defined pbrameters and the specified TTL
	 * @throw <tt>IllegblArgumentException</tt> if the ttl is not within
	 *  whbt is considered reasonable bounds
	 * @throw NullPointerException if the <tt>query</tt> brgument is 
	 *    <tt>null</tt>
	 */
	public stbtic QueryRequest createQuery(QueryRequest query, byte ttl) {
		if(ttl < 1 || ttl > MAX_QUERY_TTL) 
			throw new IllegblArgumentException("ttl too high: "+ttl);
		if(query == null) {
			throw new NullPointerException("null query");
		}

		// build it from scrbtch if it's from us
		if(query.getHops() == 0) {
			return QueryRequest.crebteQuery(query, ttl);
		} else {
			try {
				return QueryRequest.crebteNetworkQuery(query.getGUID(), ttl, 
													   query.getHops(), 
													   query.getPbyload(),
													   query.getNetwork());
			} cbtch(BadPacketException e) {
				// this should never hbppen, since the query was already 
				// rebd from the network, so report an error
				ErrorService.error(e);
				return null;
			}
		}
	}

    /**
     * Convenience method for crebting a new query with the given TTL
     * with this <tt>QueryHbndler</tt>.
     *
     * @pbram ttl the time to live for the new query
     */
    QueryRequest crebteQuery(byte ttl) {
        return crebteQuery(QUERY, ttl);
    }


	
	/**
	 * Sends the query to the current connections.  If the query is not
	 * yet rebdy to be processed, this returns immediately.
	 */
	public void sendQuery() {
		if(hbsEnoughResults()) return;

		_curTime = System.currentTimeMillis();
		if(_curTime < _nextQueryTime) return;

        if (LOG.isTrbceEnabled())
            LOG.trbce("Query = " + QUERY.getQuery() +
                      ", numHostsQueried: " + _theoreticblHostsQueried);

		if(_queryStbrtTime == 0) {
			_queryStbrtTime = _curTime;
        }
            
        // hbndle 3 query cases

        // 1) If we hbven't sent the query to our leaves, send it
        if(!_forwbrdedToLeaves) {

            _forwbrdedToLeaves = true;
            QueryRouteTbble qrt = 
                RouterService.getMessbgeRouter().getQueryRouteTable();

            QueryRequest query = crebteQuery(QUERY, (byte)1);

            _theoreticblHostsQueried += 25;
                
            // send the query to our lebves if there's a hit and wait,
            // otherwise we'll move on to the probe
            if(qrt != null && qrt.contbins(query)) {
                RouterService.getMessbgeRouter().
                    forwbrdQueryRequestToLeaves(query, 
                                                REPLY_HANDLER); 
                _nextQueryTime = 
                    System.currentTimeMillis() + _timeToWbitPerHop;
                return;
            }
        }
        
        // 2) If we hbven't sent the probe query, send it
        if(!_probeQuerySent) {
            ProbeQuery pq = 
                new ProbeQuery(_connectionMbnager.getInitializedConnections(),
                               this);
            long timeToWbit = pq.getTimeToWait();            
            _theoreticblHostsQueried += pq.sendProbe();
            _nextQueryTime = 
                System.currentTimeMillis() + timeToWbit;
            _probeQuerySent = true;
            return;
        }

        // 3) If we hbven't yet satisfied the query, keep trying
        else {
            // Otherwise, just send b normal query -- make a copy of the 
            // connections becbuse we'll be modifying it.
            int newHosts = 
                sendQuery(
                    new ArrbyList(
                            _connectionMbnager.getInitializedConnections()));
            if(newHosts == 0) {
                // if we didn't query bny new hosts, wait awhile for new
                // connections to potentiblly appear
                _nextQueryTime = System.currentTimeMillis() + 6000;
            }   
            _theoreticblHostsQueried += newHosts;

            // if we've blready queried quite a few hosts, not gotten
            // mbny results, and have been querying for awhile, start
            // decrebsing the per-hop wait time
            if(_timeToWbitPerHop > 100 &&
               (System.currentTimeMillis() - _queryStbrtTime) > 6000) {
                _timeToWbitPerHop -= _timeToDecreasePerHop;

                int resultFbctor =
                    Mbth.max(1, 
                        (RESULTS/2)-(30*RESULT_COUNTER.getNumResults()));

                int decrementFbctor = Math.max(1, (_numDecrements/6));

                // the current decrebse is weighted based on the number
                // of results returned bnd on the number of connections
                // we've tried -- the fewer results bnd the more 
                // connections, the more the decrebse
                int currentDecrebse = resultFactor * decrementFactor;

                currentDecrebse = 
                    Mbth.max(5, currentDecrease);
                _timeToDecrebsePerHop += currentDecrease; 

                _numDecrements++;
                if(_timeToWbitPerHop < 100) {
                    _timeToWbitPerHop = 100;
                }
            }
        }
    }

    /**
     * Sends b query to one of the specified <tt>List</tt> of connections.  
     * This is the hebrt of the dynamic query.  We dynamically calculate the
     * bppropriate TTL to use based on our current estimate of how widely the
     * file is distributed, how mbny connections we have, etc.  This is static
     * to decouple the blgorithm from the specific <tt>QueryHandler</tt>
     * instbnce, making testing significantly easier.
     *
     * @pbram handler the <tt>QueryHandler</tt> instance containing data
     *  for this query
     * @pbram list the <tt>List</tt> of Gnutella connections to send
     *  queries over
     * @return the number of new hosts theoreticblly reached by this
     *  query iterbtion
     */
    privbte int sendQuery(List ultrapeersAll) {

        //we wbnt to try to use all connections in ultrapeersLocale first.
        List ultrbpeers = /** method returns a copy */
            _connectionMbnager.getInitializedConnectionsMatchLocale
            (_prefLocble);
            
        QUERIED_CONNECTIONS.retbinAll(ultrapeersAll);
        QUERIED_PROBE_CONNECTIONS.retbinAll(ultrapeersAll);
        
        //if we did get b list of connections that matches the locale
        //of the query
        if(!ultrbpeers.isEmpty()) {
            ultrbpeers.removeAll(QUERIED_CONNECTIONS);
            ultrbpeers.removeAll(QUERIED_PROBE_CONNECTIONS);
            //bt this point ultrapeers could become empty
        }
                
        if(ultrbpeers.isEmpty()) { 
            ultrbpeers = ultrapeersAll;
            // now, remove bny connections we've used from our current list
            // of connections to try
            ultrbpeers.removeAll(QUERIED_CONNECTIONS);
            ultrbpeers.removeAll(QUERIED_PROBE_CONNECTIONS);
        }

		int length = ultrbpeers.size();
        if (LOG.isTrbceEnabled())
            LOG.trbce("potential querier size: " + length);
        byte ttl = 0;
        MbnagedConnection mc = null;

        // bdd randomization to who we send our queries to
        Collections.shuffle(ultrbpeers);

        // weed out bll connections that aren't yet stable
        for(int i=0; i<length; i++) {
			MbnagedConnection curConnection = 
                (MbnagedConnection)ultrapeers.get(i);			

			// if the connection hbsn't been up for long, don't use it,
            // bs the replies will never make it back to us if the
            // connection is dropped, wbsting bandwidth
            if(!curConnection.isStbble(_curTime)) continue;
            mc = curConnection;
            brebk;
        }

        int rembiningConnections = 
            Mbth.max(length+QUERIED_PROBE_CONNECTIONS.size(), 0);

        // return if we don't hbve any connections to query at this time
        if(rembiningConnections == 0) return 0;

        // pretend we hbve fewer connections than we do in case we
        // lose some
        if(rembiningConnections > 4) remainingConnections -= 4;

        boolebn probeConnection = false;
        
        // mc cbn still be null if the list of connections was empty.
        if(mc == null) {
            // if we hbve no connections to query, simply return for now
            if(QUERIED_PROBE_CONNECTIONS.isEmpty()) {
                return 0;
            }
            
            // we bctually remove this from the list to make sure that
            // QUERIED_CONNECTIONS bnd QUERIED_PROBE_CONNECTIONS do
            // not hbve any of the same entries, as this connection
            // will be bdded to QUERIED_CONNECTIONS
            mc = (MbnagedConnection)QUERIED_PROBE_CONNECTIONS.remove(0);
            probeConnection = true;
        }
        
        int results = (_numResultsReportedByLebf > 0 ? 
                       _numResultsReportedByLebf : 
                       RESULT_COUNTER.getNumResults());
        double resultsPerHost = 
            (double)results/(double)_theoreticblHostsQueried;
			
        int resultsNeeded = RESULTS - results;
        int hostsToQuery = 40000;
        if(resultsPerHost != 0) {
            hostsToQuery = (int)(resultsNeeded/resultsPerHost);
        }
                
        
        int hostsToQueryPerConnection = 
            hostsToQuery/rembiningConnections;;
        
        ttl = cblculateNewTTL(hostsToQueryPerConnection, 
                              mc.getNumIntrbUltrapeerConnections(),
                              mc.hebders().getMaxTTL());
               

        // If we're sending the query down b probe connection and we've
        // blready used that connection, or that connection doesn't have
        // b hit for the query, send it at TTL=2.  In these cases, 
        // sending the query bt TTL=1 is pointless because we've either
        // blready sent this query, or the Ultrapeer doesn't have a 
        // mbtch anyway
        if(ttl == 1 && 
           ((mc.isUltrbpeerQueryRoutingConnection() &&
            !mc.shouldForwbrdQuery(QUERY)) || probeConnection)) {
            ttl = 2;
        }
        QueryRequest query = crebteQuery(QUERY, ttl);

        // send out the query on the network, returning the number of new
        // hosts theoreticblly reached
        return sendQueryToHost(query, mc, this);        
	}
    

    /**
     * Sends b query to the specified host.
     *
     * @pbram query the <tt>QueryRequest</tt> to send
     * @pbram mc the <tt>ManagedConnection</tt> to send the query to
     * @pbram handler the <tt>QueryHandler</tt> 
     * @return the number of new hosts theoreticblly hit by this query
     */
    stbtic int sendQueryToHost(QueryRequest query, 
                               MbnagedConnection mc, 
                               QueryHbndler handler) {
        
        // send the query directly blong the connection, but if the query didn't
        // go through send bbck 0....
        if (!_messbgeRouter.originateQuery(query, mc)) return 0;
        
        byte ttl = query.getTTL();

        // bdd the reply handler to the list of queried hosts if it's not
        // b TTL=1 query or the connection does not support probe queries

        // bdds the connection to the list of probe connections if it's
        // b TTL=1 query to a connection that supports probe extensions,
        // otherwise bdd it to the list of connections we've queried
        if(ttl == 1 && mc.supportsProbeQueries()) {
            hbndler.QUERIED_PROBE_CONNECTIONS.add(mc);
        } else {
            hbndler.QUERIED_CONNECTIONS.add(mc);
            if (LOG.isTrbceEnabled())
                LOG.trbce("QUERIED_CONNECTIONS.size() = " +
                          hbndler.QUERIED_CONNECTIONS.size());
        }

        if (LOG.isTrbceEnabled())
            LOG.trbce("Querying host " + mc.getAddress() + " with ttl " +
                      query.getTTL());
        
        hbndler._nextQueryTime = System.currentTimeMillis() + 
            (ttl * hbndler._timeToWaitPerHop);

        return cblculateNewHosts(mc, ttl);
    }

	/**
	 * Cblculates the new TTL to use based on the number of hosts per connection
	 * we still need to query.
	 * 
	 * @pbram hostsToQueryPerConnection the number of hosts we should reach on
	 *  ebch remaining connections, to the best of our knowledge
     * @pbram degree the out-degree of the next connection
     * @pbram maxTTL the maximum TTL the connection will allow
     * @return the TTL to use for the next connection
	 */
	privbte static byte 
        cblculateNewTTL(int hostsToQueryPerConnection, int degree, 
                        byte mbxTTL) {

        if (mbxTTL > MAX_QUERY_TTL) maxTTL = MAX_QUERY_TTL;

        // not the most efficient blgorithm -- should use Math.log, but
        // thbt's ok
        for(byte i=1; i<MAX_QUERY_TTL; i++) {

            // bibsed towards lower TTLs since the horizon expands so
            // quickly
            int hosts = (int)(16.0*cblculateNewHosts(degree, i));            
            if(hosts >= hostsToQueryPerConnection) {
                if(i > mbxTTL) return maxTTL;
                return i;
            }
        }
        return mbxTTL;
	}

	/**
     * Cblculate the number of new hosts that would be added to the 
     * theoreticbl horizon if a query with the given ttl were sent down
     * the given connection.
	 *
     * @pbram conn the <tt>Connection</tt> that will received the query
	 * @pbram ttl the TTL of the query to add
	 */
	privbte static int calculateNewHosts(Connection conn, byte ttl) {
        return cblculateNewHosts(conn.getNumIntraUltrapeerConnections(), ttl);
	}

	/**
     * Cblculate the number of new hosts that would be added to the 
     * theoreticbl horizon if a query with the given ttl were sent to
     * b node with the given degree.  This is not precise because we're
     * bssuming that the nodes connected to the node in question also
     * hbve the same degree, but there's not much we can do about it!
	 *
     * @pbram degree the degree of the node that will received the query
	 * @pbram ttl the TTL of the query to add
	 */    
	privbte static int calculateNewHosts(int degree, byte ttl) {
		double newHosts = 0;
		for(;ttl>0; ttl--) {
			newHosts += Mbth.pow((degree-1), ttl-1);
		}
		return (int)newHosts;
	}

	/**
	 * Returns whether or not this query hbs received enough results.
	 *
	 * @return <tt>true</tt> if this query hbs received enough results,
	 *  <tt>fblse</tt> otherwise
	 */
	public boolebn hasEnoughResults() {		
		// return fblse if the query hasn't started yet
		if(_queryStbrtTime == 0) return false;

        // ----------------
        // NOTE: bs agreed, _numResultsReportedByLeaf is the number of results
        // the lebf has received/consumed by a filter DIVIDED by 4 (4 being the
        // number of UPs connection it mbintains).  That is why we don't divide
        // it here or bnything.  We aren't sure if this mixes well with
        // BebrShare's use but oh well....
        // ----------------
        // if lebf guidance is in effect, we have different criteria.
        if (_numResultsReportedByLebf > 0) {
            // we shouldn't route too much regbrdless of what the leaf says
            if (RESULT_COUNTER.getNumResults() >= MAXIMUM_ROUTED_FOR_LEAVES)
                return true;
            // if the lebf is happy, so are we....
            if (_numResultsReportedByLebf > RESULTS)
                return true;
        }
        // lebf guidance is not in effect or we are doing our own query
        else if (RESULT_COUNTER.getNumResults() >= RESULTS)
            return true;

        // if our theoreticbl horizon has gotten too high, consider
        // it enough results
        // precisely whbt this number should be is somewhat hard to determine
        // becbuse, while connection have a specfic degree, the degree of 
        // the connections on subsequent hops cbnnot be determined
		if(_theoreticblHostsQueried > 110000) {
            return true;
        }

		// return true if we've been querying for longer thbn the specified 
		// mbximum
		int queryLength = (int)(System.currentTimeMillis() - _queryStbrtTime);
		if(queryLength > MAX_QUERY_TIME) {
            return true;
        }

		return fblse;
	}

    /**
     * Use this to modify the number of results bs reported by the leaf you are
     * querying for.
     */
    public void updbteLeafResults(int numResults) {
        if (numResults > _numResultsReportedByLebf)
            _numResultsReportedByLebf = numResults;
    }

    /**
     * Returns the number of results bs reported by the leaf.  At least 0.
     */
    public int getNumResultsReportedByLebf() {
        return _numResultsReportedByLebf;
    }

    /**
     * Accessor for the <tt>ReplyHbndler</tt> instance for the connection
     * issuing this request.
     *
     * @return the <tt>ReplyHbndler</tt> for the connection issuing this 
     *  request
     */
    public ReplyHbndler getReplyHandler() {
        return REPLY_HANDLER;
    }
    
    /**
     * Accessor for the time to wbit per hop, in milliseconds,
     * for this QueryHbndler.
     *
     * @return the time to wbit per hop in milliseconds for this
     *  QueryHbndler
     */
    public long getTimeToWbitPerHop() {
        return _timeToWbitPerHop;
    }

	// overrides Object.toString
	public String toString() {
		return "QueryHbndler: QUERY: "+QUERY;
	}

    /** @return simply returns the guid of the query this is hbndling.
     */
    public GUID getGUID() {
        return new GUID(QUERY.getGUID());
    }

}



