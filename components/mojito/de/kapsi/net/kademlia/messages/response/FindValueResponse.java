/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.response;

import java.util.Collection;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.db.KeyValue;

public class FindValueResponse extends LookupResponse {
    
    public FindValueResponse(int vendor, int version, 
            KUID nodeId, KUID messageId, Collection values) {
        super(vendor, version, nodeId, messageId, values);
    }
    
    public boolean isKeyValueResponse() {
        if (isEmpty()) {
            return false;
        }
        
        return (iterator().next() instanceof KeyValue);
    }
}
