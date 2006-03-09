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
            if (LOG.isTraceEnabled()) {
                LOG.trace(node + " is known and updating its info");
            }
            //update contact info
            if(!node.getSocketAddress().equals(src)) {
                //ping host to check somebody is not spoofing an address change
                RequestMessage ping = context.getMessageFactory().createPingRequest();
                AbstractResponseHandler handler = new PingResponseHandler(context, 
                        new PingListener() {
                            public void pingResponse(KUID nodeId, SocketAddress address, long time) {
                                if(time<0) {
                                    //replace node
                                    Node n = new Node(nodeId,src);
                                    routeTable.add(n);
                                    routeTable.updateTimeStamp(n);
                                }
                                else {} //ping successfull - discard. TODO add spoofer to IP ban list
                            }
                });
                context.getMessageDispatcher().send(node,ping,handler);
            } else {
                routeTable.updateTimeStamp(node);
            }
        } else if (!routeTable.isFull()) {
            node = new Node(nodeId, src);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Adding " + node + " to RouteTable");
            }
            routeTable.add(node);
            
            // TODO store closest KeyValues
            /*List values = context.getDatabase().getBest(nodeId, REPLICATE_VALUES);
            if (!values.isEmpty()) {
                context.getMessageDispatcher().send(node, 
                        context.getMessageFactory().createStoreRequest(values), null);
            }*/
        } else if (routeTable.updateIfCached(nodeId)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(Node.toString(nodeId, src) + " is in RouteTable's LRU cache");
            }
            
        } else {
            
            List bucketList = routeTable.getBest(nodeId, KademliaSettings.getReplicationParameter());
            Node leastRecentlySeen = 
                BucketList.getLeastRecentlySeen(BucketList.sort(bucketList));
            
            node = new Node(nodeId, src);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Adding " + node 
                        + " to RouteTable's LRU cache and pinging the least recently seen Node " 
                        + leastRecentlySeen);
            }
            routeTable.addToCache(node);
            
            // TODO don't ping
            PingRequest ping = context.getMessageFactory().createPingRequest();
            context.getMessageDispatcher().send(leastRecentlySeen, ping, this);
        }
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
