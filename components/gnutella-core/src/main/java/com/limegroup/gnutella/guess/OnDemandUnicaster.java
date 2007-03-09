package com.limegroup.gnutella.guess;


import java.net.InetAddress;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.security.AddressSecurityToken;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;

/** Utility class for sending GUESS queries.
 */
public class OnDemandUnicaster {
    
    private static final Log LOG = LogFactory.getLog(OnDemandUnicaster.class);

    // time to store buffered hosts waiting for a QK to query. 
    private static final int CLEAR_TIME = 5 * 60 * 1000; // 5 minutes
    
    // time to store hosts we've sent a query to.
    private static final int QUERIED_HOSTS_CLEAR_TIME = 30 * 1000; // 30 seconds
    
    /** IpPorts that we've queried for this GUID. */
    private static final Map<GUID.TimedGUID, Set<IpPort>> _queriedHosts;

    /** GUESSEndpoints => AddressSecurityToken. */
    private static final Map<GUESSEndpoint, AddressSecurityToken> _queryKeys;

    /** Access to UDP traffic. */
    private static final UDPService _udp;
    
    /**
     * Short term store for queries waiting for query keys.
     * GUESSEndpoints => URNs
     */
    private static final Map<GUESSEndpoint, SendLaterBundle> _bufferedURNs;

    static {
        // static initializers are only called once, right?
        _queryKeys = new Hashtable<GUESSEndpoint, AddressSecurityToken>(); // need sychronization
        _bufferedURNs = new Hashtable<GUESSEndpoint, SendLaterBundle>(); // synchronization handy
        _queriedHosts = new HashMap<GUID.TimedGUID, Set<IpPort>>();
        _udp = UDPService.instance();
        // schedule a runner to clear various data structures
        RouterService.schedule(new Expirer(), CLEAR_TIME, CLEAR_TIME);
        RouterService.schedule(new QueriedHostsExpirer(), QUERIED_HOSTS_CLEAR_TIME, QUERIED_HOSTS_CLEAR_TIME);
     }        

    /** Feed me AddressSecurityToken pongs so I can query people....
     *  pre: pr.getQueryKey() != null
     */
    public static void handleQueryKeyPong(PingReply pr) 
        throws NullPointerException, IllegalArgumentException {


        // validity checks
        // ------
        if (pr == null)
            throw new NullPointerException("null pong");

        AddressSecurityToken qk = pr.getQueryKey();
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
        if (bundle != null)
            sendQuery(bundle._queryURN, qk, endpoint);
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

        // see if you have a AddressSecurityToken - if not, request one
        // ------
        AddressSecurityToken key = _queryKeys.get(ep);
        if (key == null) {
            GUESSEndpoint endpoint = new GUESSEndpoint(ep.getInetAddress(),
                                                       ep.getPort());
            SendLaterBundle bundle = new SendLaterBundle(queryURN);
            _bufferedURNs.put(endpoint, bundle);
            PingRequest pr = PingRequest.createQueryKeyRequest();
            _udp.send(pr, ep.getInetAddress(), ep.getPort());
        }
        // ------
        // if possible send query, else buffer
        // ------
        else {
            sendQuery(queryURN, key, ep);
        }
        // ------
    }
    
    /**
     * Determines if the given host was sent a direct UDP URN query
     * in the last 30 seconds.
     * 
     * @param guid
     * @param host
     * @return
     */
    public static boolean isHostQueriedForGUID(GUID guid, IpPort host) {
        synchronized(_queriedHosts) {
            Set<IpPort> hosts = _queriedHosts.get(new GUID.TimedGUID(guid));
            return hosts != null ? hosts.contains(host) : false;
        }
    }
    
    private static void sendQuery(URN urn, AddressSecurityToken qk, IpPort ipp) {
        QueryRequest query = QueryRequest.createQueryKeyQuery(urn, qk);
        // store the query's GUID -> IPP so that when we get replies over
        // UDP we can allow them without requiring the whole ReplyNumber/ACK
        // thing.
        GUID qGUID = new GUID(query.getGUID());
        synchronized(_queriedHosts) {
            GUID.TimedGUID guid = new GUID.TimedGUID(qGUID, QUERIED_HOSTS_CLEAR_TIME);
            Set<IpPort> hosts = _queriedHosts.get(guid);
            if(hosts == null)
                hosts = new IpPortSet();
            hosts.add(ipp);
            // Always re-add, so the TimedGUID will last longer
            _queriedHosts.put(guid, hosts);
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("Sending query with GUID: " + qGUID + " for URN: " + urn + " to host: " + ipp);
        
        RouterService.getMessageRouter().originateQueryGUID(query.getGUID());
        _udp.send(query, ipp.getInetAddress(), ipp.getPort());
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
                                               long queryKeyClearInterval) {
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
            if (clearDataStructures(_lastQueryKeyClearTime, 
                                        QUERY_KEY_CLEAR_TIME))
                    _lastQueryKeyClearTime = System.currentTimeMillis();
        }
    }
    
    /** This is run to clear various data structures used.
     *  Made package access for easy test access.
     */
    private static class QueriedHostsExpirer implements Runnable {
        public void run() {
            synchronized(_queriedHosts) {
                long now = System.currentTimeMillis();
                for(Iterator<GUID.TimedGUID> iter = _queriedHosts.keySet().iterator(); iter.hasNext(); ) {
                    GUID.TimedGUID guid = iter.next();
                    if(guid.shouldExpire(now))
                        iter.remove();
                }
            }
        }
    }
}
