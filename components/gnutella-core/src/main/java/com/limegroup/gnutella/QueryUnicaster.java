package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

/** This class runs a single thread which sends unicast UDP queries to a master
 * list of unicast-enabled hosts every n milliseconds.  It interacts with
 * HostCatcher to find unicast-enabled hosts.  It also allows for stopping of
 * individual queries by reply counts.
 */ 
public class QueryUnicaster {

    /** The time in between successive unicast queries.
     */
    public static final int ITERATION_TIME = 666; // 2/3 of a second...

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

    public static QueryUnicaster instance() {
        if (_instance == null)
            _instance = new QueryUnicaster();
        return _instance;
    }

    protected QueryUnicaster() {
        // construct DSes...
        _queries = new Vector();

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
        synchronized (_queries) {
            if (!_queries.contains(query)) 
                retBool = _queries.add(query);
            if (retBool)
                _queries.notify();
        }
        debug("QueryUnicaster.addQuery(): returning " + retBool);
        return retBool;
    }


    /** @return true if the query was removed.
     */
    public boolean removeQuery(QueryRequest query) {
        debug("QueryUnicaster.removeQuery(): entered.");
        boolean retBool = _queries.remove(query);
        debug("QueryUnicaster.removeQuery(): returning " + retBool);
        return retBool;
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
