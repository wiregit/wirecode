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

public class StoreRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(StoreRequestHandler.class);
    
    public StoreRequestHandler(Context context) {
        super(context);
    }
    
    public void handleRequest(KUID nodeId, SocketAddress src, Message message) throws IOException {
        // Before we're going to accept the store request make
        // sure that the originator has queried us at least. Check
        // also the replacement cache!
        ContactNode node = context.getRouteTable().get(nodeId, true);
        if (node == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error(ContactNode.toString(nodeId, src) 
                        + " is unknown and requests us to store some values");
            }
            return;
        }
        
        StoreRequest request = (StoreRequest)message;
        Collection values = request.getValues();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(ContactNode.toString(nodeId, src) 
                    + " requested us to store the KeyValues " + values);
        }
        
        List stats = new ArrayList(values.size());
        
        // Add the KeyValues...
        for(Iterator it = values.iterator(); it.hasNext(); ) {
            KeyValue keyValue = (KeyValue)it.next();
            
            try {
                if (context.getDatabase().add(keyValue)) {
                    stats.add(new StoreResponse.Status(keyValue.getKey(), StoreResponse.SUCCEEDED));
                } else {
                    stats.add(new StoreResponse.Status(keyValue.getKey(), StoreResponse.FAILED));
                }
            } catch (SignatureException err) {
                stats.add(new StoreResponse.Status(keyValue.getKey(), StoreResponse.FAILED));
            } catch (InvalidKeyException err) {
                stats.add(new StoreResponse.Status(keyValue.getKey(), StoreResponse.FAILED));
            }
        }
        
        StoreResponse response = context.getMessageFactory()
                                    .createStoreResponse(request.getMessageID(), stats);
        
        context.getMessageDispatcher().send(src, response, null);
    }
}
