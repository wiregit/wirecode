/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.Node;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.handler.response.PingResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.routing.RouteTable;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.util.BucketList;

/**
 * The DefaultMessageHandler performs basic Kademlia RouteTable 
 * update operations. That means adding new Nodes if RouteTable 
 * is not full, updating the last seen time stamp of Nodes and 
 * so forth.
 */
public final class DefaultMessageHandler extends MessageHandler 
        implements ResponseHandler, RequestHandler {
    
    private static final Log LOG = LogFactory.getLog(DefaultMessageHandler.class);
    
    public DefaultMessageHandler(Context context) {
        super(context);
    }
    
    public long timeout() {
        return 0L;
    }
    
    public void handleRequest(KUID nodeId, SocketAddress src, 
            Message message) throws IOException {
        handleSuccess(nodeId, src, message);
    }
    
    public void handleResponse(KUID nodeId, SocketAddress src, 
            Message message, long time) throws IOException {
        handleSuccess(nodeId, src, message);
    }
    
    private void handleSuccess(final KUID nodeId, final SocketAddress src, 
            Message message) throws IOException {
        final RouteTable routeTable = context.getRouteTable();
        
        Node node = routeTable.get(nodeId);
        if (node != null) {
            updateContactInfo(node, nodeId, src, message);
            
        } else if (!routeTable.isFull()) {
            addContactInfo(nodeId, src, message);
            
        } else if (routeTable.updateIfCached(nodeId)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(Node.toString(nodeId, src) + " is in RouteTable's LRU cache");
            }
        } else {
            replaceStaleContactInfo(nodeId, src, message);
        }
    }
    
    private void updateContactInfo(Node node, final KUID nodeId, 
            final SocketAddress src, final Message message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(node + " is known and updating its info");
        }
        
        final RouteTable routeTable = context.getRouteTable();
        
        //update contact info
        /*if(!node.getSocketAddress().equals(src)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(Node.toString(nodeId, src) + " claims to be " + node);
            }
            
            //ping host to check somebody is not spoofing an address change
            RequestMessage ping = context.getMessageFactory().createPingRequest();
            ResponseHandler handler = new PingResponseHandler(context, 
                    new PingListener() {
                        public void pingResponse(KUID nodeId, SocketAddress address, long time) {
                            Node node = routeTable.get(nodeId);
                            
                            if (node != null) {
                                // the old contact didn't respond.
                                if (time < 0L) {
                                    Node contact = new Node(nodeId, src);
                                    
                                    if (LOG.isTraceEnabled()) {
                                        LOG.trace(Node.toString(nodeId, address) 
                                                + " didn't respond. Replaceing it with new contact info " + contact);
                                    }
                                    
                                    // replace the old contact info
                                    routeTable.add(contact);
                                } else {
                                    
                                    if (LOG.isTraceEnabled()) {
                                        LOG.trace(node + " did respond in " + time + " ms. " 
                                                + Node.toString(nodeId, src) + " tries likely to spoof its NodeID!");
                                    }
                                    
                                    // update the time stamp of the old ontact 
                                    // since it responed to our request...
                                    routeTable.updateTimeStamp(node);
                                }
                            } else {
                                
                                if (LOG.isTraceEnabled()) {
                                    LOG.trace("Old contact " 
                                            + Node.toString(nodeId, address) + " is no longer in RouteTable. Going to add the new contact "
                                            + Node.toString(nodeId, src) + " to the replacement cache");
                                }
                                
                                // The old Node was removed from the RouteTable
                                // which can only mean the RT is full and it was
                                // replaced with the last seen Node from the
                                // replacement cache! Since all Nodes should have
                                // equal chances add it to the replacement cache...
                                try {
                                    replaceStaleContactInfo(nodeId, src, null);
                                } catch (IOException err) {
                                    LOG.error(err);
                                }
                            }
                        }
            });
            context.getMessageDispatcher().send(node, ping, handler);
        } else {*/
            node.setSocketAddress(src);
            routeTable.updateTimeStamp(node);
        //}
    }
    
    private void addContactInfo(KUID nodeId, SocketAddress src, Message message) throws IOException {
        Node node = new Node(nodeId, src);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Adding " + node + " to RouteTable");
        }
        context.getRouteTable().add(node);
        
        // TODO store closest KeyValues
        /*List values = context.getDatabase().getBest(nodeId, REPLICATE_VALUES);
        if (!values.isEmpty()) {
            context.getMessageDispatcher().send(node, 
                    context.getMessageFactory().createStoreRequest(values), null);
        }*/
    }
    
    private void replaceStaleContactInfo(KUID nodeId, SocketAddress src, Message message) throws IOException {
        List bucketList = context.getRouteTable().getBest(nodeId, KademliaSettings.getReplicationParameter());
        Node leastRecentlySeen = 
            BucketList.getLeastRecentlySeen(BucketList.sort(bucketList));
        
        Node node = new Node(nodeId, src);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Adding " + node 
                    + " to RouteTable's LRU cache and pinging the least recently seen Node " 
                    + leastRecentlySeen);
        }
        context.getRouteTable().addToCache(node);
        
        // TODO don't ping
        PingRequest ping = context.getMessageFactory().createPingRequest();
        context.getMessageDispatcher().send(leastRecentlySeen, ping, this);
    }
    
    public void handleTimeout(KUID nodeId, SocketAddress dst, long time) throws IOException {
        if (nodeId == null
                || nodeId.equals(context.getLocalNodeID())) {
            return;
        }
        
        RouteTable routeTable = context.getRouteTable();
        Node node = routeTable.get(nodeId);
        if (node == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("No Node for " + Node.toString(nodeId, dst) + " in RouteTable");
            }
            return;
        }
        
        if (routeTable.handleFailure(node)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(node + " is stale!");
            }
            
            if (routeTable.isFull() 
                    && !routeTable.isCacheEmpty()) {
                
                Node lastSeen = routeTable.replaceWithMostRecentlySeenNode(nodeId);
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Replaced " + Node.toString(nodeId, dst) + " with " + lastSeen 
                            + " from RouteTable's LRU cache");
                }
            }
        }
    }
    
}
