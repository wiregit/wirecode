package com.limegroup.gnutella;

import com.sun.java.util.collections.*;
import java.util.Stack;

/** This class runs a single thread which sends unicast UDP queries to a master
 * list of unicast-enabled hosts every n milliseconds.  It interacts with
 * HostCatcher to find unicast-enabled hosts.  It also allows for stopping of
 * individual queries by reply counts.
 */ 
public class QueryUnicaster {

    /** The time in between successive unicast queries.
     */
    public static final int ITERATION_TIME = 100; // 1/10th of a second...

    // the instance of me....
    private static QueryUnicaster _instance = null;

    /** Actually sends any QRs via unicast UDP messages.
     */
    private Thread _querier = null;

    // should the _querier be running?
    private boolean _shouldRun = true;

    /** The list of Queries I need to send every iteration.
     */
    private Vector _queries;

    /** The unicast enabled hosts I should contact for queries.
     */
    private Stack _queryHosts;

    public static QueryUnicaster instance() {
        if (_instance == null)
            _instance = new QueryUnicaster();
        return _instance;
    }

    protected QueryUnicaster() {
        // construct DSes...
        _queries = new Vector();
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
        QueryBundle qb = new QueryBundle(query);
        synchronized (_queries) {
            if (!_queries.contains(qb)) 
                retBool = _queries.add(qb);
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

    /** May block if no hosts exist.
     */
    private ExtendedEndpoint getUnicastHost() throws InterruptedException {
        synchronized (_queryHosts) {
            if (_queryHosts.isEmpty())
                _queryHosts.wait();
            return (ExtendedEndpoint) _queryHosts.pop();
        }
    }


    private class QueryBundle {
        QueryRequest _qr = null;
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


    private final static boolean debugOn = true;
    private final static void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private final static void debug(Exception out) {
        if (debugOn)
            out.printStackTrace();
    }

}
