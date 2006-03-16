/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.request;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.AbstractRequestHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.response.PingResponse;

/**
 * 
 * @author Roger Kapsi
 */
public class PingRequestHandler extends AbstractRequestHandler {

    private static final Log LOG = LogFactory.getLog(PingRequestHandler.class);
    
    public PingRequestHandler(Context context) {
        super(context);
    }
    
    public void handleRequest(KUID nodeId, SocketAddress src, Message message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(ContactNode.toString(nodeId, src) + " sent us a Ping");
        }
        
        PingResponse pong = context.getMessageFactory()
                .createPingResponse(message.getMessageID(), src);
        
        context.getMessageDispatcher().send(src, pong, null);
    }
}
