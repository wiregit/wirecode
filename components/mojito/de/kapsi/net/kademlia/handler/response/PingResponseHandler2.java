package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.io.Receipt;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.settings.NetworkSettings;

public class PingResponseHandler2 extends AbstractResponseHandler {
    
    private int errors = 0;
    
    public PingResponseHandler2(Context context) {
        super(context);
    }

    public void handleResponse(Receipt receipt, KUID nodeId, SocketAddress src, Message message, long time) throws IOException {
    }

    // TODO move it to abstract super class?
    public void handleTimeout(Receipt receipt, KUID nodeId, SocketAddress dst, long time) throws IOException {
        if (errors++ >= NetworkSettings.MAX_ERRORS.getValue()) {
            handleFinalTimeout(nodeId, dst, time);
            return;
        }
        
        // TODO since MessageID stays some MessageDispatcher modifications
        // are necessary to avoid multiple messages beeing sent to destionation
        Message request = receipt.getMessage();
        context.getMessageDispatcher().send(nodeId, dst, request, this);
    }
    
    // TODO if handleTimeout() is in super class then
    // make this an abstract method in the super class
    protected void handleFinalTimeout(KUID nodeId, SocketAddress dst, long time) {
        // TODO call listner!
    }
}
