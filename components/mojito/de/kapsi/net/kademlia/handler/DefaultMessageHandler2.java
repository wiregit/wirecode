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
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.routing.RouteTable;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.util.BucketList;
import de.kapsi.net.kademlia.util.NetworkUtils;

/**
 * The DefaultMessageHandler performs basic Kademlia RouteTable 
 * update operations. That means adding new Nodes if RouteTable 
 * is not full, updating the last seen time stamp of Nodes and 
 * so forth.
 */
public class DefaultMessageHandler2 extends MessageHandler 
        implements RequestHandler, ResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(DefaultMessageHandler2.class);
    
    public DefaultMessageHandler2(Context context) {
        super(context);
    }
    
    public long timeout() {
        return 0L;
    }

    public void handleResponse(KUID nodeId, SocketAddress src, 
            Message message, long time) throws IOException {
        
        Node node = getRouteTable().get(nodeId, true);
        if (node == null) {
            addContactInfo(nodeId, src, message);
        } else {
            updateContactInfo(node, nodeId, src, message);
        }
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, 
            long time) throws IOException {
        removeIfStale(nodeId, dst);
    }

    public void handleRequest(KUID nodeId, SocketAddress src, 
            Message message) throws IOException {
        
        Node node = getRouteTable().get(nodeId, true);
        if (node == null) {
            addContactInfo(nodeId, src, message);
        } else {
            updateContactInfo(node, nodeId, src, message);
        }
    }
    
    private void addContactInfo(KUID nodeId, SocketAddress src, 
            Message message) throws IOException {
        
        RouteTable routeTable = getRouteTable();
        
        if (!routeTable.isFull()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Adding " + Node.toString(nodeId, src) 
                        + " to RouteTable");
            }
            routeTable.add(new Node(nodeId, src));
        } else {
            addContactInfoToCache(nodeId, src, message);
        }
    }
    
    private void addContactInfoToCache(KUID nodeId, SocketAddress src, 
            Message message) throws IOException {
        
        RouteTable routeTable = getRouteTable();
        List bucketList = routeTable.getBest(nodeId, KademliaSettings.getReplicationParameter());
        Node leastRecentlySeen = 
            BucketList.getLeastRecentlySeen(BucketList.sort(bucketList));
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Adding " + Node.toString(nodeId, src) 
                    + " to RouteTable's LRU cache and pinging the least recently seen Node " 
                    + leastRecentlySeen);
        }
        routeTable.addToCache(new Node(nodeId, src));
        
        // TODO don't ping
        PingRequest ping = context.getMessageFactory().createPingRequest();
        
        context.getMessageDispatcher()
            .send(leastRecentlySeen, ping, this);
    }
    
    private void updateContactInfo(Node node, KUID nodeId, 
            SocketAddress src, Message message) throws IOException {
        
        if (node.getSocketAddress().equals(src)) {
            context.getRouteTable().updateTimeStamp(node);
            return;
        }
        
        if (NetworkUtils.isLocalAddress(src)
                && NetworkUtils.isLocalAddress(node.getSocketAddress())) {
            node.setSocketAddress(src);
            context.getRouteTable().updateTimeStamp(node);
            return;
        }
        
        // TODO check if src is trying to spoof its NodeID!
        node.setSocketAddress(src);
        context.getRouteTable().updateTimeStamp(node);
    }
    
    private void removeIfStale(KUID nodeId, SocketAddress dst) 
            throws IOException {
        
        RouteTable routeTable = getRouteTable();
        Node node = routeTable.get(nodeId);
        
        if (node == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("No Node for " 
                        + Node.toString(nodeId, dst) + " in RouteTable");
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
    
    /*private static class SpoofChecker extends AbstractResponseHandler {
        
        private DefaultMessageHandler2 handler;
        private SocketAddress address;
        
        public SpoofChecker(DefaultMessageHandler2 handler, 
                SocketAddress address) {
            super(handler.context);
            
            this.handler = handler;
            this.address = address;
        }

        public void handleResponse(KUID nodeId, SocketAddress src, 
                Message message, long time) throws IOException {
            
            if (LOG.isTraceEnabled()) {
                LOG.trace(Node.toString(nodeId, src) + " responded in " + time + " ms." 
                        + Node.toString(nodeId, address) + " tries likely to spoof its NodeID!");
            }
            
            Node node = context.getRouteTable().get(nodeId, true);
            if (node != null) {
                context.getRouteTable().updateTimeStamp(node);
            }
        }

        public void handleTimeout(KUID nodeId, SocketAddress dst, 
                long time) throws IOException {
            
            if (LOG.isTraceEnabled()) {
                LOG.trace(Node.toString(nodeId, dst) + " did not respond. Replacing it with "
                        + Node.toString(nodeId, address));
            }
            
            Node node = context.getRouteTable().get(nodeId, true);
            if (node != null) {
                node.setSocketAddress(address);
                context.getRouteTable().updateTimeStamp(node);
            } else {
                handler.addContactInfo(nodeId, address, null);
            }
        }
    }*/
}
