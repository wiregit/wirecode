/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.response;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.ResponseMessage;

public class StoreResponse extends ResponseMessage {
    
    private KUID key;
    
    public StoreResponse(int vendor, int version, KUID nodeId, 
            KUID messageId, KUID key) {
        super(vendor, version, nodeId, messageId);
        this.key = key;
    }
    
    public KUID getKey() {
        return key;
    }
}
