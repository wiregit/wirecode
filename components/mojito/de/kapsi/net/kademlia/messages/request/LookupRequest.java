/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.request;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.RequestMessage;

public abstract class LookupRequest extends RequestMessage {
    
    protected final KUID lookupId;
    
    public LookupRequest(int vendor, int version, KUID nodeId, 
            KUID messageId, KUID lookupId) {
        super(vendor, version, nodeId, messageId);
        this.lookupId = lookupId;
    }
    
    public KUID getLookupID() {
        return lookupId;
    }
}
