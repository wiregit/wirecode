package com.limegroup.gnutella.guess;

import com.sun.java.util.collections.*;
import java.io.IOException;
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
    public final UDPService _udp;
    
    static {
        _queryKeys = new Hashtable(); // need sychronization
    }        

    public OnDemandUnicaster() {
        _udp = UDPService.instance();
    }

    /** Sends out a UDP query with the specified URN to the specified host.
     *  @throws IllegalArgumentException if ep or queryURN are null.
     *  @param ep the location you want to query.
     *  @param queryURN the URN you are querying for.
     */
    public void query(GUESSEndpoint ep, URN queryURN) 
        throws IllegalArgumentException {
        if (ep == null)
            throw new IllegalArgumentException("No Endpoint!");
        if (queryURN == null)
            throw new IllegalArgumentException("No urn to look for!");
        
        // see if you have a QueryKey
        QueryKey key = (QueryKey) _queryKeys.get(ep);
        if (key == null) {
            PingRequest pr = PingRequest.createQueryKeyRequest();
            
        }
    }

}