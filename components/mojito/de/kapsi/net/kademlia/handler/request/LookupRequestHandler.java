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

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.AbstractRequestHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.request.LookupRequest;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.settings.KademliaSettings;

public class LookupRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(LookupRequestHandler.class);
    
    public LookupRequestHandler(Context context) {
        super(context);
    }

    public void handleRequest(KUID nodeId, SocketAddress src, Message message) throws IOException {
        
        LookupRequest request = (LookupRequest)message;
        KUID lookup = request.getLookupID();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(ContactNode.toString(nodeId, src) + " is trying to lookup " + lookup);
        }
        
        List bucketList 
            = context.getRouteTable().select(lookup,  
                    KademliaSettings.getReplicationParameter(),false);
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending back: " + bucketList);
        }
        
        FindNodeResponse response = context.getMessageFactory()
                    .createFindNodeResponse(request.getMessageID(), bucketList);
        
        context.getMessageDispatcher().send(src, response, null);
    }
}
