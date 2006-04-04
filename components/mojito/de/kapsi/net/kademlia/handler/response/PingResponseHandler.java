/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.handler.request.PingRequestHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.response.PingResponse;

public class PingResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(PingRequestHandler.class);
    
    private PingListener l;
    
    public PingResponseHandler(Context context, PingListener l) {
        super(context);
        this.l = l;
    }

    public void handleResponse(final KUID nodeId, final SocketAddress src, 
            Message message, final long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Ping to " + ContactNode.toString(nodeId, src) 
                    + " succeeded");
        }
        
        PingResponse response = (PingResponse)message;
        SocketAddress externalAddress = response.getSocketAddress();
       
        if (externalAddress != null) {
            SocketAddress currentAddress = context.getExternalSocketAddress();
            if (!externalAddress.equals(currentAddress)) {
                
                // TODO: Make sure it's really our external Address!
                context.setExternalSocketAddress(externalAddress);
            }
        }
        
        if (l != null) {
            getEventDispatcher().add(new Runnable() {
                public void run() {
                    l.pingSuccess(nodeId, src, time);
                }
            });
        }
    }
    
    public void handleTimeout(final KUID nodeId, final SocketAddress dst, 
            long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Ping to " + ContactNode.toString(nodeId, dst) + " failed");
        }
        
        if (l != null) {
            getEventDispatcher().add(new Runnable() {
                public void run() {
                    l.pingTimeout(nodeId, dst);
                }
            });
        }
    }
}
