/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.request;

import java.io.IOException;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.Node;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.handler.AbstractRequestHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.request.StoreRequest;

public class StoreRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(StoreRequestHandler.class);
    
    public StoreRequestHandler(Context context) {
        super(context);
    }
    
    public void handleRequest(KUID nodeId, SocketAddress src, Message message) throws IOException {
        if (!(message instanceof StoreRequest)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("StoreRequestHandler cannot handle " + message 
                        + " from " + Node.toString(nodeId, src));
            }
            return;
        }
        
        // Before we're going to accept the store request make
        // sure that the originator has queried us at least. Check
        // also the replacement cache!
        Node node = context.getRouteTable().get(nodeId, true);
        if (node == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error(Node.toString(nodeId, src) + " is unknown and requests us to store some values");
            }
            return;
        }
        
        StoreRequest request = (StoreRequest)message;
        Collection values = request.getValues();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(Node.toString(nodeId, src) 
                    + " requested us to store the Keys and Values " + values);
        }
        
        // Add the KeyValues...
        for(Iterator it = values.iterator(); it.hasNext(); ) {
            try {
                context.getDatabase().add((KeyValue)it.next());
            } catch (SignatureException err) {
                // TODO
            } catch (InvalidKeyException err) {
                // TODO 
            }
        }
    }
}
