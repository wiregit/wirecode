package com.limegroup.gnutella.guess;

import com.sun.java.util.collections.*;
import java.io.IOException;
import java.net.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;

/** Utility class for sending GUESS queries.
 */
public class OnDemandUnicaster {

    /** GUESSEndpoints => QueryKey.
     */
    private static final Map _queryKeys;

    /** Access to UDP traffic.
     */
    private static final UDPService _udp;
    
    /** Short term store for queries waiting for query keys.
     */
    private static final Map _bufferedQueries;

    static {
        _queryKeys = new Hashtable(); // need sychronization
        _bufferedQueries = new Hashtable(); // synchronization handy
        _udp = UDPService.instance();
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
        InetAddress address = null;
        try {
            address = InetAddress.getByName(pr.getIP());
        }
        catch (UnknownHostException damn) {
            // unknown host exception??  weird - well, don't continue....
            return;
        }
        int port = pr.getPort();
        GUESSEndpoint endpoint = new GUESSEndpoint(address, port);
        // ------

        // store query key
        _queryKeys.put(endpoint, qk);

        // if a buffered query exists, send it...
        // -----
        SendLaterBundle bundle = 
            (SendLaterBundle) _bufferedQueries.remove(endpoint);
        if (bundle != null) {
            QueryRequest query = 
                QueryRequest.createQueryKeyQuery(bundle._queryURN, qk);
            try {
                _udp.send(query, endpoint.getAddress(), 
                          endpoint.getPort());
            }
            catch (IOException ignored) {}
        }
        // -----
    }

    /** Sends out a UDP query with the specified URN to the specified host.
     *  @throws IllegalArgumentException if ep or queryURN are null.
     *  @throws InterruptedException useful for guys who want to be notified.
     *  @param ep the location you want to query.
     *  @param queryURN the URN you are querying for.
     */
    public static void query(GUESSEndpoint ep, URN queryURN) 
        throws IllegalArgumentException, InterruptedException {

        // validity checks
        // ------
        if (ep == null)
            throw new IllegalArgumentException("No Endpoint!");
        if (queryURN == null)
            throw new IllegalArgumentException("No urn to look for!");
        // ------

        // see if you have a QueryKey - if not, request one
        // ------
        QueryKey key = (QueryKey) _queryKeys.get(ep);
        if (key == null) {
            PingRequest pr = PingRequest.createQueryKeyRequest();
            try {
                _udp.send(pr, ep.getAddress(), ep.getPort());
            }
            catch (IOException veryBad) {
                return;
            }
        }
        // ------
        
        // if possible send query, else buffer
        // ------
        if (key != null) {
            QueryRequest query = QueryRequest.createQueryKeyQuery(queryURN, key);
            try {
                _udp.send(query, ep.getAddress(), ep.getPort());
            }
            catch (IOException ignored) {}
        }
        else {
            // don't want to get a query key before buffering the query - this
            // still may happen but it seems HIGHLY unlikely
            synchronized (_bufferedQueries) {
                GUESSEndpoint endpoint = new GUESSEndpoint(ep.getAddress(),
                                                           ep.getPort());
                SendLaterBundle bundle = new SendLaterBundle(queryURN);
                _bufferedQueries.put(endpoint, bundle);
            }
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

}