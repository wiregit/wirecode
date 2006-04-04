/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.response;

import java.util.Collection;
import java.util.Collections;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.ResponseMessage;

public abstract class LookupResponse extends ResponseMessage {
    
    protected final Collection values;
    
    public LookupResponse(int vendor, int version, KUID nodeId,
            KUID messageId, Collection responseValues) {
        super(vendor, version, nodeId, messageId);
        
        this.values = Collections.unmodifiableCollection(responseValues);
    }
    
    public Collection getValues() {
        return values;
    }
    
    public String toString() {
        return values.toString();
    }
}
