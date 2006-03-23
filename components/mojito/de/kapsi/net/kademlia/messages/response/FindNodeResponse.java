/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.response;

import java.util.List;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.security.QueryKey;

public class FindNodeResponse extends LookupResponse {
    
    private QueryKey queryKey;
    
    public FindNodeResponse(int vendor, int version, KUID nodeId, 
            KUID messageId, QueryKey queryKey, List bucketList) {
        super(vendor, version, nodeId, messageId, bucketList);
        
        this.queryKey = queryKey;
    }

    public QueryKey getQueryKey() {
        return queryKey;
    }
}
