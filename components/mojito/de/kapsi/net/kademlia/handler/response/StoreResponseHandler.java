/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.request.StoreRequest;
import de.kapsi.net.kademlia.messages.response.StoreResponse;

/**
 * Currently unused. Could be used to ACK/NACK store
 * requests
 */
public class StoreResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(StoreResponseHandler.class);
    
    private static final int MAX_ERRORS = 3;
    
    private int errorCount = 0;
    
    private int index = 0;
    private int length = 0;
    
    private List keyValues;
    
    public StoreResponseHandler(Context context, List keyValues) {
        super(context);
        
        this.keyValues = keyValues;
    }

    public void handleResponse(KUID nodeId, SocketAddress src, Message message, long time) 
            throws IOException {
        
        StoreResponse response = (StoreResponse)message;
        
        int requesting = response.getRequestCount();
        
        // TODO: What to do with the staus?
        Collection storeStatus = response.getStoreStatus();
        
        if (requesting > 0 && index < keyValues.size()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(ContactNode.toString(nodeId, src) 
                        + " is requesting from us " + requesting + " KeyValues");
            }
            
            index += length;
            length = Math.min(requesting, keyValues.size()-index);
            
            List toSend = new ArrayList(length);
            for(int i = 0; i < length; i++) {
                toSend.add(keyValues.get(index + i));
            }
            
            StoreRequest request 
                = (StoreRequest)context.getMessageFactory()
                    .createStoreRequest(keyValues.size()-index-length, toSend);
            
            context.getMessageDispatcher().send(nodeId, src, request, this);
        }
        
        // reset the error counter
        errorCount = 0;
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, long time) 
            throws IOException {
        
        errorCount++;
        if (errorCount >= MAX_ERRORS) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("");
            }
            return;
        }
        
        int remaining = keyValues.size()-index;
        
        if (remaining > 0) {
            StoreRequest request 
                = (StoreRequest)context.getMessageFactory()
                    .createStoreRequest(remaining, Collections.EMPTY_LIST);
        
            context.getMessageDispatcher().send(nodeId, dst, request, this);
        }
    }
}
