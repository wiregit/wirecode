/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.request;

import java.util.Collection;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.RequestMessage;

public class StoreRequest extends RequestMessage {

    private Collection values;
    
    public StoreRequest(int vendor, int version, 
            KUID nodeId, KUID messageId, Collection values) {
        super(vendor, version, nodeId, messageId);
        
        this.values = values;
    }

    public Collection getValues() {
        return values;
    }
    
    public String toString() {
        return values.toString();
    }
}
