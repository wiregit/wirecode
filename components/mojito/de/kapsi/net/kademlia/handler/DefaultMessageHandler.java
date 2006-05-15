/*
 * Lime Kademlia Distributed Hash Table (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
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

import com.limegroup.gnutella.dht.statistics.DataBaseStatisticContainer;
import com.limegroup.gnutella.guess.QueryKey;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.db.Database;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.db.Database.KeyValueBag;
import de.kapsi.net.kademlia.messages.DHTMessage;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.routing.RoutingTable;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.settings.NetworkSettings;

/**
 * The DefaultMessageHandler performs basic Kademlia RouteTable 
 * update operations. That means adding new Nodes if RouteTable 
 * is not full, updating the last seen time stamp of Nodes and 
 * so forth.
 */
public class DefaultMessageHandler extends MessageHandler 
        implements RequestHandler, ResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(DefaultMessageHandler.class);
    
    private DataBaseStatisticContainer databaseStats;
    
    public DefaultMessageHandler(Context context) {
        super(context);
        databaseStats = context.getDataBaseStats();
    }
    
    public void addTime(long time) {
    }
    
    public long time() {
        return 0L;
    }
    
    public long timeout() {
        return NetworkSettings.MAX_TIMEOUT.getValue();
    }

    public void handleResponse(ResponseMessage message, long time) throws IOException {
        addLiveContactInfo(message.getSource(), message);
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        context.getRouteTable().handleFailure(nodeId);
    }

    public void handleRequest(RequestMessage message) throws IOException {
        addLiveContactInfo(message.getSource(), message);
    }
    
    public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        // never called
    }
    
    private void addLiveContactInfo(ContactNode node, 
            DHTMessage message) throws IOException {
        
        if(node.isFirewalled()) return;
        
        RoutingTable routeTable = getRouteTable();
        boolean newNode = false;
        //only do store forward if it is a new node in our routing table (we are (re)connecting to the network) 
        //or a node that is reconnecting
        ContactNode existingNode = routeTable.get(node.getNodeID());
        if(existingNode == null || existingNode.getInstanceID() != node.getInstanceID()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Node "+node+" is new or has changed his instanceID, will check for store forward!");   
            }
            newNode = true;
        }
        //add node to the routing table -- update timestamp and info if needed
        routeTable.add(node, true);

        if (newNode) {
            
            int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
            
            //are we one of the K closest nodes to the contact?
            List closestNodes = routeTable.select(node.getNodeID(), k, false, false);
            
            if (closestNodes.contains(context.getLocalNode())) {
                List keyValuesToForward = new ArrayList();
                
                Database database = context.getDatabase();
                synchronized(database) {
                    Collection bags = database.getKeyValueBags();
                    for (Iterator iter = bags.iterator(); iter.hasNext(); ) {
                        KeyValueBag bag = (KeyValueBag)iter.next();
                        
                        //To avoid redundant STORE forward, a node only transfers a value if it is the closest to the key
                        //or if it's ID is closer than any other ID (except the new closest one of course)
                        //TODO: maybe relax this a little bit: what if we're not the closest and the closest is stale?
                        List closestNodesToKey = routeTable.select(bag.getKey(), k, false, false);
                        ContactNode closest = (ContactNode)closestNodesToKey.get(0);
                        if (context.isLocalNode(closest)   
                                || ((node.equals(closest)
                                        //maybe we haven't added him to the routing table
                                        || node.getNodeID().isCloser(closest.getNodeID(), bag.getKey())) 
                                        && (closestNodesToKey.size() > 1)
                                        && closestNodesToKey.get(1).equals(context.getLocalNode()))) {
                            
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Node "+node+" is now close enough to a value and we are responsible for xfer");   
                            }
                            databaseStats.STORE_FORWARD_COUNT.incrementStat();
                            for (Iterator iterator = bag.values().iterator(); iterator.hasNext();) {
                                KeyValue keyValue = (KeyValue) iterator.next();
                                KUID originatorID = keyValue.getNodeID();
                                if(originatorID != null && !originatorID.equals(node)) {
                                    keyValuesToForward.add(keyValue);
                                }
                            }
                        } else if (closestNodesToKey.size() == k) {
                            //if we are the furthest node: delete non-local value from local db
                            ContactNode furthest = (ContactNode)closestNodesToKey.get(closestNodesToKey.size()-1);
                            if(context.isLocalNode(furthest)) {
                                int count = bag.removeAll(true);
                                databaseStats.STORE_FORWARD_REMOVALS.addData(count);
                            }
                        }
                    }
                }
                
                if (!keyValuesToForward.isEmpty()) {
                    ResponseHandler handler = new StoreForwardResponseHandler(context, keyValuesToForward);           
                    RequestMessage request = context.getMessageFactory()
                        .createFindNodeRequest(node.getSocketAddress(), node.getNodeID());
                    
                    context.getMessageDispatcher().send(node, request, handler);
                }
            }
        }
    }
    
    /**
     * Handles Store-Forward response. We're actually sending our
     * Target Node a lookup for its own Node ID and it will tell us
     * the QueryKey we need to store a KeyValue.
     */
    private static class StoreForwardResponseHandler extends AbstractResponseHandler {

        private List keyValues;
        
        public StoreForwardResponseHandler(Context context, List keyValues) {
            super(context);
            this.keyValues = keyValues;
        }
        
        public void response(ResponseMessage message, long time) throws IOException {
            
            FindNodeResponse response = (FindNodeResponse)message;
            
            Collection values = response.getValues();
            for(Iterator it = values.iterator(); it.hasNext(); ) {
                // We did a FIND_NODE lookup use the info
                // to fill our routing table
                ContactNode node = (ContactNode)it.next();
                context.getRouteTable().add(node, false);
            }
            
            QueryKey queryKey = response.getQueryKey();
            context.store(message.getSource(), queryKey, keyValues);
        }

        protected void timeout(KUID nodeId, SocketAddress dst, 
                RequestMessage message, long time) throws IOException {
        }
        
        public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Sending a store-forward request to " + ContactNode.toString(nodeId, dst) + " failed", e);
            }
            
            fireTimeout(nodeId, dst, message, -1L);
        }
    }
}
