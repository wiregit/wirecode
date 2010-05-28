package org.limewire.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.mojito.KUID;
import org.limewire.mojito.message.Message;
import org.limewire.mojito.message.PingResponse;
import org.limewire.mojito.message.RequestMessage;
import org.limewire.mojito.message.ResponseMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.util.ContactUtils;

public class DefaultMessageHandler implements ResponseHandler {

    private static final Log LOG 
        = LogFactory.getLog(DefaultMessageHandler.class);
    
    private final RouteTable routeTable;
    
    private final StoreForward storeForward;
    
    public DefaultMessageHandler(RouteTable routeTable, 
            StoreForward storeForward) {
        
        this.routeTable = routeTable;
        this.storeForward = storeForward;
    }
    
    @Override
    public void handleResponse(RequestHandle request, 
            ResponseMessage response, long time, TimeUnit unit) 
                throws IOException {
        processContact(response.getContact(), response);
    }
    
    @Override
    public void handleTimeout(RequestHandle request, 
            long time, TimeUnit unit) {
        
        KUID contactId = request.getContactId();
        SocketAddress address = request.getAddress();
        
        routeTable.handleFailure(contactId, address);
    }
    
    public void handleLateResponse(ResponseMessage message) {
        Contact node = message.getContact();
        
        if (!node.isFirewalled()) {
            routeTable.add(node); // update
        }
    }
    
    @Override
    public void handleException(RequestHandle request, Throwable exception) {
    }

    public void handleRequest(RequestMessage message) {
        processContact(message.getContact(), message);
    }
    
    private boolean isLocalNodeID(KUID contactId) {
        return getLocalNodeID().equals(contactId);
    }
    
    private KUID getLocalNodeID() {
        return routeTable.getLocalNode().getContactId();
    }
    
    /**
     * 
     */
    private synchronized void processContact(
            Contact node, Message message) {
        
        // If the Node is going to shutdown then don't bother
        // further than this.
        if (node.isShutdown()) {
            if (LOG.isInfoEnabled()) {
                LOG.info(node + " is going to shut down");
            }
            
            synchronized (routeTable) {
                // Make sure there's an existing Contact in the RouteTable.
                // Otherwise don't bother!
                Contact existing = routeTable.get(node.getContactId());
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
        
        KUID nodeId = node.getContactId();
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
