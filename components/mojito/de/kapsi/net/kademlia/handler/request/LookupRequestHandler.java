/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.request;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.Node;
import de.kapsi.net.kademlia.handler.AbstractRequestHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.request.LookupRequest;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.settings.RouteTableSettings;

public class LookupRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(LookupRequestHandler.class);
    
    public LookupRequestHandler(Context context) {
        super(context);
    }

    public void handleRequest(KUID nodeId, SocketAddress src, Message message) throws IOException {
        
        if (!(message instanceof LookupRequest)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("LookupRequestHandler cannot handle " + message 
                        + " from " + Node.toString(nodeId, src));
            }
            return;
        }
        
        LookupRequest request = (LookupRequest)message;
        KUID lookup = request.getLookupID();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(Node.toString(nodeId, src) + " is trying to lookup " + lookup);
        }
        
        List bucketList 
            = context.getRouteTable().getBest(lookup, nodeId, KademliaSettings.getReplicationParameter(), RouteTableSettings.getSkipStale());
        
        FindNodeResponse response = context.getMessageFactory()
                    .createFindNodeResponse(request.getMessageID(), bucketList);
        
        context.getMessageDispatcher().send(src, response, null);
    }
}
