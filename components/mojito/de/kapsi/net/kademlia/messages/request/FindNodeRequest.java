/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.request;

import de.kapsi.net.kademlia.KUID;

public class FindNodeRequest extends LookupRequest {

    public FindNodeRequest(int vendor, int version, KUID nodeId, 
            KUID messageId, KUID lookupId) {
        super(vendor, version, nodeId, messageId, lookupId);
    }
}
