/*
 * Mojito Distributed Hash Table (Mojito DHT)
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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.DatabaseStatisticContainer;


/**
 * The DefaultMessageHandler performs basic Kademlia RouteTable 
 * update operations. That means adding new Nodes if RouteTable 
 * is not full, updating the last seen time stamp of Nodes and 
 * so forth.
 */
public class DefaultMessageHandler implements RequestHandler, ResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(DefaultMessageHandler.class);
    
    private DatabaseStatisticContainer databaseStats;
    
    protected final Context context;
    
    public DefaultMessageHandler(Context context) {
        this.context = context;
        
        databaseStats = context.getDatabaseStats();
    }
    
    public long timeout() {
        return Long.MAX_VALUE;
    }

    public boolean isCancelled() {
        return false;
    }

    public void handleResponse(ResponseMessage message, long time) throws IOException {
        addLiveContactInfo(message.getContact(), message);
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        context.getRouteTable().handleFailure(nodeId, dst);
    }

    public void handleRequest(RequestMessage message) throws IOException {
        addLiveContactInfo(message.getContact(), message);
    }
    
    public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        // never called
    }
    
    public void handleTick() {
        // never called
    }
    
    private synchronized void addLiveContactInfo(Contact node, DHTMessage message) throws IOException {
        
        if (node.isFirewalled()) {
            if (LOG.isInfoEnabled()) {
                LOG.info(node + " is firewalled");
            }
            return;
        }
        
        // NOTE: NetworkUtils.isPrivateAddress() is checking internally
        // if ConnectionSettings.LOCAL_IS_PRIVATE is true!
        if (NetworkUtils.isPrivateAddress(node.getContactAddress())) {
            if (LOG.isInfoEnabled()) {
                LOG.info(node + " has a private address");
            }
            return;
        }
        
        KUID nodeId = node.getNodeID();
        if (context.isLocalNodeID(nodeId)) {
            if (message instanceof PingResponse) {
            	// This is expected if there's a Node ID collision
                if (LOG.isInfoEnabled()) {
                    LOG.info("Looks like our NodeID collides with " + node);
            	}
            } else {
            	// This is unexpected. The MessageDispatcher should have
                // caught it!
                if (LOG.isErrorEnabled()) {
                    LOG.error("Received a " + message + " message from " + node 
                        + ". This message should have never gotten so far!");
            	}
            }
            
            return;
        }
        
        RouteTable routeTable = context.getRouteTable();
        
        // Only do store forward if it is a new node in our routing table 
        // (we are (re)connecting to the network) or a node that is reconnecting
        Contact existing = routeTable.get(nodeId);
        
        if (existing == null 
                || existing.getInstanceID() != node.getInstanceID()) {
            
            if (context.isBootstrapped()) {
                int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
                //we select the 2*k closest nodes in order to also check those values
                //where the local node is part of the k closest to the value but not part
                //of the k closest to the new joining node.
                List<Contact> nodes = routeTable.select(nodeId, 2*k, false);
                
                // Are we one of the K nearest Nodes to the contact?
                if (containsNodeID(nodes, context.getLocalNodeID())) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Node " + node + " is new or has changed his instanceID, will check for store forward!");   
                    }
                    
                    forwardOrRemoveValues(node, existing, message);
                }
            }
        }
        
        // Add the Node to our RouteTable or if it's
        // already there update its timeStamp and whatsoever
        routeTable.add(node);
    }
    
    private void forwardOrRemoveValues(Contact node, Contact existing, DHTMessage message) throws IOException {
        
        RouteTable routeTable = context.getRouteTable();
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        List<DHTValue> valuesToForward = new ArrayList<DHTValue>();
        
        Database database = context.getDatabase();
        synchronized(database) {
            for(KUID valueId : database.keySet()) {
                
                // To avoid redundant STORE forward, a node only transfers a value 
                // if it is the closest to the key or if its ID is closer than any 
                // other ID (except the new closest one of course)
                // TODO: maybe relax this a little bit: what if we're not the closest 
                // and the closest is stale?
                
                List<Contact> nodes = routeTable.select(valueId, k, false);
                Contact closest = nodes.get(0);
                Contact furthest = nodes.get(nodes.size()-1);
                
                //System.out.println(CollectionUtils.toString(nodes));
                //System.out.println("RT nearest: " + closest);
                //System.out.println("RT furthest: " + furthest);
                //System.out.println(context.getLocalNode());
                //System.out.println(node);
                //System.out.println(CollectionUtils.toString(nodes));
                //System.out.println();
                
                // We store forward if:
                // #1 We're the nearest Node of the k-closest Nodes to
                //    the given valueId
                //
                // #2 We're the second nearest of the k-closest Nodes to
                //    the given valueId AND the other Node is the nearest.
                //    In other words it changed its instance ID 'cause it
                //    was offile for a short period of time or whatsoever.
                //
                // The first condition applies if the Node is new
                // and we're the closest Node. The second condition
                // applies if the Node has changed it's instanceId.
                // That means we're the second closest and since
                // the other Node has changed its instanceId we must
                // re-send the values
                if (context.isLocalNode(closest)
                        || (node.equals(closest)
                                && nodes.size() > 1
                                && context.isLocalNode(nodes.get(1)))) {
                    
                    KUID nodeId = node.getNodeID();
                    KUID furthestId = furthest.getNodeID();
                    
                    // #3 The other Node must be equal to the furthest Node
                    //    or better
                    if (nodeId.equals(furthestId) 
                            || nodeId.isNearerTo(valueId, furthestId)) {
                        
                        //System.out.println("CONDITION B (FORWARD)");
                        //System.out.println("Local (from): " + context.getLocalNode());
                        //System.out.println("Remote (to): " + node);
                        //System.out.println(CollectionUtils.toString(nodes));
                        //System.out.println();
                        
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Node " + node + " is now close enough to a value and we are responsible for xfer");   
                        }
                        
                        databaseStats.STORE_FORWARD_COUNT.incrementStat();
                        valuesToForward.addAll(database.get(valueId).values());
                    }
                
                // We remove a value if:
                // #1 The value is stored at k Nodes 
                //    (i.e. the total number of Nodes in the DHT
                //     is equal or greater than k. If the DHT has
                //     less than k Nodes then there's no reason to
                //     remove a value)
                //
                // #2 This Node is the furthest of the k-closest Nodes
                //
                // #3 The new Node isn't in our RouteTable yet. That means
                //    adding it will push this Node out of the club of the
                //    k-closest Nodes and makes it the (k+1)-closest Node.
                //    
                // #4 The new Node is nearer to the given valueId then
                //    the furthest away Node (we).
                } else if (nodes.size() >= k 
                        && context.isLocalNode(furthest)
                        && (existing==null)
                        /*&& !containsNodeID(nodes, node.getNodeID())*/) {
                    
                    KUID nodeId = node.getNodeID();
                    KUID furthestId = furthest.getNodeID();
                        
                    if (nodeId.isNearerTo(valueId, furthestId)) {
                        //System.out.println("CONDITION C");
                        //System.out.println(valueId);
                        //System.out.println(context.getLocalNode());
                        //System.out.println(node);
                        //System.out.println(CollectionUtils.toString(nodes));
                        //System.out.println();
                        
                        boolean delete = DatabaseSettings.DELETE_VALUE_IF_FURTHEST_NODE.getValue();
                        
                        if (delete) {
                            int count = 0;
                            for (DHTValue value : database.get(valueId).values()) {
                                if (!value.isLocalValue()) {
                                    //System.out.println("REMOVING: " + value + "\n");
                                    
                                    database.remove(value);
                                    count++;
                                }
                            }
                            
                            databaseStats.STORE_FORWARD_REMOVALS.addData(count);
                        }
                    }
                }
            }
        }
        
        if (!valuesToForward.isEmpty()) {
            QueryKey queryKey = null;
            if (message instanceof FindNodeResponse) {
                queryKey = ((FindNodeResponse)message).getQueryKey();
                
                if (queryKey == null) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info(node + " sent us a null QueryKey");
                    }
                    return;
                }
            }
            
            context.store(node, queryKey, valuesToForward);
        }
    }
    
    /**
     * Returns whether or not the local Node is in the given List
     */
    private boolean containsNodeID(List<Contact> nodes, KUID id) {
        for (int i = nodes.size()-1; i >= 0; i--) {
            if (id.equals(nodes.get(i).getNodeID())) {
                return true;
            }
        }
        return false;
    }
}
