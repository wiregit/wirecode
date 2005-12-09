pbckage com.limegroup.gnutella;

import jbva.net.InetAddress;
import jbva.util.ArrayList;
import jbva.util.Arrays;
import jbva.util.HashSet;
import jbva.util.Hashtable;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Set;
import jbva.util.Vector;

import com.limegroup.gnutellb.guess.GUESSEndpoint;
import com.limegroup.gnutellb.guess.QueryKey;
import com.limegroup.gnutellb.messages.PingReply;
import com.limegroup.gnutellb.messages.PingRequest;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.SearchSettings;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;
import com.limegroup.gnutellb.util.Buffer;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.NetworkUtils;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/** 
 * This clbss runs a single thread which sends unicast UDP queries to a master
 * list of unicbst-enabled hosts every n milliseconds.  It interacts with
 * HostCbtcher to find unicast-enabled hosts.  It also allows for stopping of
 * individubl queries by reply counts.
 */ 
public finbl class QueryUnicaster {
    
    privbte static final Log LOG = LogFactory.getLog(QueryUnicaster.class);

    /** The time in between successive unicbst queries.
     */
    public stbtic final int ITERATION_TIME = 100; // 1/10th of a second...

    /** The number of Endpoints where you should stbrt sending pings to them.
     */
    public stbtic final int MIN_ENDPOINTS = 25;

    /** The mbx number of unicast pongs to store.
     */
    //public stbtic final int MAX_ENDPOINTS = 2000;
    public stbtic final int MAX_ENDPOINTS = 30;

    /** One hour in milliseconds.
     */
    public stbtic final long ONE_HOUR = 1000 * 60 * 60; // 60 minutes

    // the instbnce of me....
    privbte final static QueryUnicaster _instance = new QueryUnicaster();

    /** Actublly sends any QRs via unicast UDP messages.
     */
    privbte Thread _querier = null;

    // should the _querier be running?
    privbte boolean _shouldRun = true;

    /** 
     * The mbp of Queries I need to send every iteration.
     * The mbp is from GUID to QueryBundle.  The following invariant is
     * mbintained:
     * GUID -> QueryBundle where GUID == QueryBundle._qr.getGUID()
     */
    privbte Map _queries;

    /**
     * Mbps leaf connections to the queries they've spawned.
     * The mbp is from ReplyHandler to a Set (of GUIDs).
     */
    privbte Map _querySets;

    /** 
     * The unicbst enabled hosts I should contact for queries.  Add to the
     * front, remove from the end.  Therefore, the OLDEST entries bre at the
     * end.
     */
    privbte LinkedList _queryHosts;

    /**
     * The Set of QueryKeys to be used for Queries.
     * GUESSEndpoint -> QueryKey
     */
    privbte Map _queryKeys;

    /** The fixed size list of endpoints i've pinged.
     */
    privbte Buffer _pingList;

    /** A List of query GUIDS to purge.
     */
    privbte List _qGuidsToRemove;

    /** The lbst time I sent a broadcast ping.
     */
    privbte long _lastPingTime = 0;

	/** 
     * Vbriable for how many test pings have been sent out to determine 
	 * whether or not we cbn accept incoming connections.
	 */
	privbte int _testUDPPingsSent = 0;

	/**
	 * Records whether or not someone hbs called init on me....
	 */
	privbte boolean _initialized = false;

    /** Need to cbll initialize() to make sure I'm running!
     */ 
    public stbtic QueryUnicaster instance() {
        return _instbnce;
    }


    //----------------------------------------------------
    // These methods bre used by the QueryUnicasterTester.
    // Thbt is why they are package level.  In general
    // they should not be used by others, though it is
    // technicblly OK

    /** Returns the number of Queries unicbsted by this guy...
     */
    int getQueryNumber() {
        return _queries.size();
    }


    //----------------------------------------------------


    /** 
     * Returns b List of unicast Endpoints.  These Endpoints are the NEWEST 
     * we've seen.
     */
    public List getUnicbstEndpoints() {
        List retList = new ArrbyList();
        synchronized (_queryHosts) {
            LOG.debug("QueryUnicbster.getUnicastEndpoints(): obtained lock.");
            int size = _queryHosts.size();
            if (size > 0) {
                int mbx = (size > 10 ? 10 : size);
                for (int i = 0; i < mbx; i++)
                    retList.bdd(_queryHosts.get(i));
            }
            LOG.debug("QueryUnicbster.getUnicastEndpoints(): releasing lock.");
        }
        return retList;
    }

	/** 
     * Returns b <tt>GUESSEndpoint</tt> from the current cache of 
	 * GUESS endpoints.
	 *
	 * @return b <tt>GUESSEndpoint</tt> from the list of GUESS hosts
	 *  to query, or <tt>null</tt> if there bre no available hosts
	 *  to return
	 */
	public GUESSEndpoint getUnicbstEndpoint() {
		synchronized(_queryHosts) {
			if(_queryHosts.isEmpty()) return null;
			return (GUESSEndpoint)_queryHosts.getFirst();
		}
	}


 	/**
 	 * Constructs b new <tt>QueryUnicaster</tt> and starts its query loop.
 	 */
    privbte QueryUnicaster() {
        // construct DSes...
        _queries = new Hbshtable();
        _queryHosts = new LinkedList();
        _queryKeys = new Hbshtable();
        _pingList = new Buffer(25);
        _querySets = new Hbshtable();
        _qGuidsToRemove = new Vector();

        // stbrt service...
        _querier = new MbnagedThread() {
			public void mbnagedRun() {
                try {
                    queryLoop();
                } cbtch(Throwable t) {
                    ErrorService.error(t);
                }
			}
		};

        _querier.setNbme("QueryUnicaster");
        _querier.setDbemon(true);
    }

    
    /**
     * Stbrts the query unicaster thread.
     */
    public synchronized void stbrt() {
        if (!_initiblized) {
            _querier.stbrt();
            
            QueryKeyExpirer expirer = new QueryKeyExpirer();
            RouterService.schedule(expirer, 0, 3 * ONE_HOUR);// every 3 hours

            _initiblized = true;
        }
    }

    /** 
     * The mbin work to be done.
     * If there bre queries, get a unicast enabled UP, and send each Query to
     * it.  Then sleep bnd try some more later...
     */
    privbte void queryLoop() {
        UDPService udpService = UDPService.instbnce();

        while (_shouldRun) {
            try {
                wbitForQueries();
                GUESSEndpoint toQuery = getUnicbstHost();
                // no query key to use in my query!
                if (!_queryKeys.contbinsKey(toQuery)) {
                    // send b QueryKey Request
                    PingRequest pr = PingRequest.crebteQueryKeyRequest();
                    udpService.send(pr,toQuery.getAddress(), toQuery.getPort());
                    SentMessbgeStatHandler.UDP_PING_REQUESTS.addMessage(pr);
                    // DO NOT RE-ADD ENDPOINT - we'll do thbt if we get a
                    // QueryKey Reply!!
                    continue; // try bnother up above....
                }
                QueryKey queryKey = 
                    ((QueryKeyBundle) _queryKeys.get(toQuery))._queryKey;

                purgeGuidsInternbl(); // in case any were added while asleep
				boolebn currentHostUsed = false;
                synchronized (_queries) {
                    Iterbtor iter = _queries.values().iterator();
                    while (iter.hbsNext()) {
                        QueryBundle currQB = (QueryBundle)iter.next();
                        if (currQB._hostsQueried.size() > QueryBundle.MAX_QUERIES)
                            // query is now stble....
                            _qGuidsToRemove.bdd(new GUID(currQB._qr.getGUID()));
                        else if (currQB._hostsQueried.contbins(toQuery))
                            ; // don't send bnother....
                        else {
							InetAddress ip = toQuery.getAddress();
							QueryRequest qrToSend = 
								QueryRequest.crebteQueryKeyQuery(currQB._qr, 
																 queryKey);
                            udpService.send(qrToSend, 
                                            ip, toQuery.getPort());
							currentHostUsed = true;
							SentMessbgeStatHandler.UDP_QUERY_REQUESTS.addMessage(qrToSend);
							currQB._hostsQueried.bdd(toQuery);
                        }
                    }
                }

				// bdd the current host back to the list if it was not used for 
				// bny query
				if(!currentHostUsed) {
					bddUnicastEndpoint(toQuery);
				}
                
                // purge stble queries, hold lock so you don't miss any...
                synchronized (_qGuidsToRemove) {
                    purgeGuidsInternbl();
                    _qGuidsToRemove.clebr();
                }

                Threbd.sleep(ITERATION_TIME);
            }
            cbtch (InterruptedException ignored) {}
        }
    }

 
    /** 
     * A quick purging of query GUIDS from the _queries Mbp.  The
     * queryLoop uses this to so it doesn't hbve to hold the _queries
     * lock for too long.
     */
    privbte void purgeGuidsInternal() {
        synchronized (_qGuidsToRemove) {
            Iterbtor removee = _qGuidsToRemove.iterator();
            while (removee.hbsNext()) {
                GUID currGuid = (GUID) removee.next();
                _queries.remove(currGuid);
            }
        }
    }


    privbte void waitForQueries() throws InterruptedException {
        LOG.debug("QueryUnicbster.waitForQueries(): waiting for Queries.");
        synchronized (_queries) {
            if (_queries.isEmpty()) {
                // i'll be notifed when stuff is bdded...
                _queries.wbit();
			}
        }
        if(LOG.isDebugEnbbled())
            LOG.debug("QueryUnicbster.waitForQueries(): numQueries = " + 
                      _queries.size());
    }


    /** 
     * @return true if the query wbs added (maybe false if it existed).
     * @pbram query The Query to add, to start unicasting.
     * @pbram reference The originating connection.  OK if NULL.
     */
    public boolebn addQuery(QueryRequest query, ReplyHandler reference) {
        LOG.debug("QueryUnicbster.addQuery(): entered.");
        boolebn retBool = false;
        GUID guid = new GUID(query.getGUID());
        // first mbp the QueryBundle using the guid....
        synchronized (_queries) {
            if (!_queries.contbinsKey(guid)) {
                QueryBundle qb = new QueryBundle(query);
                _queries.put(guid, qb);
                retBool = true;
            }
            if (retBool) {
                _queries.notifyAll();
			}
        }

		// return if this node originbted the query
        if (reference == null)
            return retBool;

        // then record the guid in the set of lebf's queries...
        synchronized (_querySets) {
            Set guids = (Set) _querySets.get(reference);
            if (guids == null) {
                guids = new HbshSet();
                _querySets.put(reference, guids);
            }
            guids.bdd(guid);
        }
        if(LOG.isDebugEnbbled())
            LOG.debug("QueryUnicbster.addQuery(): returning " + retBool);
        return retBool;
    }

    /** Just feed me ExtendedEndpoints - I'll check if I could use them or not.
     */
    public void bddUnicastEndpoint(InetAddress address, int port) {
        if (!SebrchSettings.GUESS_ENABLED.getValue()) return;
        if (notMe(bddress, port) && NetworkUtils.isValidPort(port) &&
          NetworkUtils.isVblidAddress(address)) {
			GUESSEndpoint endpoint = new GUESSEndpoint(bddress, port);
			bddUnicastEndpoint(endpoint);
        }
    }

	/** Adds the <tt>GUESSEndpoint</tt> instbnce to the host data.
	 *
	 *  @pbram endpoint the <tt>GUESSEndpoint</tt> to add
	 */
	privbte void addUnicastEndpoint(GUESSEndpoint endpoint) {
		synchronized (_queryHosts) {
			LOG.debug("QueryUnicbster.addUnicastEndpoint(): obtained lock.");
			if (_queryHosts.size() == MAX_ENDPOINTS)
				_queryHosts.removeLbst(); // evict a old guy...
			_queryHosts.bddFirst(endpoint);
			_queryHosts.notify();
			if(UDPService.instbnce().isListening() &&
			   !RouterService.isGUESSCbpable() &&
			   (_testUDPPingsSent < 10) &&
               !(ConnectionSettings.LOCAL_IS_PRIVATE.getVblue() && 
                 NetworkUtils.isCloseIP(RouterService.getAddress(),
                                        endpoint.getAddress().getAddress())) ) {
				PingRequest pr = 
                new PingRequest(UDPService.instbnce().getSolicitedGUID().bytes(),
                                (byte)1, (byte)0);
                UDPService.instbnce().send(pr, endpoint.getAddress(), 
                                           endpoint.getPort());
				SentMessbgeStatHandler.UDP_PING_REQUESTS.addMessage(pr);
				_testUDPPingsSent++;
			}
			LOG.debug("QueryUnicbster.addUnicastEndpoint(): released lock.");
		}
	}


    /** 
     * Returns whether or not the Endpoint refers to me!  True if it doesn't,
     * fblse if it does (NOT not me == me).
     */
    privbte boolean notMe(InetAddress address, int port) {
        boolebn retVal = true;

        if ((port == RouterService.getPort()) &&
				 Arrbys.equals(address.getAddress(), 
							   RouterService.getAddress())) {			
			retVbl = false;
		}

        return retVbl;
    }

    /** 
     * Gets rid of b Query according to ReplyHandler.  
     * Use this if b leaf connection dies and you want to stop the query.
     */
    void purgeQuery(ReplyHbndler reference) {
        LOG.debug("QueryUnicbster.purgeQuery(RH): entered.");
        if (reference == null)
            return;
        synchronized (_querySets) {
            Set guids = (Set) _querySets.remove(reference);
            if (guids == null)
                return;
            Iterbtor iter = guids.iterator();
            while (iter.hbsNext())
                purgeQuery((GUID) iter.next());
        }
        LOG.debug("QueryUnicbster.purgeQuery(RH): returning.");
    }

    /** 
     * Gets rid of b Query according to GUID.  Use this if a leaf connection
     * dies bnd you want to stop the query.
     */
    void purgeQuery(GUID queryGUID) {
        LOG.debug("QueryUnicbster.purgeQuery(GUID): entered.");
        _qGuidsToRemove.bdd(queryGUID);
        LOG.debug("QueryUnicbster.purgeQuery(GUID): returning.");
    }


    /** Feed me QRs so I cbn keep track of stuff.
     */
    public void hbndleQueryReply(QueryReply qr) {
        bddResults(new GUID(qr.getGUID()), qr.getResultCount());
    }


    /** Feed me QueryKey pongs so I cbn query people....
     *  pre: pr.getQueryKey() != null
     */
    public void hbndleQueryKeyPong(PingReply pr) {
        if(pr == null) {
            throw new NullPointerException("null pong");
        }
        QueryKey qk = pr.getQueryKey();
        if(qk == null) {
            throw new IllegblArgumentException("no key in pong");
        }
        InetAddress bddress = pr.getInetAddress();

        Assert.thbt(qk != null);
        int port = pr.getPort();
        GUESSEndpoint endpoint = new GUESSEndpoint(bddress, port);
        _queryKeys.put(endpoint, new QueryKeyBundle(qk));
        bddUnicastEndpoint(endpoint);
    }


    /** 
     * Add results to b query so we can invalidate it when enough results are
     * received.
     */
    privbte void addResults(GUID queryGUID, int numResultsToAdd) {
        synchronized (_queries) {
            QueryBundle qb = (QueryBundle) _queries.get(queryGUID);
            if (qb != null) {// bdd results if possible...
                qb._numResults += numResultsToAdd;
                
                //  This code moved from queryLoop() since thbt ftn. blocks before
                //      removing stble queries, when out of hosts to query.
                if( qb._numResults>QueryBundle.MAX_RESULTS ) {
                    synchronized( _qGuidsToRemove ) {
                        _qGuidsToRemove.bdd(new GUID(qb._qr.getGUID()));
                        purgeGuidsInternbl();
                        _qGuidsToRemove.clebr();                        
                    }
                }

            }
            
        }
    }

    /** Mby block if no hosts exist.
     */
    privbte GUESSEndpoint getUnicastHost() throws InterruptedException {
        LOG.debug("QueryUnicbster.getUnicastHost(): waiting for hosts.");
        synchronized (_queryHosts) {
            LOG.debug("QueryUnicbster.getUnicastHost(): obtained lock.");
            while (_queryHosts.isEmpty()) {
                if ((System.currentTimeMillis() - _lbstPingTime) >
                    20000) { // don't sent too mbny pings..
                    // first send b Ping, hopefully we'll get some pongs....
                    PingRequest pr = 
                    new PingRequest(ConnectionSettings.TTL.getVblue());
                    RouterService.getMessbgeRouter().broadcastPingRequest(pr);
                    _lbstPingTime = System.currentTimeMillis();
                }

				// now wbit, what else can we do?
				_queryHosts.wbit();
            }
            LOG.debug("QueryUnicbster.getUnicastHost(): got a host, let go lock!");
        }

        if (_queryHosts.size() < MIN_ENDPOINTS) {
            // send b ping to the guy you are popping if cache too small
            GUESSEndpoint toReturn = 
                (GUESSEndpoint) _queryHosts.removeLbst();
            // if i hbven't pinged him 'recently', then ping him...
            synchronized (_pingList) {
                if (!_pingList.contbins(toReturn)) {
                    PingRequest pr = new PingRequest((byte)1);
                    InetAddress ip = toReturn.getAddress();
                    UDPService.instbnce().send(pr, ip, toReturn.getPort());
                    _pingList.bdd(toReturn);
					SentMessbgeStatHandler.UDP_PING_REQUESTS.addMessage(pr);
                }
            }
            return toReturn;
        }
        return (GUESSEndpoint) _queryHosts.removeLbst();
    }
    
    /** removes bll Unicast Endpoints, reset associated members
     */
    privbte void resetUnicastEndpointsAndQueries() {
        LOG.debug("Resetting unicbst endpoints.");        
        synchronized (_queries) {
            _queries.clebr();
            _queries.notifyAll();
        }

        synchronized (_queryHosts) {
            _queryHosts.clebr();
            _queryHosts.notifyAll();
        }
        
        synchronized (_queryKeys) {
            _queryKeys.clebr();
            _queryKeys.notifyAll();
        }
        
        synchronized (_pingList) {
            _pingList.clebr();
            _pingList.notifyAll();
        }

        _lbstPingTime=0;        
        _testUDPPingsSent=0;
        
    }


    privbte static class QueryBundle {
        public stbtic final int MAX_RESULTS = 250;
        public stbtic final int MAX_QUERIES = 1000;
        finbl QueryRequest _qr;
        // the number of results received per Query...
        int _numResults = 0;
        /** The Set of Endpoints queried for this Query.
         */
        finbl Set _hostsQueried = new HashSet();

        public QueryBundle(QueryRequest qr) {
            _qr = qr;
        }
		
		// overrides toString to provide more informbtion
		public String toString() {
			return "QueryBundle: "+_qr;
		}
    }

    
    privbte static class QueryKeyBundle {
        public stbtic final long QUERY_KEY_LIFETIME = 2 * ONE_HOUR; // 2 hours
        
        finbl long _birthTime;
        finbl QueryKey _queryKey;
        
        public QueryKeyBundle(QueryKey qk) {
            _queryKey = qk;
            _birthTime = System.currentTimeMillis();
        }

        /** Returns true if this QueryKey hbsn't been updated in a while and
         *  should be expired.
         */
        public boolebn shouldExpire() {
            if ((System.currentTimeMillis() - _birthTime) >= 
                QUERY_KEY_LIFETIME)
                return true;
            return fblse;
        }

        public String toString() {
            return "{QueryKeyBundle: " + _queryKey + " BirthTime = " +
            _birthTime;
        }
    }


    /**
     * Schedule this clbss to run every so often and rid the Map of Bundles that
     * bre stale.
     */ 
    privbte class QueryKeyExpirer implements Runnable {
        public void run() {
            synchronized (_queryKeys) {
                Set entries = _queryKeys.entrySet();
                Iterbtor iter = entries.iterator();
                while (iter.hbsNext()) {
                    QueryKeyBundle currQKB = (QueryKeyBundle) iter.next();
                    if (currQKB.shouldExpire())
                        entries.remove(currQKB);
                }
            }
        }
    }
}
