/*
 * Mojito Distributed Hash Tabe (DHT)
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
 
package com.limegroup.mojito.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.db.Database.KeyValueBag;
import com.limegroup.mojito.handler.response.StoreResponseHandler;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.ContactNode;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.statistics.DatabaseStatisticContainer;


/**
 * The DefaultMessageHandler performs basic Kademlia RouteTable 
 * update operations. That means adding new Nodes if RouteTable 
 * is not full, updating the last seen time stamp of Nodes and 
 * so forth.
 */
public class DefaultMessageHandler extends MessageHandler 
        implements RequestHandler, ResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(DefaultMessageHandler.class);
    
    private DatabaseStatisticContainer databaseStats;
    
    public DefaultMessageHandler(Context context) {
        super(context);
        databaseStats = context.getDatabaseStats();
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
        addLiveContactInfo(message.getContact(), message);
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        context.getRouteTable().handleFailure(nodeId);
    }

    public void handleRequest(RequestMessage message) throws IOException {
        addLiveContactInfo(message.getContact(), message);
    }
    
    public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        // never called
    }
    
    private void addLiveContactInfo(Contact node, DHTMessage message) throws IOException {
        
        if (node.isFirewalled()) {
            return;
        }
        
        RouteTable routeTable = getRouteTable();
        boolean newNode = false;
        //only do store forward if it is a new node in our routing table (we are (re)connecting to the network) 
        //or a node that is reconnecting
        Contact existingNode = routeTable.get(node.getNodeID());
        if (existingNode == null || existingNode.getInstanceID() != node.getInstanceID()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Node " + node + " is new or has changed his instanceID, will check for store forward!");   
            }
            newNode = true;
        }
        
        //add node to the routing table -- update timestamp and info if needed
        routeTable.add(node);

        if (newNode) {
            
            int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
            
            //are we one of the K closest nodes to the contact?
            List<Contact> closestNodes = routeTable.select(node.getNodeID(), k, false, false);
            
            if (closestNodes.contains(context.getLocalNode())) {
                List<KeyValue> keyValuesToForward = new ArrayList<KeyValue>();
                
                Database database = context.getDatabase();
                synchronized(database) {
                    Collection<KeyValueBag> bags = database.getKeyValueBags();
                    for(KeyValueBag bag : bags) {
                        
                        //To avoid redundant STORE forward, a node only transfers a value if it is the closest to the key
                        //or if it's ID is closer than any other ID (except the new closest one of course)
                        //TODO: maybe relax this a little bit: what if we're not the closest and the closest is stale?
                        List<Contact> closestNodesToKey = routeTable.select(bag.getKey(), k, false, false);
                        Contact closest = closestNodesToKey.get(0);
                        
                        if (context.isLocalNode(closest)   
                                || ((node.equals(closest)
                                        //maybe we haven't added him to the routing table
                                        || node.getNodeID().isNearer(closest.getNodeID(), bag.getKey())) 
                                        && (closestNodesToKey.size() > 1)
                                        && closestNodesToKey.get(1).equals(context.getLocalNode()))) {
                            
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Node "+node+" is now close enough to a value and we are responsible for xfer");   
                            }
                            databaseStats.STORE_FORWARD_COUNT.incrementStat();
                            for(KeyValue keyValue : bag.values()) {
                                KUID originatorID = keyValue.getNodeID();
                                if(originatorID != null && !originatorID.equals(node.getNodeID())) {
                                    keyValuesToForward.add(keyValue);
                                }
                            }
                            
                        } else if (closestNodesToKey.size() == k) {
                            //if we are the furthest node: delete non-local value from local db
                            Contact furthest = closestNodesToKey.get(closestNodesToKey.size()-1);
                            if(context.isLocalNode(furthest)) {
                                int count = bag.removeAll(true);
                                databaseStats.STORE_FORWARD_REMOVALS.addData(count);
                            }
                        }
                    }
                }
                
                if (!keyValuesToForward.isEmpty()) {
                    if (message instanceof FindNodeResponse) {
                        store(message.getContact(), 
                                ((FindNodeResponse)message).getQueryKey(), 
                                keyValuesToForward);
                    } else {
                        ResponseHandler handler = new GetQueryKeyHandler(keyValuesToForward);
                        RequestMessage request = context.getMessageHelper()
                            .createFindNodeRequest(node.getSocketAddress(), node.getNodeID());
                        
                        context.getMessageDispatcher().send(node, request, handler);
                    }
                }
            }
        }
    }
    
    private void store(Contact node, QueryKey queryKey, List<KeyValue> keyValues) throws IOException {
        new StoreResponseHandler(context, queryKey, keyValues).store(node);
    }
    
    private class GetQueryKeyHandler extends AbstractResponseHandler {
        
        private List<KeyValue> keyValues;
        
        private GetQueryKeyHandler(List<KeyValue> keyValues) {
            super(DefaultMessageHandler.this.context);
            this.keyValues = keyValues;
        }

        protected void response(ResponseMessage message, long time) throws IOException {
            
            FindNodeResponse response = (FindNodeResponse)message;
            
            Collection<? extends Contact> nodes = response.getNodes();
            for(Contact node : nodes) {
                // We did a FIND_NODE lookup use the info
                // to fill our routing table
                context.getRouteTable().add(node);
            }
            
            Contact node = message.getContact();
            store(node, response.getQueryKey(), keyValues);
        }

        protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        }

        public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
            
            if (LOG.isErrorEnabled()) {
                LOG.error("Getting the QueryKey from " + ContactNode.toString(nodeId, dst) + " failed", e);
            }
            
            fireTimeout(nodeId, dst, message, -1L);
        }
    }
}
