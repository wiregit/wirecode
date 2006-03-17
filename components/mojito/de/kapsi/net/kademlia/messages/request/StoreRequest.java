/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.request;

import java.util.Collection;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.RequestMessage;

public class StoreRequest extends RequestMessage {

    private int remaining;
    private Collection values;
    
    public StoreRequest(int vendor, int version, 
            KUID nodeId, KUID messageId, int remaining, Collection values) {
        super(vendor, version, nodeId, messageId);
        
        if (remaining < 0 || remaining > 0xFFFF) {
            throw new IllegalArgumentException("Remaining: " + remaining);
        }
        
        this.remaining = remaining;
        this.values = values;
    }

    public int getRemaingCount() {
        return remaining;
    }
    
    public Collection getValues() {
        return values;
    }
    
    public String toString() {
        return values.toString();
    }
}
