/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.response;

import java.util.Collection;

import de.kapsi.net.kademlia.KUID;

public class FindValueResponse extends LookupResponse {
    
    public FindValueResponse(int vendor, int version, 
            KUID nodeId, KUID messageId, Collection values) {
        super(vendor, version, nodeId, messageId, values);
    }
}
