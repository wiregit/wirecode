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
    public static final UDPService _udp;
    
    static {
        _queryKeys = new Hashtable(); // need sychronization
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
        
        // see if you have a QueryKey
        // ------
        QueryKey key = (QueryKey) _queryKeys.get(ep);
        if (key == null) {
            PingRequest pr = PingRequest.createQueryKeyRequest();
            try {
                _udp.send(pr, ep.getAddress(), ep.getPort());
            }
            catch (IOException ignored) {}
            // wait a little, hope to get the query key
            Thread.sleep(150);
            key = (QueryKey) _queryKeys.get(ep);
            // TODO: is this the right thing to do?  i hate it but lets keep it
            // for now.  a couple of options though:
            // 1) pre-fetch the query keys when bypassing the result
            // 2) return status to the caller and have them react
            if (key == null) return;
        }
        // ------
        
        // construct a URN Guess query and send it off
        // ------
        QueryRequest query = QueryRequest.createQueryKeyQuery(queryURN, key);
        try {
            _udp.send(query, ep.getAddress(), ep.getPort());
        }
        catch (IOException ignored) {}
        // ------
    }

}