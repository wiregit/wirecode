package org.limewire.mojito.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.mojito.KUID;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.PingResponse;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.util.ContactUtils;

public class DefaultMessageHandler2 implements ResponseHandler2 {

    private static final Log LOG 
        = LogFactory.getLog(DefaultMessageHandler2.class);
    
    private final RouteTable routeTable;
    
    private final StoreForward storeForward;
    
    public DefaultMessageHandler2(RouteTable routeTable, 
            StoreForward storeForward) {
        
        this.routeTable = routeTable;
        this.storeForward = storeForward;
    }
    
    @Override
    public void handleResponse(RequestMessage request, ResponseMessage response, 
            long time, TimeUnit unit) throws IOException {
        processContact(response.getContact(), response);
    }
    
    @Override
    public void handleTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time, TimeUnit unit) {
        routeTable.handleFailure(nodeId, dst);
    }
    
    public void handleLateResponse(ResponseMessage message) {
        Contact node = message.getContact();
        
        if (!node.isFirewalled()) {
            routeTable.add(node); // update
        }
    }
    
    @Override
    public void handleException(RequestMessage request, Throwable exception) {
    }

    public void handleRequest(RequestMessage message) {
        processContact(message.getContact(), message);
    }
    
    private boolean isLocalNodeID(KUID contactId) {
        return getLocalNodeID().equals(contactId);
    }
    
    private KUID getLocalNodeID() {
        return routeTable.getLocalNode().getNodeID();
    }
    
    /**
     * 
     */
    private synchronized void processContact(
            Contact node, DHTMessage message) {
        
        // If the Node is going to shutdown then don't bother
        // further than this.
        if (node.isShutdown()) {
            if (LOG.isInfoEnabled()) {
                LOG.info(node + " is going to shut down");
            }
            
            synchronized (routeTable) {
                // Make sure there's an existing Contact in the RouteTable.
                // Otherwise don't bother!
                Contact existing = routeTable.get(node.getNodeID());
                if (node.equals(existing)) {
                    
                    // Update the new Contact in the RouteTable and 
                    // mark it as shutdown
                    // mark the existing contact as shutdown if its alive or
                    // it will not be removed.
                    if (existing.isAlive()) {
                        existing.shutdown(true);
                    }
                    
                    routeTable.add(node);
                    node.shutdown(true);
                }
            }
            return;
        }
        
        // Ignore firewalled Nodes
        if (node.isFirewalled()) {
            if (LOG.isInfoEnabled()) {
                LOG.info(node + " is firewalled");
            }
            return;
        }
        
        if (ContactUtils.isPrivateAddress(node)) {
            if (LOG.isInfoEnabled()) {
                LOG.info(node + " has a private address");
            }
            return;
        }
        
        KUID nodeId = node.getNodeID();
        if (isLocalNodeID(nodeId)) {
            
            // This is expected if there's a Node ID collision
            assert (message instanceof PingResponse) 
                : "Expected a PingResponse but got a " + message.getClass()
                    + " from " + message.getContact();
            
            if (LOG.isInfoEnabled()) {
                LOG.info("Looks like our NodeID collides with " + node);
            }
            
            return;
        }
        
        if (storeForward != null) {
            storeForward.process(node, message);
        }
        
        // Add the Node to our RouteTable or if it's
        // already there update its timeStamp and whatsoever
        routeTable.add(node);
    }
}
