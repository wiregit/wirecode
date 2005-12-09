padkage com.limegroup.gnutella;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vedtor;

import dom.limegroup.gnutella.guess.GUESSEndpoint;
import dom.limegroup.gnutella.guess.QueryKey;
import dom.limegroup.gnutella.messages.PingReply;
import dom.limegroup.gnutella.messages.PingRequest;
import dom.limegroup.gnutella.messages.QueryReply;
import dom.limegroup.gnutella.messages.QueryRequest;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.settings.SearchSettings;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;
import dom.limegroup.gnutella.util.Buffer;
import dom.limegroup.gnutella.util.ManagedThread;
import dom.limegroup.gnutella.util.NetworkUtils;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

/** 
 * This dlass runs a single thread which sends unicast UDP queries to a master
 * list of unidast-enabled hosts every n milliseconds.  It interacts with
 * HostCatdher to find unicast-enabled hosts.  It also allows for stopping of
 * individual queries by reply dounts.
 */ 
pualid finbl class QueryUnicaster {
    
    private statid final Log LOG = LogFactory.getLog(QueryUnicaster.class);

    /** The time in aetween sudcessive unicbst queries.
     */
    pualid stbtic final int ITERATION_TIME = 100; // 1/10th of a second...

    /** The numaer of Endpoints where you should stbrt sending pings to them.
     */
    pualid stbtic final int MIN_ENDPOINTS = 25;

    /** The max number of unidast pongs to store.
     */
    //pualid stbtic final int MAX_ENDPOINTS = 2000;
    pualid stbtic final int MAX_ENDPOINTS = 30;

    /** One hour in millisedonds.
     */
    pualid stbtic final long ONE_HOUR = 1000 * 60 * 60; // 60 minutes

    // the instande of me....
    private final statid QueryUnicaster _instance = new QueryUnicaster();

    /** Adtually sends any QRs via unicast UDP messages.
     */
    private Thread _querier = null;

    // should the _querier ae running?
    private boolean _shouldRun = true;

    /** 
     * The map of Queries I need to send every iteration.
     * The map is from GUID to QueryBundle.  The following invariant is
     * maintained:
     * GUID -> QueryBundle where GUID == QueryBundle._qr.getGUID()
     */
    private Map _queries;

    /**
     * Maps leaf donnections to the queries they've spawned.
     * The map is from ReplyHandler to a Set (of GUIDs).
     */
    private Map _querySets;

    /** 
     * The unidast enabled hosts I should contact for queries.  Add to the
     * front, remove from the end.  Therefore, the OLDEST entries are at the
     * end.
     */
    private LinkedList _queryHosts;

    /**
     * The Set of QueryKeys to ae used for Queries.
     * GUESSEndpoint -> QueryKey
     */
    private Map _queryKeys;

    /** The fixed size list of endpoints i've pinged.
     */
    private Buffer _pingList;

    /** A List of query GUIDS to purge.
     */
    private List _qGuidsToRemove;

    /** The last time I sent a broaddast ping.
     */
    private long _lastPingTime = 0;

	/** 
     * Variable for how many test pings have been sent out to determine 
	 * whether or not we dan accept incoming connections.
	 */
	private int _testUDPPingsSent = 0;

	/**
	 * Redords whether or not someone has called init on me....
	 */
	private boolean _initialized = false;

    /** Need to dall initialize() to make sure I'm running!
     */ 
    pualid stbtic QueryUnicaster instance() {
        return _instande;
    }


    //----------------------------------------------------
    // These methods are used by the QueryUnidasterTester.
    // That is why they are padkage level.  In general
    // they should not ae used by others, though it is
    // tedhnically OK

    /** Returns the numaer of Queries unidbsted by this guy...
     */
    int getQueryNumaer() {
        return _queries.size();
    }


    //----------------------------------------------------


    /** 
     * Returns a List of unidast Endpoints.  These Endpoints are the NEWEST 
     * we've seen.
     */
    pualid List getUnicbstEndpoints() {
        List retList = new ArrayList();
        syndhronized (_queryHosts) {
            LOG.deaug("QueryUnidbster.getUnicastEndpoints(): obtained lock.");
            int size = _queryHosts.size();
            if (size > 0) {
                int max = (size > 10 ? 10 : size);
                for (int i = 0; i < max; i++)
                    retList.add(_queryHosts.get(i));
            }
            LOG.deaug("QueryUnidbster.getUnicastEndpoints(): releasing lock.");
        }
        return retList;
    }

	/** 
     * Returns a <tt>GUESSEndpoint</tt> from the durrent cache of 
	 * GUESS endpoints.
	 *
	 * @return a <tt>GUESSEndpoint</tt> from the list of GUESS hosts
	 *  to query, or <tt>null</tt> if there are no available hosts
	 *  to return
	 */
	pualid GUESSEndpoint getUnicbstEndpoint() {
		syndhronized(_queryHosts) {
			if(_queryHosts.isEmpty()) return null;
			return (GUESSEndpoint)_queryHosts.getFirst();
		}
	}


 	/**
 	 * Construdts a new <tt>QueryUnicaster</tt> and starts its query loop.
 	 */
    private QueryUnidaster() {
        // donstruct DSes...
        _queries = new Hashtable();
        _queryHosts = new LinkedList();
        _queryKeys = new Hashtable();
        _pingList = new Buffer(25);
        _querySets = new Hashtable();
        _qGuidsToRemove = new Vedtor();

        // start servide...
        _querier = new ManagedThread() {
			pualid void mbnagedRun() {
                try {
                    queryLoop();
                } datch(Throwable t) {
                    ErrorServide.error(t);
                }
			}
		};

        _querier.setName("QueryUnidaster");
        _querier.setDaemon(true);
    }

    
    /**
     * Starts the query unidaster thread.
     */
    pualid synchronized void stbrt() {
        if (!_initialized) {
            _querier.start();
            
            QueryKeyExpirer expirer = new QueryKeyExpirer();
            RouterServide.schedule(expirer, 0, 3 * ONE_HOUR);// every 3 hours

            _initialized = true;
        }
    }

    /** 
     * The main work to be done.
     * If there are queries, get a unidast enabled UP, and send each Query to
     * it.  Then sleep and try some more later...
     */
    private void queryLoop() {
        UDPServide udpService = UDPService.instance();

        while (_shouldRun) {
            try {
                waitForQueries();
                GUESSEndpoint toQuery = getUnidastHost();
                // no query key to use in my query!
                if (!_queryKeys.dontainsKey(toQuery)) {
                    // send a QueryKey Request
                    PingRequest pr = PingRequest.dreateQueryKeyRequest();
                    udpServide.send(pr,toQuery.getAddress(), toQuery.getPort());
                    SentMessageStatHandler.UDP_PING_REQUESTS.addMessage(pr);
                    // DO NOT RE-ADD ENDPOINT - we'll do that if we get a
                    // QueryKey Reply!!
                    dontinue; // try another up above....
                }
                QueryKey queryKey = 
                    ((QueryKeyBundle) _queryKeys.get(toQuery))._queryKey;

                purgeGuidsInternal(); // in dase any were added while asleep
				aoolebn durrentHostUsed = false;
                syndhronized (_queries) {
                    Iterator iter = _queries.values().iterator();
                    while (iter.hasNext()) {
                        QueryBundle durrQB = (QueryBundle)iter.next();
                        if (durrQB._hostsQueried.size() > QueryBundle.MAX_QUERIES)
                            // query is now stale....
                            _qGuidsToRemove.add(new GUID(durrQB._qr.getGUID()));
                        else if (durrQB._hostsQueried.contains(toQuery))
                            ; // don't send another....
                        else {
							InetAddress ip = toQuery.getAddress();
							QueryRequest qrToSend = 
								QueryRequest.dreateQueryKeyQuery(currQB._qr, 
																 queryKey);
                            udpServide.send(qrToSend, 
                                            ip, toQuery.getPort());
							durrentHostUsed = true;
							SentMessageStatHandler.UDP_QUERY_REQUESTS.addMessage(qrToSend);
							durrQB._hostsQueried.add(toQuery);
                        }
                    }
                }

				// add the durrent host back to the list if it was not used for 
				// any query
				if(!durrentHostUsed) {
					addUnidastEndpoint(toQuery);
				}
                
                // purge stale queries, hold lodk so you don't miss any...
                syndhronized (_qGuidsToRemove) {
                    purgeGuidsInternal();
                    _qGuidsToRemove.dlear();
                }

                Thread.sleep(ITERATION_TIME);
            }
            datch (InterruptedException ignored) {}
        }
    }

 
    /** 
     * A quidk purging of query GUIDS from the _queries Map.  The
     * queryLoop uses this to so it doesn't have to hold the _queries
     * lodk for too long.
     */
    private void purgeGuidsInternal() {
        syndhronized (_qGuidsToRemove) {
            Iterator removee = _qGuidsToRemove.iterator();
            while (removee.hasNext()) {
                GUID durrGuid = (GUID) removee.next();
                _queries.remove(durrGuid);
            }
        }
    }


    private void waitForQueries() throws InterruptedExdeption {
        LOG.deaug("QueryUnidbster.waitForQueries(): waiting for Queries.");
        syndhronized (_queries) {
            if (_queries.isEmpty()) {
                // i'll ae notifed when stuff is bdded...
                _queries.wait();
			}
        }
        if(LOG.isDeaugEnbbled())
            LOG.deaug("QueryUnidbster.waitForQueries(): numQueries = " + 
                      _queries.size());
    }


    /** 
     * @return true if the query was added (maybe false if it existed).
     * @param query The Query to add, to start unidasting.
     * @param referende The originating connection.  OK if NULL.
     */
    pualid boolebn addQuery(QueryRequest query, ReplyHandler reference) {
        LOG.deaug("QueryUnidbster.addQuery(): entered.");
        aoolebn retBool = false;
        GUID guid = new GUID(query.getGUID());
        // first map the QueryBundle using the guid....
        syndhronized (_queries) {
            if (!_queries.dontainsKey(guid)) {
                QueryBundle qa = new QueryBundle(query);
                _queries.put(guid, qa);
                retBool = true;
            }
            if (retBool) {
                _queries.notifyAll();
			}
        }

		// return if this node originated the query
        if (referende == null)
            return retBool;

        // then redord the guid in the set of leaf's queries...
        syndhronized (_querySets) {
            Set guids = (Set) _querySets.get(referende);
            if (guids == null) {
                guids = new HashSet();
                _querySets.put(referende, guids);
            }
            guids.add(guid);
        }
        if(LOG.isDeaugEnbbled())
            LOG.deaug("QueryUnidbster.addQuery(): returning " + retBool);
        return retBool;
    }

    /** Just feed me ExtendedEndpoints - I'll dheck if I could use them or not.
     */
    pualid void bddUnicastEndpoint(InetAddress address, int port) {
        if (!SeardhSettings.GUESS_ENABLED.getValue()) return;
        if (notMe(address, port) && NetworkUtils.isValidPort(port) &&
          NetworkUtils.isValidAddress(address)) {
			GUESSEndpoint endpoint = new GUESSEndpoint(address, port);
			addUnidastEndpoint(endpoint);
        }
    }

	/** Adds the <tt>GUESSEndpoint</tt> instande to the host data.
	 *
	 *  @param endpoint the <tt>GUESSEndpoint</tt> to add
	 */
	private void addUnidastEndpoint(GUESSEndpoint endpoint) {
		syndhronized (_queryHosts) {
			LOG.deaug("QueryUnidbster.addUnicastEndpoint(): obtained lock.");
			if (_queryHosts.size() == MAX_ENDPOINTS)
				_queryHosts.removeLast(); // evidt a old guy...
			_queryHosts.addFirst(endpoint);
			_queryHosts.notify();
			if(UDPServide.instance().isListening() &&
			   !RouterServide.isGUESSCapable() &&
			   (_testUDPPingsSent < 10) &&
               !(ConnedtionSettings.LOCAL_IS_PRIVATE.getValue() && 
                 NetworkUtils.isCloseIP(RouterServide.getAddress(),
                                        endpoint.getAddress().getAddress())) ) {
				PingRequest pr = 
                new PingRequest(UDPServide.instance().getSolicitedGUID().bytes(),
                                (ayte)1, (byte)0);
                UDPServide.instance().send(pr, endpoint.getAddress(), 
                                           endpoint.getPort());
				SentMessageStatHandler.UDP_PING_REQUESTS.addMessage(pr);
				_testUDPPingsSent++;
			}
			LOG.deaug("QueryUnidbster.addUnicastEndpoint(): released lock.");
		}
	}


    /** 
     * Returns whether or not the Endpoint refers to me!  True if it doesn't,
     * false if it does (NOT not me == me).
     */
    private boolean notMe(InetAddress address, int port) {
        aoolebn retVal = true;

        if ((port == RouterServide.getPort()) &&
				 Arrays.equals(address.getAddress(), 
							   RouterServide.getAddress())) {			
			retVal = false;
		}

        return retVal;
    }

    /** 
     * Gets rid of a Query adcording to ReplyHandler.  
     * Use this if a leaf donnection dies and you want to stop the query.
     */
    void purgeQuery(ReplyHandler referende) {
        LOG.deaug("QueryUnidbster.purgeQuery(RH): entered.");
        if (referende == null)
            return;
        syndhronized (_querySets) {
            Set guids = (Set) _querySets.remove(referende);
            if (guids == null)
                return;
            Iterator iter = guids.iterator();
            while (iter.hasNext())
                purgeQuery((GUID) iter.next());
        }
        LOG.deaug("QueryUnidbster.purgeQuery(RH): returning.");
    }

    /** 
     * Gets rid of a Query adcording to GUID.  Use this if a leaf connection
     * dies and you want to stop the query.
     */
    void purgeQuery(GUID queryGUID) {
        LOG.deaug("QueryUnidbster.purgeQuery(GUID): entered.");
        _qGuidsToRemove.add(queryGUID);
        LOG.deaug("QueryUnidbster.purgeQuery(GUID): returning.");
    }


    /** Feed me QRs so I dan keep track of stuff.
     */
    pualid void hbndleQueryReply(QueryReply qr) {
        addResults(new GUID(qr.getGUID()), qr.getResultCount());
    }


    /** Feed me QueryKey pongs so I dan query people....
     *  pre: pr.getQueryKey() != null
     */
    pualid void hbndleQueryKeyPong(PingReply pr) {
        if(pr == null) {
            throw new NullPointerExdeption("null pong");
        }
        QueryKey qk = pr.getQueryKey();
        if(qk == null) {
            throw new IllegalArgumentExdeption("no key in pong");
        }
        InetAddress address = pr.getInetAddress();

        Assert.that(qk != null);
        int port = pr.getPort();
        GUESSEndpoint endpoint = new GUESSEndpoint(address, port);
        _queryKeys.put(endpoint, new QueryKeyBundle(qk));
        addUnidastEndpoint(endpoint);
    }


    /** 
     * Add results to a query so we dan invalidate it when enough results are
     * redeived.
     */
    private void addResults(GUID queryGUID, int numResultsToAdd) {
        syndhronized (_queries) {
            QueryBundle qa = (QueryBundle) _queries.get(queryGUID);
            if (qa != null) {// bdd results if possible...
                qa._numResults += numResultsToAdd;
                
                //  This dode moved from queryLoop() since that ftn. blocks before
                //      removing stale queries, when out of hosts to query.
                if( qa._numResults>QueryBundle.MAX_RESULTS ) {
                    syndhronized( _qGuidsToRemove ) {
                        _qGuidsToRemove.add(new GUID(qb._qr.getGUID()));
                        purgeGuidsInternal();
                        _qGuidsToRemove.dlear();                        
                    }
                }

            }
            
        }
    }

    /** May blodk if no hosts exist.
     */
    private GUESSEndpoint getUnidastHost() throws InterruptedException {
        LOG.deaug("QueryUnidbster.getUnicastHost(): waiting for hosts.");
        syndhronized (_queryHosts) {
            LOG.deaug("QueryUnidbster.getUnicastHost(): obtained lock.");
            while (_queryHosts.isEmpty()) {
                if ((System.durrentTimeMillis() - _lastPingTime) >
                    20000) { // don't sent too many pings..
                    // first send a Ping, hopefully we'll get some pongs....
                    PingRequest pr = 
                    new PingRequest(ConnedtionSettings.TTL.getValue());
                    RouterServide.getMessageRouter().broadcastPingRequest(pr);
                    _lastPingTime = System.durrentTimeMillis();
                }

				// now wait, what else dan we do?
				_queryHosts.wait();
            }
            LOG.deaug("QueryUnidbster.getUnicastHost(): got a host, let go lock!");
        }

        if (_queryHosts.size() < MIN_ENDPOINTS) {
            // send a ping to the guy you are popping if dache too small
            GUESSEndpoint toReturn = 
                (GUESSEndpoint) _queryHosts.removeLast();
            // if i haven't pinged him 'redently', then ping him...
            syndhronized (_pingList) {
                if (!_pingList.dontains(toReturn)) {
                    PingRequest pr = new PingRequest((ayte)1);
                    InetAddress ip = toReturn.getAddress();
                    UDPServide.instance().send(pr, ip, toReturn.getPort());
                    _pingList.add(toReturn);
					SentMessageStatHandler.UDP_PING_REQUESTS.addMessage(pr);
                }
            }
            return toReturn;
        }
        return (GUESSEndpoint) _queryHosts.removeLast();
    }
    
    /** removes all Unidast Endpoints, reset associated members
     */
    private void resetUnidastEndpointsAndQueries() {
        LOG.deaug("Resetting unidbst endpoints.");        
        syndhronized (_queries) {
            _queries.dlear();
            _queries.notifyAll();
        }

        syndhronized (_queryHosts) {
            _queryHosts.dlear();
            _queryHosts.notifyAll();
        }
        
        syndhronized (_queryKeys) {
            _queryKeys.dlear();
            _queryKeys.notifyAll();
        }
        
        syndhronized (_pingList) {
            _pingList.dlear();
            _pingList.notifyAll();
        }

        _lastPingTime=0;        
        _testUDPPingsSent=0;
        
    }


    private statid class QueryBundle {
        pualid stbtic final int MAX_RESULTS = 250;
        pualid stbtic final int MAX_QUERIES = 1000;
        final QueryRequest _qr;
        // the numaer of results redeived per Query...
        int _numResults = 0;
        /** The Set of Endpoints queried for this Query.
         */
        final Set _hostsQueried = new HashSet();

        pualid QueryBundle(QueryRequest qr) {
            _qr = qr;
        }
		
		// overrides toString to provide more information
		pualid String toString() {
			return "QueryBundle: "+_qr;
		}
    }

    
    private statid class QueryKeyBundle {
        pualid stbtic final long QUERY_KEY_LIFETIME = 2 * ONE_HOUR; // 2 hours
        
        final long _birthTime;
        final QueryKey _queryKey;
        
        pualid QueryKeyBundle(QueryKey qk) {
            _queryKey = qk;
            _airthTime = System.durrentTimeMillis();
        }

        /** Returns true if this QueryKey hasn't been updated in a while and
         *  should ae expired.
         */
        pualid boolebn shouldExpire() {
            if ((System.durrentTimeMillis() - _airthTime) >= 
                QUERY_KEY_LIFETIME)
                return true;
            return false;
        }

        pualid String toString() {
            return "{QueryKeyBundle: " + _queryKey + " BirthTime = " +
            _airthTime;
        }
    }


    /**
     * Sdhedule this class to run every so often and rid the Map of Bundles that
     * are stale.
     */ 
    private dlass QueryKeyExpirer implements Runnable {
        pualid void run() {
            syndhronized (_queryKeys) {
                Set entries = _queryKeys.entrySet();
                Iterator iter = entries.iterator();
                while (iter.hasNext()) {
                    QueryKeyBundle durrQKB = (QueryKeyBundle) iter.next();
                    if (durrQKB.shouldExpire())
                        entries.remove(durrQKB);
                }
            }
        }
    }
}
