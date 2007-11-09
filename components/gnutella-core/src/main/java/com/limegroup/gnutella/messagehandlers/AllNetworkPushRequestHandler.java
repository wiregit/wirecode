/**
 * 
 */
package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;
import java.util.EnumMap;
import java.util.Map;

import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutella.statistics.RouteErrorStat;

/**
 * Handles push request on all networks by looking up the corresponding reply handler
 * and notifying it of the push request.
 */
public class AllNetworkPushRequestHandler implements MessageHandler {
    
    private final Map<Network, ReceivedMessageStatHandler> statsHandlerByNetwork = new EnumMap<Network, ReceivedMessageStatHandler>(Network.class);
    private final MessageRouter messageRouter; 

    {
        statsHandlerByNetwork.put(Network.TCP, ReceivedMessageStatHandler.TCP_PUSH_REQUESTS);
        statsHandlerByNetwork.put(Network.UDP, ReceivedMessageStatHandler.UDP_PUSH_REQUESTS);
        statsHandlerByNetwork.put(Network.MULTICAST, ReceivedMessageStatHandler.MULTICAST_DUPLICATE_QUERIES);
    }
    
    public AllNetworkPushRequestHandler(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }
    
    public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        statsHandlerByNetwork.get(msg.getNetwork()).addMessage(msg);
        PushRequest request = (PushRequest)msg;
        if (handler == null) {
            throw new NullPointerException("null ReplyHandler");
        }
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler = messageRouter.getPushHandler(request.getClientGUID());

        if(replyHandler != null) {
            replyHandler.handlePushRequest(request, handler);
        } else {
            RouteErrorStat.PUSH_REQUEST_ROUTE_ERRORS.incrementStat();
            handler.countDroppedMessage();
        }
    }
}