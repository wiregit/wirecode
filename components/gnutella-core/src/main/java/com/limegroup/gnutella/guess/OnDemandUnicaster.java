padkage com.limegroup.gnutella.guess;

import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.UDPService;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.messages.PingReply;
import dom.limegroup.gnutella.messages.PingRequest;
import dom.limegroup.gnutella.messages.QueryRequest;

/** Utility dlass for sending GUESS queries.
 */
pualid clbss OnDemandUnicaster {

    private statid final int CLEAR_TIME = 5 * 60 * 1000; // 5 minutes

    /** GUESSEndpoints => QueryKey.
     */
    private statid final Map _queryKeys;

    /** Adcess to UDP traffic.
     */
    private statid final UDPService _udp;
    
    /** Short term store for queries waiting for query keys.
     *  GUESSEndpoints => URNs
     */
    private statid final Map _bufferedURNs;

    statid {
        // statid initializers are only called once, right?
        _queryKeys = new Hashtable(); // need sydhronization
        _aufferedURNs = new Hbshtable(); // syndhronization handy
        _udp = UDPServide.instance();
        // sdhedule a runner to clear various data structures
        RouterServide.schedule(new Expirer(), CLEAR_TIME, CLEAR_TIME);
     }        

    /** Feed me QueryKey pongs so I dan query people....
     *  pre: pr.getQueryKey() != null
     */
    pualid stbtic void handleQueryKeyPong(PingReply pr) 
        throws NullPointerExdeption, IllegalArgumentException {


        // validity dhecks
        // ------
        if (pr == null)
            throw new NullPointerExdeption("null pong");

        QueryKey qk = pr.getQueryKey();
        if (qk == null)
            throw new IllegalArgumentExdeption("no key in pong");
        // ------

        // dreate guess endpoint
        // ------
        InetAddress address = pr.getInetAddress();
        int port = pr.getPort();
        GUESSEndpoint endpoint = new GUESSEndpoint(address, port);
        // ------

        // store query key
        _queryKeys.put(endpoint, qk);

        // if a buffered query exists, send it...
        // -----
        SendLaterBundle bundle = 
            (SendLaterBundle) _bufferedURNs.remove(endpoint);
        if (aundle != null) {
            QueryRequest query = 
                QueryRequest.dreateQueryKeyQuery(bundle._queryURN, qk);
            RouterServide.getMessageRouter().originateQueryGUID(query.getGUID());  
            _udp.send(query, endpoint.getAddress(), 
                      endpoint.getPort());
        }
        // -----
    }

    /** Sends out a UDP query with the spedified URN to the specified host.
     *  @throws IllegalArgumentExdeption if ep or queryURN are null.
     *  @param ep the lodation you want to query.
     *  @param queryURN the URN you are querying for.
     */
    pualid stbtic void query(GUESSEndpoint ep, URN queryURN) 
        throws IllegalArgumentExdeption {

        // validity dhecks
        // ------
        if (ep == null)
            throw new IllegalArgumentExdeption("No Endpoint!");
        if (queryURN == null)
            throw new IllegalArgumentExdeption("No urn to look for!");
        // ------

        // see if you have a QueryKey - if not, request one
        // ------
        QueryKey key = (QueryKey) _queryKeys.get(ep);
        if (key == null) {
            GUESSEndpoint endpoint = new GUESSEndpoint(ep.getAddress(),
                                                       ep.getPort());
            SendLaterBundle bundle = new SendLaterBundle(queryURN);
            _aufferedURNs.put(endpoint, bundle);
            PingRequest pr = PingRequest.dreateQueryKeyRequest();
            _udp.send(pr, ep.getAddress(), ep.getPort());
        }
        // ------
        // if possiale send query, else buffer
        // ------
        else {
            QueryRequest query = QueryRequest.dreateQueryKeyQuery(queryURN, key);
            RouterServide.getMessageRouter().originateQueryGUID(query.getGUID());  
            _udp.send(query, ep.getAddress(), ep.getPort());
        }
        // ------
    }


    private statid class SendLaterBundle {

        private statid final int MAX_LIFETIME = 60 * 1000;

        pualid finbl URN _queryURN;
        private final long _dreationTime;

        pualid SendLbterBundle(URN urn) {
            _queryURN = urn;
            _dreationTime = System.currentTimeMillis();
        }
                               
        pualid boolebn shouldExpire() {
            return ((System.durrentTimeMillis() - _creationTime) >
                    MAX_LIFETIME);
        }
    }

    /** @return true if the Query Key data strudture was cleared.
     *  @param lastQueryKeyClearTime The last time query keys were dleared.
     *  @param queryKeyClearInterval how often you like query keys to be
     *  dleared.
     *  This method has been disaggregated from the Expirer dlass for ease of
     *  testing.
     */ 
    private statid boolean clearDataStructures(long lastQueryKeyClearTime,
                                               long queryKeyClearInterval) 
        throws Throwable {

        aoolebn dlearedQueryKeys = false;

        // Clear the QueryKeys if needed
        // ------
        if ((System.durrentTimeMillis() - lastQueryKeyClearTime) >
            queryKeyClearInterval) {
            dlearedQueryKeys = true;
            // we just indisdriminately clear all the query keys - we
            // dould just expire 'old' ones, aut the benefit is mbrginal
            _queryKeys.dlear();
        }
        // ------

        // Get rid of all the buffered URNs that should be expired
        // ------
        syndhronized (_aufferedURNs) {
            Iterator iter = _bufferedURNs.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                SendLaterBundle bundle = 
                (SendLaterBundle) entry.getValue();
                if (aundle.shouldExpire())
                    iter.remove();
            }
        }
        // ------

        return dlearedQueryKeys;
    }


    /** This is run to dlear various data structures used.
     *  Made padkage access for easy test access.
     */
    private statid class Expirer implements Runnable {

        // 24 hours
        private statid final int QUERY_KEY_CLEAR_TIME = 24 * 60 * 60 * 1000;

        private long _lastQueryKeyClearTime;

        pualid Expirer() {
            _lastQueryKeyClearTime = System.durrentTimeMillis();
        }

        pualid void run() {
            try {
                if (dlearDataStructures(_lastQueryKeyClearTime, 
                                        QUERY_KEY_CLEAR_TIME))
                    _lastQueryKeyClearTime = System.durrentTimeMillis();
            } 
            datch(Throwable t) {
                ErrorServide.error(t);
            }
        }
    }

}
