package com.limegroup.gnutella;

import com.sun.java.util.collections.*;
import java.net.*;
import java.util.Stack;

/** This class runs a single thread which sends unicast UDP queries to a master
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

    // the instance of me....
    private static QueryUnicaster _instance = null;

    /** Actually sends any QRs via unicast UDP messages.
     */
    private Thread _querier = null;

    // should the _querier be running?
    private boolean _shouldRun = true;

    /** The map of Queries I need to send every iteration.
     *  The map is from GUID to QueryBundle.  The following invariant is
     *  maintained:
     *  GUID -> QueryBundle where GUID == QueryBundle._qr.getGUID()
     *  Not a Map because I want to enforce synchronization.
     */
    private Hashtable _queries;

    /** The unicast enabled hosts I should contact for queries.
     */
    private Stack _queryHosts;

    public static QueryUnicaster instance() {
        if (_instance == null)
            _instance = new QueryUnicaster();
        return _instance;
    }


    /** Returns a List of unicast Endpoints.
     */
    public List getUnicastEndpoints() {
        List retList = new ArrayList();
        synchronized (_queryHosts) {
            int size = _queryHosts.size();
            if (size > 0) {
                int max = (size > 10 ? 10 : size);
                for (int i = 0; i < max; i++)
                    retList.add(_queryHosts.get(i));
            }
        }
        return retList;
    }


    private QueryUnicaster() {
        // construct DSes...
        _queries = new Hashtable();
        _queryHosts = new Stack();

        // start service...
        _querier = new Thread() {
                public void run() {
                    queryLoop();
                }
            };
        _querier.start();
    }

    /** The main work to be done.
     *  If there are queries, get a unicast enabled UP, and send each Query to
     *  it.  Then sleep and try some more later...
     */
    private void queryLoop() {
        while (_shouldRun) {
            try {
                waitForQueries();
                Endpoint toQuery = getUnicastHost();
                List toRemove = new ArrayList();
                UDPAcceptor udpService = UDPAcceptor.instance();

                synchronized (_queries) {
                    Iterator iter = _queries.entrySet().iterator();
                    while (iter.hasNext()) {
                        QueryBundle currQB = 
                        (QueryBundle) ((Map.Entry)iter.next()).getValue();
                        if ((currQB._numResults > QueryBundle.MAX_RESULTS) ||
                            (currQB._hostsQueried.size() > 
                             QueryBundle.MAX_QUERIES)
                            )
                            toRemove.add(currQB);
                        else if (currQB._hostsQueried.contains(toQuery))
                            ; // don't send another....
                        else {
                            try {
                                InetAddress ip = 
                                InetAddress.getByName(toQuery.getHostname());
                                // send the query
                                debug("QueryUnicaster.queryLoop(): sending" +
                                      " query " + currQB._qr.getQuery());
                                udpService.send(currQB._qr, ip, 
                                                toQuery.getPort());
                                currQB._hostsQueried.add(toQuery);
                            }
                            catch (UnknownHostException ignored) {}
                        }
                    }
                }
                
                // purge stale queries...
                Iterator removee = toRemove.iterator();
                while (removee.hasNext()) {
                    QueryBundle currQB = (QueryBundle) removee.next();
                    _queries.remove(new GUID(currQB._qr.getGUID()));
                }

                Thread.sleep(ITERATION_TIME);
            }
            catch (InterruptedException ignored) {}
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


    /** @return true if the query was added (maybe false if it existed).
     */
    public boolean addQuery(QueryRequest query) {
        debug("QueryUnicaster.addQuery(): entered.");
        boolean retBool = false;
        GUID guid = new GUID(query.getGUID());
        synchronized (_queries) {
            if (!_queries.containsKey(guid)) {
                QueryBundle qb = new QueryBundle(query);
                _queries.put(guid, qb);
                retBool = true;
            }
            if (retBool)
                _queries.notify();
        }
        debug("QueryUnicaster.addQuery(): returning " + retBool);
        return retBool;
    }

    /** Just feed me ExtendedEndpoints - I'll check if I could use them or not.
     */
    public void addUnicastEndpoint(ExtendedEndpoint endpoint) {
        if (endpoint.getUnicastSupport()) {
            synchronized (_queryHosts) {
                _queryHosts.push(endpoint);
                _queryHosts.notify();
            }
        }
    }

    /** Feed me QRs so I can keep track of stuff.
     */
    public void handleQueryReply(QueryReply qr) {
        addResults(new GUID(qr.getGUID()), qr.getResultCount());
    }


    /** Add results to a query so we can invalidate it when enough results are
     *  received.
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
    private Endpoint getUnicastHost() throws InterruptedException {
        debug("QueryUnicaster.getUnicastHost(): waiting for hosts.");
        synchronized (_queryHosts) {

            if (_queryHosts.isEmpty()) {
                // first send a Ping, hopefully we'll get some pongs....
                PingRequest pr = 
                new PingRequest(SettingsManager.instance().getTTL());
                RouterService.getMessageRouter().broadcastPingRequest(pr);
                // now wait, what else can we do?
                _queryHosts.wait();
            }
            debug("QueryUnicaster.getUnicastHost(): got a host!");

            if (_queryHosts.size() < MIN_ENDPOINTS) {
                // send a ping to the guy you are popping if cache too small
                ExtendedEndpoint toReturn = 
                (ExtendedEndpoint) _queryHosts.pop();
                PingRequest pr = new PingRequest((byte)1);
                UDPAcceptor udpService = UDPAcceptor.instance();
                try {
                    InetAddress ip = 
                    InetAddress.getByName(toReturn.getHostname());
                    // send the query
                    udpService.send(pr, ip, toReturn.getPort());
                }
                catch (UnknownHostException ignored) {}
                return toReturn;
            }
            return (ExtendedEndpoint) _queryHosts.pop();
        }
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
