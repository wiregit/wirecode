/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.routing.RoutingTable;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.util.NetworkUtils;

/**
 * The DefaultMessageHandler performs basic Kademlia RouteTable 
 * update operations. That means adding new Nodes if RouteTable 
 * is not full, updating the last seen time stamp of Nodes and 
 * so forth.
 */
public class DefaultMessageHandler extends MessageHandler 
        implements RequestHandler, ResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(DefaultMessageHandler.class);
    
    public DefaultMessageHandler(Context context) {
        super(context);
    }
    
    public long timeout() {
        return 0L;
    }

    public void handleResponse(KUID nodeId, SocketAddress src, 
            Message message, long time) throws IOException {
        
        ContactNode node = getRouteTable().get(nodeId, true);
        if (node == null) {
            addLiveContactInfo(nodeId, src, message);
        } else {
            updateContactInfo(node, nodeId, src, message);
        }
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, 
            long time) throws IOException {
        RoutingTable routeTable = getRouteTable();
        routeTable.handleFailure(nodeId);
    }

    public void handleRequest(KUID nodeId, SocketAddress src, 
            Message message) throws IOException {
        
        ContactNode node = getRouteTable().get(nodeId, true);
        if (node == null) {
            addLiveContactInfo(nodeId, src, message);
        } else {
            updateContactInfo(node, nodeId, src, message);
        }
    }
    
    private void addLiveContactInfo(KUID nodeId, SocketAddress src, 
            Message message) throws IOException {
        
        RoutingTable routeTable = getRouteTable();
        
        ContactNode node = new ContactNode(nodeId, src);
        routeTable.add(node,true);
            
        Collection keyValues = context.getDatabase().getAllValues();
        for (Iterator iter = keyValues.iterator(); iter.hasNext();) {
            KeyValue keyValue = (KeyValue) iter.next();
            boolean isCloser = node.getNodeID().isCloser(context.getLocalNodeID(),keyValue.getKey());
            if(!keyValue.isLocalKeyValue() && isCloser) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Node "+node+" is now closer then us to a value. Sending store request");   
                }
                context.getMessageDispatcher().send(node, 
                      context.getMessageFactory().createStoreRequest(keyValues), null);
            }
        }
    }
    
    private void updateContactInfo(ContactNode node, KUID nodeId, 
            SocketAddress src, Message message) throws IOException {
        
        if (node.getSocketAddress().equals(src)) {
            context.getRouteTable().updateTimeStamp(node);
            return;
        }
        
        // Huh? The addresses are not equal but both belong
        // obviously to this local machine!? There isn't much
        // we can do. Set it to the new address and hope it 
        // doesn't use a different NIF everytime...
        if (NetworkUtils.isLocalAddress(src)
                && NetworkUtils.isLocalAddress(node.getSocketAddress())) {
            node.setSocketAddress(src);
            context.getRouteTable().updateTimeStamp(node);
            return;
        }
        
        // TODO check if src is trying to spoof its NodeID!
        node.setSocketAddress(src);
        context.getRouteTable().updateTimeStamp(node);
        
        /*if (!isSpoofCheckActive(nodeId)) {
            ResponseHandler handler = createSpoofChecker(nodeId, src);
            PingRequest request = context.getMessageFactory().createPingRequest();
            context.getMessageDispatcher().send(node, request, handler);
        }*/
    }
    
    /*private final Map checker = new FixedSizeHashMap(8);
    
    private ResponseHandler createSpoofChecker(KUID nodeId, SocketAddress address) {
        ResponseHandler responseHandler = new SpoofChecker(nodeId, address);
        checker.put(nodeId, responseHandler);
        return responseHandler;
    }
    
    private boolean isSpoofCheckActive(KUID nodeId) {
        return checker.containsKey(nodeId);
    }
    
    private class SpoofChecker extends AbstractResponseHandler {
        
        private KUID nodeId;
        private SocketAddress address;
        
        private SpoofChecker(KUID nodeId, SocketAddress address) {
            super(DefaultMessageHandler2.this.context);
            
            this.nodeId = nodeId;
            this.address = address;
        }
        
        public void handleResponse(KUID nodeId, SocketAddress src, 
                Message message, long time) throws IOException {
            
            checker.remove(this.nodeId);
            
            if (LOG.isTraceEnabled()) {
                LOG.trace(ContactNode.toString(nodeId, src) + " responded in " + time + " ms." 
                        + ContactNode.toString(nodeId, address) + " tries likely to spoof its NodeID!");
            }
            
            // DO NOTHING 
        }

        public void handleTimeout(KUID nodeId, SocketAddress dst, 
                long time) throws IOException {
            
            checker.remove(this.nodeId);
            
            if (LOG.isTraceEnabled()) {
                LOG.trace(ContactNode.toString(nodeId, dst) + " did not respond. Replacing it with "
                        + ContactNode.toString(nodeId, address));
            }
            
            ContactNode node = context.getRouteTable().get(nodeId, true);
            if (node != null && !address.equals(node.getSocketAddress())) {
                node.setSocketAddress(address);
                context.getRouteTable().updateTimeStamp(node);
            }
        }
    }*/
}
