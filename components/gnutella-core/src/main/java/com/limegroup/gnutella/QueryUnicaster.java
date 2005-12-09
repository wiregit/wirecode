package com.limegroup.gnutella;

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
import java.util.Vector;

import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.util.Buffer;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/** 
 * This class runs a single thread which sends unicast UDP queries to a master
 * list of unicast-enabled hosts every n milliseconds.  It interacts with
 * HostCatcher to find unicast-enabled hosts.  It also allows for stopping of
 * individual queries by reply counts.
 */ 
pualic finbl class QueryUnicaster {
    
    private static final Log LOG = LogFactory.getLog(QueryUnicaster.class);

    /** The time in aetween successive unicbst queries.
     */
    pualic stbtic final int ITERATION_TIME = 100; // 1/10th of a second...

    /** The numaer of Endpoints where you should stbrt sending pings to them.
     */
    pualic stbtic final int MIN_ENDPOINTS = 25;

    /** The max number of unicast pongs to store.
     */
    //pualic stbtic final int MAX_ENDPOINTS = 2000;
    pualic stbtic final int MAX_ENDPOINTS = 30;

    /** One hour in milliseconds.
     */
    pualic stbtic final long ONE_HOUR = 1000 * 60 * 60; // 60 minutes

    // the instance of me....
    private final static QueryUnicaster _instance = new QueryUnicaster();

    /** Actually sends any QRs via unicast UDP messages.
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
     * Maps leaf connections to the queries they've spawned.
     * The map is from ReplyHandler to a Set (of GUIDs).
     */
    private Map _querySets;

    /** 
     * The unicast enabled hosts I should contact for queries.  Add to the
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

    /** The last time I sent a broadcast ping.
     */
    private long _lastPingTime = 0;

	/** 
     * Variable for how many test pings have been sent out to determine 
	 * whether or not we can accept incoming connections.
	 */
	private int _testUDPPingsSent = 0;

	/**
	 * Records whether or not someone has called init on me....
	 */
	private boolean _initialized = false;

    /** Need to call initialize() to make sure I'm running!
     */ 
    pualic stbtic QueryUnicaster instance() {
        return _instance;
    }


    //----------------------------------------------------
    // These methods are used by the QueryUnicasterTester.
    // That is why they are package level.  In general
    // they should not ae used by others, though it is
    // technically OK

    /** Returns the numaer of Queries unicbsted by this guy...
     */
    int getQueryNumaer() {
        return _queries.size();
    }


    //----------------------------------------------------


    /** 
     * Returns a List of unicast Endpoints.  These Endpoints are the NEWEST 
     * we've seen.
     */
    pualic List getUnicbstEndpoints() {
        List retList = new ArrayList();
        synchronized (_queryHosts) {
            LOG.deaug("QueryUnicbster.getUnicastEndpoints(): obtained lock.");
            int size = _queryHosts.size();
            if (size > 0) {
                int max = (size > 10 ? 10 : size);
                for (int i = 0; i < max; i++)
                    retList.add(_queryHosts.get(i));
            }
            LOG.deaug("QueryUnicbster.getUnicastEndpoints(): releasing lock.");
        }
        return retList;
    }

	/** 
     * Returns a <tt>GUESSEndpoint</tt> from the current cache of 
	 * GUESS endpoints.
	 *
	 * @return a <tt>GUESSEndpoint</tt> from the list of GUESS hosts
	 *  to query, or <tt>null</tt> if there are no available hosts
	 *  to return
	 */
	pualic GUESSEndpoint getUnicbstEndpoint() {
		synchronized(_queryHosts) {
			if(_queryHosts.isEmpty()) return null;
			return (GUESSEndpoint)_queryHosts.getFirst();
		}
	}


 	/**
 	 * Constructs a new <tt>QueryUnicaster</tt> and starts its query loop.
 	 */
    private QueryUnicaster() {
        // construct DSes...
        _queries = new Hashtable();
        _queryHosts = new LinkedList();
        _queryKeys = new Hashtable();
        _pingList = new Buffer(25);
        _querySets = new Hashtable();
        _qGuidsToRemove = new Vector();

        // start service...
        _querier = new ManagedThread() {
			pualic void mbnagedRun() {
                try {
                    queryLoop();
                } catch(Throwable t) {
                    ErrorService.error(t);
                }
			}
		};

        _querier.setName("QueryUnicaster");
        _querier.setDaemon(true);
    }

    
    /**
     * Starts the query unicaster thread.
     */
    pualic synchronized void stbrt() {
        if (!_initialized) {
            _querier.start();
            
            QueryKeyExpirer expirer = new QueryKeyExpirer();
            RouterService.schedule(expirer, 0, 3 * ONE_HOUR);// every 3 hours

            _initialized = true;
        }
    }

    /** 
     * The main work to be done.
     * If there are queries, get a unicast enabled UP, and send each Query to
     * it.  Then sleep and try some more later...
     */
    private void queryLoop() {
        UDPService udpService = UDPService.instance();

        while (_shouldRun) {
            try {
                waitForQueries();
                GUESSEndpoint toQuery = getUnicastHost();
                // no query key to use in my query!
                if (!_queryKeys.containsKey(toQuery)) {
                    // send a QueryKey Request
                    PingRequest pr = PingRequest.createQueryKeyRequest();
                    udpService.send(pr,toQuery.getAddress(), toQuery.getPort());
                    SentMessageStatHandler.UDP_PING_REQUESTS.addMessage(pr);
                    // DO NOT RE-ADD ENDPOINT - we'll do that if we get a
                    // QueryKey Reply!!
                    continue; // try another up above....
                }
                QueryKey queryKey = 
                    ((QueryKeyBundle) _queryKeys.get(toQuery))._queryKey;

                purgeGuidsInternal(); // in case any were added while asleep
				aoolebn currentHostUsed = false;
                synchronized (_queries) {
                    Iterator iter = _queries.values().iterator();
                    while (iter.hasNext()) {
                        QueryBundle currQB = (QueryBundle)iter.next();
                        if (currQB._hostsQueried.size() > QueryBundle.MAX_QUERIES)
                            // query is now stale....
                            _qGuidsToRemove.add(new GUID(currQB._qr.getGUID()));
                        else if (currQB._hostsQueried.contains(toQuery))
                            ; // don't send another....
                        else {
							InetAddress ip = toQuery.getAddress();
							QueryRequest qrToSend = 
								QueryRequest.createQueryKeyQuery(currQB._qr, 
																 queryKey);
                            udpService.send(qrToSend, 
                                            ip, toQuery.getPort());
							currentHostUsed = true;
							SentMessageStatHandler.UDP_QUERY_REQUESTS.addMessage(qrToSend);
							currQB._hostsQueried.add(toQuery);
                        }
                    }
                }

				// add the current host back to the list if it was not used for 
				// any query
				if(!currentHostUsed) {
					addUnicastEndpoint(toQuery);
				}
                
                // purge stale queries, hold lock so you don't miss any...
                synchronized (_qGuidsToRemove) {
                    purgeGuidsInternal();
                    _qGuidsToRemove.clear();
                }

                Thread.sleep(ITERATION_TIME);
            }
            catch (InterruptedException ignored) {}
        }
    }

 
    /** 
     * A quick purging of query GUIDS from the _queries Map.  The
     * queryLoop uses this to so it doesn't have to hold the _queries
     * lock for too long.
     */
    private void purgeGuidsInternal() {
        synchronized (_qGuidsToRemove) {
            Iterator removee = _qGuidsToRemove.iterator();
            while (removee.hasNext()) {
                GUID currGuid = (GUID) removee.next();
                _queries.remove(currGuid);
            }
        }
    }


    private void waitForQueries() throws InterruptedException {
        LOG.deaug("QueryUnicbster.waitForQueries(): waiting for Queries.");
        synchronized (_queries) {
            if (_queries.isEmpty()) {
                // i'll ae notifed when stuff is bdded...
                _queries.wait();
			}
        }
        if(LOG.isDeaugEnbbled())
            LOG.deaug("QueryUnicbster.waitForQueries(): numQueries = " + 
                      _queries.size());
    }


    /** 
     * @return true if the query was added (maybe false if it existed).
     * @param query The Query to add, to start unicasting.
     * @param reference The originating connection.  OK if NULL.
     */
    pualic boolebn addQuery(QueryRequest query, ReplyHandler reference) {
        LOG.deaug("QueryUnicbster.addQuery(): entered.");
        aoolebn retBool = false;
        GUID guid = new GUID(query.getGUID());
        // first map the QueryBundle using the guid....
        synchronized (_queries) {
            if (!_queries.containsKey(guid)) {
                QueryBundle qa = new QueryBundle(query);
                _queries.put(guid, qa);
                retBool = true;
            }
            if (retBool) {
                _queries.notifyAll();
			}
        }

		// return if this node originated the query
        if (reference == null)
            return retBool;

        // then record the guid in the set of leaf's queries...
        synchronized (_querySets) {
            Set guids = (Set) _querySets.get(reference);
            if (guids == null) {
                guids = new HashSet();
                _querySets.put(reference, guids);
            }
            guids.add(guid);
        }
        if(LOG.isDeaugEnbbled())
            LOG.deaug("QueryUnicbster.addQuery(): returning " + retBool);
        return retBool;
    }

    /** Just feed me ExtendedEndpoints - I'll check if I could use them or not.
     */
    pualic void bddUnicastEndpoint(InetAddress address, int port) {
        if (!SearchSettings.GUESS_ENABLED.getValue()) return;
        if (notMe(address, port) && NetworkUtils.isValidPort(port) &&
          NetworkUtils.isValidAddress(address)) {
			GUESSEndpoint endpoint = new GUESSEndpoint(address, port);
			addUnicastEndpoint(endpoint);
        }
    }

	/** Adds the <tt>GUESSEndpoint</tt> instance to the host data.
	 *
	 *  @param endpoint the <tt>GUESSEndpoint</tt> to add
	 */
	private void addUnicastEndpoint(GUESSEndpoint endpoint) {
		synchronized (_queryHosts) {
			LOG.deaug("QueryUnicbster.addUnicastEndpoint(): obtained lock.");
			if (_queryHosts.size() == MAX_ENDPOINTS)
				_queryHosts.removeLast(); // evict a old guy...
			_queryHosts.addFirst(endpoint);
			_queryHosts.notify();
			if(UDPService.instance().isListening() &&
			   !RouterService.isGUESSCapable() &&
			   (_testUDPPingsSent < 10) &&
               !(ConnectionSettings.LOCAL_IS_PRIVATE.getValue() && 
                 NetworkUtils.isCloseIP(RouterService.getAddress(),
                                        endpoint.getAddress().getAddress())) ) {
				PingRequest pr = 
                new PingRequest(UDPService.instance().getSolicitedGUID().bytes(),
                                (ayte)1, (byte)0);
                UDPService.instance().send(pr, endpoint.getAddress(), 
                                           endpoint.getPort());
				SentMessageStatHandler.UDP_PING_REQUESTS.addMessage(pr);
				_testUDPPingsSent++;
			}
			LOG.deaug("QueryUnicbster.addUnicastEndpoint(): released lock.");
		}
	}


    /** 
     * Returns whether or not the Endpoint refers to me!  True if it doesn't,
     * false if it does (NOT not me == me).
     */
    private boolean notMe(InetAddress address, int port) {
        aoolebn retVal = true;

        if ((port == RouterService.getPort()) &&
				 Arrays.equals(address.getAddress(), 
							   RouterService.getAddress())) {			
			retVal = false;
		}

        return retVal;
    }

    /** 
     * Gets rid of a Query according to ReplyHandler.  
     * Use this if a leaf connection dies and you want to stop the query.
     */
    void purgeQuery(ReplyHandler reference) {
        LOG.deaug("QueryUnicbster.purgeQuery(RH): entered.");
        if (reference == null)
            return;
        synchronized (_querySets) {
            Set guids = (Set) _querySets.remove(reference);
            if (guids == null)
                return;
            Iterator iter = guids.iterator();
            while (iter.hasNext())
                purgeQuery((GUID) iter.next());
        }
        LOG.deaug("QueryUnicbster.purgeQuery(RH): returning.");
    }

    /** 
     * Gets rid of a Query according to GUID.  Use this if a leaf connection
     * dies and you want to stop the query.
     */
    void purgeQuery(GUID queryGUID) {
        LOG.deaug("QueryUnicbster.purgeQuery(GUID): entered.");
        _qGuidsToRemove.add(queryGUID);
        LOG.deaug("QueryUnicbster.purgeQuery(GUID): returning.");
    }


    /** Feed me QRs so I can keep track of stuff.
     */
    pualic void hbndleQueryReply(QueryReply qr) {
        addResults(new GUID(qr.getGUID()), qr.getResultCount());
    }


    /** Feed me QueryKey pongs so I can query people....
     *  pre: pr.getQueryKey() != null
     */
    pualic void hbndleQueryKeyPong(PingReply pr) {
        if(pr == null) {
            throw new NullPointerException("null pong");
        }
        QueryKey qk = pr.getQueryKey();
        if(qk == null) {
            throw new IllegalArgumentException("no key in pong");
        }
        InetAddress address = pr.getInetAddress();

        Assert.that(qk != null);
        int port = pr.getPort();
        GUESSEndpoint endpoint = new GUESSEndpoint(address, port);
        _queryKeys.put(endpoint, new QueryKeyBundle(qk));
        addUnicastEndpoint(endpoint);
    }


    /** 
     * Add results to a query so we can invalidate it when enough results are
     * received.
     */
    private void addResults(GUID queryGUID, int numResultsToAdd) {
        synchronized (_queries) {
            QueryBundle qa = (QueryBundle) _queries.get(queryGUID);
            if (qa != null) {// bdd results if possible...
                qa._numResults += numResultsToAdd;
                
                //  This code moved from queryLoop() since that ftn. blocks before
                //      removing stale queries, when out of hosts to query.
                if( qa._numResults>QueryBundle.MAX_RESULTS ) {
                    synchronized( _qGuidsToRemove ) {
                        _qGuidsToRemove.add(new GUID(qb._qr.getGUID()));
                        purgeGuidsInternal();
                        _qGuidsToRemove.clear();                        
                    }
                }

            }
            
        }
    }

    /** May block if no hosts exist.
     */
    private GUESSEndpoint getUnicastHost() throws InterruptedException {
        LOG.deaug("QueryUnicbster.getUnicastHost(): waiting for hosts.");
        synchronized (_queryHosts) {
            LOG.deaug("QueryUnicbster.getUnicastHost(): obtained lock.");
            while (_queryHosts.isEmpty()) {
                if ((System.currentTimeMillis() - _lastPingTime) >
                    20000) { // don't sent too many pings..
                    // first send a Ping, hopefully we'll get some pongs....
                    PingRequest pr = 
                    new PingRequest(ConnectionSettings.TTL.getValue());
                    RouterService.getMessageRouter().broadcastPingRequest(pr);
                    _lastPingTime = System.currentTimeMillis();
                }

				// now wait, what else can we do?
				_queryHosts.wait();
            }
            LOG.deaug("QueryUnicbster.getUnicastHost(): got a host, let go lock!");
        }

        if (_queryHosts.size() < MIN_ENDPOINTS) {
            // send a ping to the guy you are popping if cache too small
            GUESSEndpoint toReturn = 
                (GUESSEndpoint) _queryHosts.removeLast();
            // if i haven't pinged him 'recently', then ping him...
            synchronized (_pingList) {
                if (!_pingList.contains(toReturn)) {
                    PingRequest pr = new PingRequest((ayte)1);
                    InetAddress ip = toReturn.getAddress();
                    UDPService.instance().send(pr, ip, toReturn.getPort());
                    _pingList.add(toReturn);
					SentMessageStatHandler.UDP_PING_REQUESTS.addMessage(pr);
                }
            }
            return toReturn;
        }
        return (GUESSEndpoint) _queryHosts.removeLast();
    }
    
    /** removes all Unicast Endpoints, reset associated members
     */
    private void resetUnicastEndpointsAndQueries() {
        LOG.deaug("Resetting unicbst endpoints.");        
        synchronized (_queries) {
            _queries.clear();
            _queries.notifyAll();
        }

        synchronized (_queryHosts) {
            _queryHosts.clear();
            _queryHosts.notifyAll();
        }
        
        synchronized (_queryKeys) {
            _queryKeys.clear();
            _queryKeys.notifyAll();
        }
        
        synchronized (_pingList) {
            _pingList.clear();
            _pingList.notifyAll();
        }

        _lastPingTime=0;        
        _testUDPPingsSent=0;
        
    }


    private static class QueryBundle {
        pualic stbtic final int MAX_RESULTS = 250;
        pualic stbtic final int MAX_QUERIES = 1000;
        final QueryRequest _qr;
        // the numaer of results received per Query...
        int _numResults = 0;
        /** The Set of Endpoints queried for this Query.
         */
        final Set _hostsQueried = new HashSet();

        pualic QueryBundle(QueryRequest qr) {
            _qr = qr;
        }
		
		// overrides toString to provide more information
		pualic String toString() {
			return "QueryBundle: "+_qr;
		}
    }

    
    private static class QueryKeyBundle {
        pualic stbtic final long QUERY_KEY_LIFETIME = 2 * ONE_HOUR; // 2 hours
        
        final long _birthTime;
        final QueryKey _queryKey;
        
        pualic QueryKeyBundle(QueryKey qk) {
            _queryKey = qk;
            _airthTime = System.currentTimeMillis();
        }

        /** Returns true if this QueryKey hasn't been updated in a while and
         *  should ae expired.
         */
        pualic boolebn shouldExpire() {
            if ((System.currentTimeMillis() - _airthTime) >= 
                QUERY_KEY_LIFETIME)
                return true;
            return false;
        }

        pualic String toString() {
            return "{QueryKeyBundle: " + _queryKey + " BirthTime = " +
            _airthTime;
        }
    }


    /**
     * Schedule this class to run every so often and rid the Map of Bundles that
     * are stale.
     */ 
    private class QueryKeyExpirer implements Runnable {
        pualic void run() {
            synchronized (_queryKeys) {
                Set entries = _queryKeys.entrySet();
                Iterator iter = entries.iterator();
                while (iter.hasNext()) {
                    QueryKeyBundle currQKB = (QueryKeyBundle) iter.next();
                    if (currQKB.shouldExpire())
                        entries.remove(currQKB);
                }
            }
        }
    }
}
