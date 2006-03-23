/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.request;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.request.FindValueRequest;
import de.kapsi.net.kademlia.messages.request.LookupRequest;
import de.kapsi.net.kademlia.messages.response.FindValueResponse;

public class FindValueRequestHandler extends LookupRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(FindValueRequestHandler.class);
    
    public FindValueRequestHandler(Context context) {
        super(context);
    }

    public void handleRequest(KUID nodeId, SocketAddress src, Message message) throws IOException {
        
        if (message instanceof FindValueRequest) {
            LookupRequest request = (LookupRequest)message;
            KUID lookup = request.getLookupID();

            Collection values = context.getDatabase().get(lookup);
            if (values != null && !values.isEmpty()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Hit! " + lookup + " = " + values);
                }
                
                FindValueResponse response = context.getMessageFactory()
                            .createFindValueResponse(request.getMessageID(), values);
                context.getMessageDispatcher().send(src, response, null);
                
                return; // We're done here!
            }
        }
        
        super.handleRequest(nodeId, src, message);
    }
}
