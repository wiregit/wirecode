/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.db.Database;
import de.kapsi.net.kademlia.db.KeyValueCollection;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.routing.RoutingTable;
import de.kapsi.net.kademlia.security.QueryKey;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.settings.LookupSettings;
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
    
    private long timeout = LookupSettings.getTimeout();
    
    public DefaultMessageHandler(Context context) {
        super(context);
    }
    
    public long timeout() {
        return timeout;
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
        routeTable.add(node, true);
        
        int k = KademliaSettings.getReplicationParameter();
        
        //are we one of the K closest nodes to the contact?
        List closestNodes = routeTable.select(nodeId, k, false, false);
        
        if (closestNodes.contains(context.getLocalNode())) {
            final List toStore = new ArrayList();
            
            Database database = context.getDatabase();
            synchronized(database) {
                Collection keyValues = database.getAllCollections();
                for (Iterator iter = keyValues.iterator(); iter.hasNext(); ) {
                    KeyValueCollection c = (KeyValueCollection)iter.next();
                    
                    //To avoid redundant STORE forward, a node only transfers a value if it is the closest to the key
                    //or if it's ID is closer than any other ID (except the new closest one of course)
                    //TODO: maybe relax this a little bit: what if we're not the closest and the closest is stale?
                    List closestNodesToKey = routeTable.select(c.getKey(), k, false, false);
                    ContactNode closest = (ContactNode)closestNodesToKey.get(0);
                    if (closest.equals(context.getLocalNode())   
                            || ((node.equals(closest)|| node.getNodeID().isCloser(closest.getNodeID(), c.getKey()))
                                    && (closestNodesToKey.size() > 1)
                                    && closestNodesToKey.get(1).equals(context.getLocalNode()))) {
                        
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Node "+node+" is now close enough to a value and we are responsible for xfer");   
                        }
                        toStore.addAll(c);
                    }
                }
            }
            
            if (!toStore.isEmpty()) {
                ResponseHandler handler = new StoreForwardResponseHandler(context, toStore);
                RequestMessage request = context.getMessageFactory().createFindNodeRequest(nodeId);
                context.getMessageDispatcher().send(nodeId, src, request, handler);
            }
        }
    }
    
    
    //TODO TODO TODO TODO TODO
    private void updateContactInfo(ContactNode node, KUID nodeId, 
            SocketAddress src, Message message) throws IOException {
        
        if (node.getSocketAddress().equals(src)) {
            context.getRouteTable().add(node,true);
            return;
        }
        
        // Huh? The addresses are not equal but both belong
        // obviously to this local machine!? There isn't much
        // we can do. Set it to the new address and hope it 
        // doesn't use a different NIF everytime...
        if (NetworkUtils.isLocalAddress(src)
                && NetworkUtils.isLocalAddress(node.getSocketAddress())) {
            node.setSocketAddress(src);
//            context.getRouteTable().updateTimeStamp(node);
            return;
        }
        
        // TODO check if src is trying to spoof its NodeID!
        node.setSocketAddress(src);
//        context.getRouteTable().updateTimeStamp(node);
        
        /*if (!isSpoofCheckActive(nodeId)) {
            ResponseHandler handler = createSpoofChecker(nodeId, src);
            PingRequest request = context.getMessageFactory().createPingRequest();
            context.getMessageDispatcher().send(node, request, handler);
        }*/
    }
    
    private static class StoreForwardResponseHandler extends AbstractResponseHandler {

        private static final int MAX_ERRORS = 3;
        
        private List keyValues;
        
        private boolean done = false;
        private int errors = 0;
        
        public StoreForwardResponseHandler(Context context, List keyValues) {
            super(context);
            this.keyValues = keyValues;
        }
        
        public void handleResponse(KUID nodeId, SocketAddress src, 
                Message message, long time) throws IOException {
            
            if (done) {
                return;
            }
            
            FindNodeResponse response = (FindNodeResponse)message;
            
            for(Iterator it = response.iterator(); it.hasNext(); ) {
                // We did a FIND_NODE lookup use the info
                // to fill our routing table
                ContactNode node = (ContactNode)it.next();
                context.getRouteTable().add(node, false);
            }
            
            QueryKey queryKey = response.getQueryKey();
            context.store(new ContactNode(nodeId, src), queryKey, keyValues);
            done = true;
        }

        public void handleTimeout(KUID nodeId, SocketAddress dst, long time) 
                throws IOException {
            
            if (done) {
                return;
            }
            
            if (++errors >= MAX_ERRORS) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Max number of errors has occured. Giving up!");
                }
                return;
            }
            
            RequestMessage request = context.getMessageFactory().createFindNodeRequest(nodeId);
            context.getMessageDispatcher().send(nodeId, dst, request, this);
        }
    }
}
