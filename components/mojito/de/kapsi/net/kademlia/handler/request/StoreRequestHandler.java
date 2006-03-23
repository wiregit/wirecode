/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.request;

import java.io.IOException;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.handler.AbstractRequestHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.request.StoreRequest;
import de.kapsi.net.kademlia.messages.response.StoreResponse;
import de.kapsi.net.kademlia.security.QueryKey;
import de.kapsi.net.kademlia.settings.KademliaSettings;

public class StoreRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(StoreRequestHandler.class);
    
    public StoreRequestHandler(Context context) {
        super(context);
    }
    
    public void handleRequest(KUID nodeId, SocketAddress src, 
            Message message) throws IOException {
        
        StoreRequest request = (StoreRequest)message;
        QueryKey queryKey = request.getQueryKey();
        
        if (queryKey == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error(ContactNode.toString(nodeId, src) 
                        + " does not provide a QueryKey");
            }
            return;
        }
        
        QueryKey expected = QueryKey.getQueryKey(src);
        if (!expected.equals(queryKey)) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Expected " + expected + " from " 
                        + ContactNode.toString(nodeId, src) 
                        + " but got " + queryKey);
            }
            return;
        }
        
        int remaining = request.getRemaingCount();
        Collection values = request.getValues();
        
        if (LOG.isTraceEnabled()) {
            if (!values.isEmpty()) {
                LOG.trace(ContactNode.toString(nodeId, src) 
                        + " requested us to store the KeyValues " + values);
            } else {
                LOG.trace(ContactNode.toString(nodeId, src)
                        + " requested us to store " + remaining + " KeyValues");
            }
        }
        
        int k = KademliaSettings.getReplicationParameter();
        
        // Avoid to create an empty ArrayList
        List stats = (values.isEmpty() ? Collections.EMPTY_LIST : new ArrayList(values.size()));
        
        // Add the KeyValues...
        for(Iterator it = values.iterator(); it.hasNext(); ) {
            KeyValue keyValue = (KeyValue)it.next();

            // under the assumption that the requester sent us a lookup before
            // check if we are part of the closest alive nodes to this value
            List nodesList = getRouteTable().select(keyValue.getKey(), k, true, false);
            if(nodesList.contains(context.getLocalNode())) {
                try {
                    if (context.getDatabase().add(keyValue)) {
                        stats.add(new StoreResponse.StoreStatus(keyValue.getKey(), StoreResponse.SUCCEEDED));
                    } else {
                        stats.add(new StoreResponse.StoreStatus(keyValue.getKey(), StoreResponse.FAILED));
                    }
                } catch (SignatureException err) {
                    stats.add(new StoreResponse.StoreStatus(keyValue.getKey(), StoreResponse.FAILED));
                } catch (InvalidKeyException err) {
                    stats.add(new StoreResponse.StoreStatus(keyValue.getKey(), StoreResponse.FAILED));
                }
            } else {
                stats.add(new StoreResponse.StoreStatus(keyValue.getKey(), StoreResponse.FAILED));
            }
        }
        
        // TODO do not request all values at once
        StoreResponse response 
            = context.getMessageFactory().createStoreResponse(request.getMessageID(), remaining, stats);
        
        context.getMessageDispatcher().send(src, response, null);
    }
}
