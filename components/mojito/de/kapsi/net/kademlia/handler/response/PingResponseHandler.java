/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.Node;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.handler.request.PingRequestHandler;
import de.kapsi.net.kademlia.messages.Message;

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
            LOG.trace("Ping to " + Node.toString(nodeId, src) 
                    + " succeeded");
        }
        
        if (l != null) {
            getEventDispatcher().add(new Runnable() {
                public void run() {
                    l.pingResponse(nodeId, src, time);
                }
            });
        }
    }
    
    public void handleTimeout(final KUID nodeId, final SocketAddress dst, 
            long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Ping to " + Node.toString(nodeId, dst) + " failed");
        }
        
        if (l != null) {
            getEventDispatcher().add(new Runnable() {
                public void run() {
                    l.pingResponse(nodeId, dst, -1L);
                }
            });
        }
    }
}
