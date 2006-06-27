package com.limegroup.gnutella.guess;

import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;

/** Utility class for sending GUESS queries.
 */
public class OnDemandUnicaster {

    private static final int CLEAR_TIME = 5 * 60 * 1000; // 5 minutes

    /** GUESSEndpoints => QueryKey.
     */
    private static final Map<GUESSEndpoint, QueryKey> _queryKeys;

    /** Access to UDP traffic.
     */
    private static final UDPService _udp;
    
    /** Short term store for queries waiting for query keys.
     *  GUESSEndpoints => URNs
     */
    private static final Map<GUESSEndpoint, SendLaterBundle> _bufferedURNs;

    static {
        // static initializers are only called once, right?
        _queryKeys = new Hashtable<GUESSEndpoint, QueryKey>(); // need sychronization
        _bufferedURNs = new Hashtable<GUESSEndpoint, SendLaterBundle>(); // synchronization handy
        _udp = UDPService.instance();
        // schedule a runner to clear various data structures
        RouterService.schedule(new Expirer(), CLEAR_TIME, CLEAR_TIME);
     }        

    /** Feed me QueryKey pongs so I can query people....
     *  pre: pr.getQueryKey() != null
     */
    public static void handleQueryKeyPong(PingReply pr) 
        throws NullPointerException, IllegalArgumentException {


        // validity checks
        // ------
        if (pr == null)
            throw new NullPointerException("null pong");

        QueryKey qk = pr.getQueryKey();
        if (qk == null)
            throw new IllegalArgumentException("no key in pong");
        // ------

        // create guess endpoint
        // ------
        InetAddress address = pr.getInetAddress();
        int port = pr.getPort();
        GUESSEndpoint endpoint = new GUESSEndpoint(address, port);
        // ------

        // store query key
        _queryKeys.put(endpoint, qk);

        // if a buffered query exists, send it...
        // -----
        SendLaterBundle bundle = _bufferedURNs.remove(endpoint);
        if (bundle != null) {
            QueryRequest query = 
                QueryRequest.createQueryKeyQuery(bundle._queryURN, qk);
            RouterService.getMessageRouter().originateQueryGUID(query.getGUID());  
            _udp.send(query, endpoint.getAddress(), 
                      endpoint.getPort());
        }
        // -----
    }

    /** Sends out a UDP query with the specified URN to the specified host.
     *  @throws IllegalArgumentException if ep or queryURN are null.
     *  @param ep the location you want to query.
     *  @param queryURN the URN you are querying for.
     */
    public static void query(GUESSEndpoint ep, URN queryURN) 
        throws IllegalArgumentException {

        // validity checks
        // ------
        if (ep == null)
            throw new IllegalArgumentException("No Endpoint!");
        if (queryURN == null)
            throw new IllegalArgumentException("No urn to look for!");
        // ------

        // see if you have a QueryKey - if not, request one
        // ------
        QueryKey key = _queryKeys.get(ep);
        if (key == null) {
            GUESSEndpoint endpoint = new GUESSEndpoint(ep.getAddress(),
                                                       ep.getPort());
            SendLaterBundle bundle = new SendLaterBundle(queryURN);
            _bufferedURNs.put(endpoint, bundle);
            PingRequest pr = PingRequest.createQueryKeyRequest();
            _udp.send(pr, ep.getAddress(), ep.getPort());
        }
        // ------
        // if possible send query, else buffer
        // ------
        else {
            QueryRequest query = QueryRequest.createQueryKeyQuery(queryURN, key);
            RouterService.getMessageRouter().originateQueryGUID(query.getGUID());  
            _udp.send(query, ep.getAddress(), ep.getPort());
        }
        // ------
    }


    private static class SendLaterBundle {

        private static final int MAX_LIFETIME = 60 * 1000;

        public final URN _queryURN;
        private final long _creationTime;

        public SendLaterBundle(URN urn) {
            _queryURN = urn;
            _creationTime = System.currentTimeMillis();
        }
                               
        public boolean shouldExpire() {
            return ((System.currentTimeMillis() - _creationTime) >
                    MAX_LIFETIME);
        }
    }

    /** @return true if the Query Key data structure was cleared.
     *  @param lastQueryKeyClearTime The last time query keys were cleared.
     *  @param queryKeyClearInterval how often you like query keys to be
     *  cleared.
     *  This method has been disaggregated from the Expirer class for ease of
     *  testing.
     */ 
    private static boolean clearDataStructures(long lastQueryKeyClearTime,
                                               long queryKeyClearInterval) 
        throws Throwable {

        boolean clearedQueryKeys = false;

        // Clear the QueryKeys if needed
        // ------
        if ((System.currentTimeMillis() - lastQueryKeyClearTime) >
            queryKeyClearInterval) {
            clearedQueryKeys = true;
            // we just indiscriminately clear all the query keys - we
            // could just expire 'old' ones, but the benefit is marginal
            _queryKeys.clear();
        }
        // ------

        // Get rid of all the buffered URNs that should be expired
        // ------
        synchronized (_bufferedURNs) {
            for(Iterator<SendLaterBundle> iter =  _bufferedURNs.values().iterator(); iter.hasNext(); ) {
                SendLaterBundle bundle = iter.next();
                if (bundle.shouldExpire())
                    iter.remove();
            }
        }
        // ------

        return clearedQueryKeys;
    }


    /** This is run to clear various data structures used.
     *  Made package access for easy test access.
     */
    private static class Expirer implements Runnable {

        // 24 hours
        private static final int QUERY_KEY_CLEAR_TIME = 24 * 60 * 60 * 1000;

        private long _lastQueryKeyClearTime;

        public Expirer() {
            _lastQueryKeyClearTime = System.currentTimeMillis();
        }

        public void run() {
            try {
                if (clearDataStructures(_lastQueryKeyClearTime, 
                                        QUERY_KEY_CLEAR_TIME))
                    _lastQueryKeyClearTime = System.currentTimeMillis();
            } 
            catch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }

}
