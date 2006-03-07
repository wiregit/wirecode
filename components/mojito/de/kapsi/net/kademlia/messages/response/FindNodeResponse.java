/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.response;

import java.util.List;

import de.kapsi.net.kademlia.KUID;

public class FindNodeResponse extends LookupResponse {
    
    public FindNodeResponse(int vendor, int version, KUID nodeId, 
            KUID messageId, List bucketList) {
        super(vendor, version, nodeId, messageId, bucketList);
    }
    
    public boolean isKeyValueResponse() {
        return false;
    }
}
