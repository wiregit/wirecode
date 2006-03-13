/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.response.StoreResponse;

/**
 * Currently unused. Could be used to ACK/NACK store
 * requests
 */
public class StoreResponseHandler extends AbstractResponseHandler {
    
    private Collection keyValues;
    
    public StoreResponseHandler(Context context, Collection keyValues) {
        super(context);
        this.keyValues = keyValues;
    }

    public void handleResponse(KUID nodeId, SocketAddress src, Message message, long time) 
            throws IOException {
        
        StoreResponse response = (StoreResponse)message;
        Collection storeStatus = response.getStoreStatus();
        
        // TODO The collection contains only one entry as 
        // does the keyValues collection
        
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, long time) 
            throws IOException {
        
        // TODO Node didn't reply or the message got lost
    }
}
