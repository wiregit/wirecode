package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.response.PingResponse;

public class PingResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(PingResponseHandler.class);
    
    private List listeners = new ArrayList();
    
    public PingResponseHandler(Context context) {
        super(context);
    }

    public void addPingListener(PingListener listener) {
        listeners.add(listener);
    }
    
    public void removePingListener(PingListener listener) {
        listeners.remove(listener);
    }

    public PingListener[] getPingListeners() {
        return (PingListener[])listeners.toArray(new PingListener[0]);
    }

    protected void response(ResponseMessage message, long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Received pong from " + message.getContactNode() 
                    + " after " + getErrors() + " errors and a total time of " + time() + "ms");
        }
        
        PingResponse response = (PingResponse)message;
        context.setExternalSocketAddress(response.getSocketAddress());
    }

    public void handleTimeout(KUID nodeId, 
            SocketAddress dst, RequestMessage message, long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Ping to " + ContactNode.toString(nodeId, dst) 
                    + " failed after " + time + "ms");
        }
        
        super.handleTimeout(nodeId, dst, message, time);
    }
    
    protected void resend(KUID nodeId, SocketAddress dst, Message message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Re-sending Ping to " + ContactNode.toString(nodeId, dst));
        }
        
        super.resend(nodeId, dst, message);
    }

    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Giving up to ping " + ContactNode.toString(nodeId, dst) 
                    + " after " + getMaxErrors() + " errors and a total time of "+ time() + "ms");
        }
    }
}
