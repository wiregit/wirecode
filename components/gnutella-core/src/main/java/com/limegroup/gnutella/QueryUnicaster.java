package com.limegroup.gnutella;

import com.limegroup.gnutella.guess.*;
import com.limegroup.gnutella.statistics.*;
import com.sun.java.util.collections.*;
import java.net.*;
import java.util.Stack;

/** 
 * This class runs a single thread which sends unicast UDP queries to a master
 * list of unicast-enabled hosts every n milliseconds.  It interacts with
 * HostCatcher to find unicast-enabled hosts.  It also allows for stopping of
 * individual queries by reply counts.
 */ 
public final class QueryUnicaster {

    /** The time in between successive unicast queries.
     */
    public static final int ITERATION_TIME = 100; // 1/10th of a second...

    /** The number of Endpoints where you should start sending pings to them.
     */
    public static final int MIN_ENDPOINTS = 50;

    /** The max number of unicast pongs to store.
     */
    public static final int MAX_ENDPOINTS = 2000;

    // the instance of me....
    private static QueryUnicaster _instance = null;

    /** Actually sends any QRs via unicast UDP messages.
     */
    private Thread _querier = null;

    // should the _querier be running?
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

    /** The fixed size list of endpoints i've pinged.
     */
    private FixedSizeList _pingList;

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

    static {
        _instance = new QueryUnicaster();
    }

    public static QueryUnicaster instance() {
        return _instance;
    }


    //----------------------------------------------------
    // These methods are used by the QueryUnicasterTester.
    // That is why they are package level.  In general
    // they should not be used by others, though it is
    // technically OK

    /** Returns the number of Queries unicasted by this guy...
     */
    int getQueryNumber() {
        return _queries.size();
    }


    //----------------------------------------------------


    /** 
     * Returns a List of unicast Endpoints.  These Endpoints are the NEWEST 
     * we've seen.
     */
    public List getUnicastEndpoints() {
        List retList = new ArrayList();
        synchronized (_queryHosts) {
            debug("QueryUnicaster.getUnicastEndpoints(): obtained lock.");
            int size = _queryHosts.size();
            if (size > 0) {
                int max = (size > 10 ? 10 : size);
                for (int i = 0; i < max; i++)
                    retList.add(_queryHosts.get(i));
            }
            debug("QueryUnicaster.getUnicastEndpoints(): releasing lock.");
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
	public GUESSEndpoint getUnicastEndpoint() {
		synchronized(_queryHosts) {
			if(_queryHosts.isEmpty()) return null;
			return (GUESSEndpoint)_queryHosts.getFirst();
		}
	}


    private QueryUnicaster() {
        // construct DSes...
        _queries = new Hashtable();
        _queryHosts = new LinkedList();
        _pingList = new FixedSizeList(25);
        _querySets = new Hashtable();
        _qGuidsToRemove = new Vector();

        // start service...
        _querier = new Thread() {
			public void run() {
				queryLoop();
			}
		};
        // only if settings says i can....
        if (SettingsManager.instance().getGuessEnabled())
            _querier.start();
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
                purgeGuidsInternal(); // in case any were added while asleep

                synchronized (_queries) {
                    Iterator iter = _queries.values().iterator();
                    while (iter.hasNext()) {
                        QueryBundle currQB = (QueryBundle)iter.next();
                        if ((currQB._numResults > QueryBundle.MAX_RESULTS) ||
                            (currQB._hostsQueried.size() > 
                             QueryBundle.MAX_QUERIES)
                            )
                            // query is now stale....
                            _qGuidsToRemove.add(new GUID(currQB._qr.getGUID()));
                        else if (currQB._hostsQueried.contains(toQuery))
                            ; // don't send another....
                        else {
							InetAddress ip = toQuery.getAddress();
							debug("QueryUnicaster.queryLoop(): sending" +
								  " query " + currQB._qr.getQuery());
							udpService.send(currQB._qr, ip, 
											toQuery.getPort());
							SentMessageStatHandler.UDP_QUERY_REQUESTS.
                                addMessage(currQB._qr);
							currQB._hostsQueried.add(toQuery);
                        }
                    }
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
        debug("QueryUnicaster.waitForQueries(): waiting for Queries.");
        synchronized (_queries) {
            if (_queries.isEmpty())
                // i'll be notifed when stuff is added...
                _queries.wait();
        }
        debug("QueryUnicaster.waitForQueries(): numQueries = " + 
              _queries.size());
    }


    /** 
     * @return true if the query was added (maybe false if it existed).
     * @param query The Query to add, to start unicasting.
     * @param reference The originating connection.  OK if NULL.
     */
    public boolean addQuery(QueryRequest query, ReplyHandler reference) {
        debug("QueryUnicaster.addQuery(): entered.");
        boolean retBool = false;
        GUID guid = new GUID(query.getGUID());
        // first map the QueryBundle using the guid....
        synchronized (_queries) {
            if (!_queries.containsKey(guid)) {
                QueryBundle qb = new QueryBundle(query);
                _queries.put(guid, qb);
                retBool = true;
            }
            if (retBool)
                _queries.notify();
        }
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
        debug("QueryUnicaster.addQuery(): returning " + retBool);
        return retBool;
    }

    /** Just feed me ExtendedEndpoints - I'll check if I could use them or not.
     */
    public void addUnicastEndpoint(InetAddress address, int port) {
        if (!SettingsManager.instance().getGuessEnabled()) return;
        if (notMe(address, port)) {
            synchronized (_queryHosts) {
                debug("QueryUnicaster.addUnicastEndpoint(): obtained lock.");
                if (_queryHosts.size() == MAX_ENDPOINTS)
                    _queryHosts.removeLast(); // evict a old guy...
                _queryHosts.addFirst(new GUESSEndpoint(address, port));
                _queryHosts.notify();
				if(UDPService.instance().isListening() &&
				   !RouterService.isGUESSCapable() &&
				   _testUDPPingsSent < 5) {
					PingRequest pr = new PingRequest((byte)1);
					UDPService.instance().send(pr, address, port);
					SentMessageStatHandler.UDP_PING_REQUESTS.addMessage(pr);
					_testUDPPingsSent++;
				}
                debug("QueryUnicaster.addUnicastEndpoint(): released lock.");
            }
        }
    }

    /** 
     * Returns whether or not the Endpoint refers to me!  True if it doesn't,
     * false if it does (NOT not me == me).
     */
    private boolean notMe(InetAddress address, int port) {
        boolean retVal = true;

        SettingsManager sm = SettingsManager.instance();
        if (sm.getForceIPAddress()) {
            if (port == sm.getForcedPort() &&
				address.getHostAddress().equals(sm.getForcedIPAddressString()))
				retVal = false;
        }
        else if ((port == RouterService.getPort()) &&
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
        debug("QueryUnicaster.purgeQuery(RH): entered.");
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
        debug("QueryUnicaster.purgeQuery(RH): returning.");
    }

    /** 
     * Gets rid of a Query according to GUID.  Use this if a leaf connection
     * dies and you want to stop the query.
     */
    void purgeQuery(GUID queryGUID) {
        debug("QueryUnicaster.purgeQuery(GUID): entered.");
        _qGuidsToRemove.add(queryGUID);
        debug("QueryUnicaster.purgeQuery(GUID): returning.");
    }


    /** Feed me QRs so I can keep track of stuff.
     */
    public void handleQueryReply(QueryReply qr) {
        addResults(new GUID(qr.getGUID()), qr.getResultCount());
    }


    /** 
     * Add results to a query so we can invalidate it when enough results are
     * received.
     */
    private void addResults(GUID queryGUID, int numResultsToAdd) {
        synchronized (_queries) {
            QueryBundle qb = (QueryBundle) _queries.get(queryGUID);
            if (qb != null) // add results if possible...
                qb._numResults += numResultsToAdd;
        }
    }

    /** May block if no hosts exist.
     */
    private GUESSEndpoint getUnicastHost() throws InterruptedException {
        debug("QueryUnicaster.getUnicastHost(): waiting for hosts.");
        synchronized (_queryHosts) {
            debug("QueryUnicaster.getUnicastHost(): obtained lock.");
            if (_queryHosts.isEmpty()) {
                if ((System.currentTimeMillis() - _lastPingTime) >
                    20000) { // don't sent too many pings..
                    // first send a Ping, hopefully we'll get some pongs....
                    PingRequest pr = 
                    new PingRequest(SettingsManager.instance().getTTL());
                    RouterService.getMessageRouter().broadcastPingRequest(pr);
                    _lastPingTime = System.currentTimeMillis();
                }
                // now wait, what else can we do?
                _queryHosts.wait();
            }
            debug("QueryUnicaster.getUnicastHost(): got a host, let go lock!");
        }

        if (_queryHosts.size() < MIN_ENDPOINTS) {
            // send a ping to the guy you are popping if cache too small
            GUESSEndpoint toReturn = 
                (GUESSEndpoint) _queryHosts.removeLast();
            // if i haven't pinged him 'recently', then ping him...
            if (_pingList.add(toReturn)) {  
                PingRequest pr = new PingRequest((byte)1);
                UDPService udpService = UDPService.instance();
				InetAddress ip = toReturn.getAddress();				
				UDPService.instance().send(pr, ip, toReturn.getPort());
				SentMessageStatHandler.UDP_PING_REQUESTS.addMessage(pr);
            }
            return toReturn;
        }
        return (GUESSEndpoint) _queryHosts.removeLast();
    }


    private class QueryBundle {
        public static final int MAX_RESULTS = 250;
        public static final int MAX_QUERIES = 1000;
        QueryRequest _qr = null;
        // the number of results received per Query...
        int _numResults = 0;
        /** The Set of Endpoints queried for this Query.
         */
        final Set _hostsQueried = new HashSet();

        public QueryBundle(QueryRequest qr) {
            _qr = qr;
        }

        public boolean equals(Object other) {
            boolean retVal = false;
            if (other instanceof QueryBundle)
                retVal = _qr.equals(((QueryBundle)other)._qr);
            return retVal;
        }
    }


    /** 
     * Temporary DS that keeps holds Objects and evicts the oldest entry when
     * the threshold is reached.
     * Handles all synchronization.
     */ 
    private class FixedSizeList {
        
        private int _threshold = 1;
        private List _objects = new ArrayList();

        /** Minimum threshold is 1.
         */
        public FixedSizeList(int threshold) {
            if (threshold > 0)
                _threshold = threshold;
        }

        /* @return if the badboy o was added.
         */
        public synchronized boolean add(GUESSEndpoint o) {
            boolean hasObject = _objects.contains(o);
            if (!hasObject) {
                if (_objects.size() >= _threshold)
                    _objects.remove(0);
                _objects.add(o);
            }
            return !hasObject;
        }
    }


    private final static boolean debugOn = false;
    private final static void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private final static void debug(Exception out) {
        if (debugOn)
            out.printStackTrace();
    }

}
