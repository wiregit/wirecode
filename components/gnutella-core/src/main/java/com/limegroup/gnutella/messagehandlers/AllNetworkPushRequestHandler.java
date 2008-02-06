/**
 * 
 */
package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;

import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;

/**
 * Handles push request on all networks by looking up the corresponding reply handler
 * and notifying it of the push request.
 */
public class AllNetworkPushRequestHandler implements MessageHandler {
    
    private final MessageRouter messageRouter;
    
    public AllNetworkPushRequestHandler(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }
    
    public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        PushRequest request = (PushRequest)msg;
        if (handler == null) {
            throw new NullPointerException("null ReplyHandler");
        }
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler = messageRouter.getPushHandler(request.getClientGUID());

        if(replyHandler != null) {
            replyHandler.handlePushRequest(request, handler);
        } else {
            handler.countDroppedMessage();
        }
    }
}