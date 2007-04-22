/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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
 
package org.limewire.mojito.handler;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.PingResponse;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.messages.SecurityTokenProvider;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTable.SelectMode;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.statistics.DatabaseStatisticContainer;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.security.SecurityToken;
import org.limewire.service.ErrorService;


/**
 * The DefaultMessageHandler performs basic Kademlia RouteTable 
 * update operations. That means adding new Nodes if RouteTable 
 * is not full, updating the last seen time stamp of Nodes and 
 * so forth.
 */
public class DefaultMessageHandler {
    
    private static final Log LOG = LogFactory.getLog(DefaultMessageHandler.class);
    
    private static enum Operation {
        // Do nothing
        NOTHING,
        
        // Forward value
        FORWARD,
        
        // Delete value
        DELETE;
    }
    
    private DatabaseStatisticContainer databaseStats;
    
    protected final Context context;
    
    public DefaultMessageHandler(Context context) {
        this.context = context;
        
        databaseStats = context.getDatabaseStats();
    }
    
    public void handleResponse(ResponseMessage message, long time) {
        addLiveContactInfo(message.getContact(), message);
    }

    public void handleLateResponse(ResponseMessage message) {
        Contact node = message.getContact();
        
        if (!node.isFirewalled()) {
            context.getRouteTable().add(node); // update
        }
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) {
        context.getRouteTable().handleFailure(nodeId, dst);
    }

    public void handleRequest(RequestMessage message) {
        addLiveContactInfo(message.getContact(), message);
    }
    
    /**
     * Adds the given Contact or updates it if it's already in our RouteTable
     */
    private synchronized void addLiveContactInfo(Contact node, DHTMessage message) {
        
        RouteTable routeTable = context.getRouteTable();
        
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
        if (context.isLocalNodeID(nodeId)) {
            if (message instanceof PingResponse) {
            	// This is expected if there's a Node ID collision
                if (LOG.isInfoEnabled()) {
                    LOG.info("Looks like our NodeID collides with " + node);
            	}
            } else {
                // This is unexpected. The MessageDispatcher should have
                // caught it!
                
                String msg = "Received a " + message + " message from " + node 
                    + ". This message should have never gotten so far!";
                
                if (LOG.isErrorEnabled()) {
                    LOG.error(msg);
            	}
                
                ErrorService.error(new IllegalArgumentException(msg));
            }
            
            return;
        }
        
        if (KademliaSettings.STORE_FORWARD_ENABLED.getValue()) {
            // Only do store forward if it is a new node in our routing table 
            // (we are (re)connecting to the network) or a node that is reconnecting
            Contact existing = routeTable.get(nodeId);
            
            if (existing == null
                    || existing.isUnknown()
                    || existing.getInstanceID() != node.getInstanceID()) {
                
                // Store forward only if we're bootstrapped
                if (context.isBootstrapped()) {
                    int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
                    //we select the 2*k closest nodes in order to also check those values
                    //where the local node is part of the k closest to the value but not part
                    //of the k closest to the new joining node.
                    List<Contact> nodes = routeTable.select(nodeId, 2*k, SelectMode.ALL);
                    
                    // Are we one of the K nearest Nodes to the contact?
                    if (containsNodeID(nodes, context.getLocalNodeID())) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Node " + node + " is new or has changed his instanceID, will check for store forward!");   
                        }
                        
                        forwardOrRemoveValues(node, existing, message);
                    }
                }
            }
        }
        
        // Add the Node to our RouteTable or if it's
        // already there update its timeStamp and whatsoever
        routeTable.add(node);
    }
    
    /**
     * This method depends on addLiveContactInfo(...) and does two things.
     * It either forwards or removes a DHTValue it from the local Database.
     * For details see Kademlia spec!
     */
    private void forwardOrRemoveValues(Contact node, Contact existing, DHTMessage message) {
        
        List<DHTValueEntity> valuesToForward = new ArrayList<DHTValueEntity>();
        
        Database database = context.getDatabase();
        synchronized(database) {
            for(KUID valueId : database.keySet()) {
                
                Operation op = getOperation(node, existing, valueId);
                
                if (op.equals(Operation.FORWARD)) {
                    Map<KUID, DHTValueEntity> bag = database.get(valueId);
                    valuesToForward.addAll(bag.values());
                    databaseStats.STORE_FORWARD_COUNT.incrementStat();
                    
                } else if (op.equals(Operation.DELETE)
                        && DatabaseSettings.DELETE_VALUE_IF_FURTHEST_NODE.getValue()) {
                    Map<KUID, DHTValueEntity> bag = database.get(valueId);
                    for (DHTValueEntity entity : bag.values()) {
                        if (!entity.isLocalValue()) {
                            //System.out.println("REMOVING: " + entity + "\n");
                            database.remove(entity.getKey(), entity.getSecondaryKey());
                        }
                    }
                    databaseStats.STORE_FORWARD_REMOVALS.incrementStat();
                }
            }
        }
        
        if (!valuesToForward.isEmpty()) {
            SecurityToken securityToken = null;
            if (message instanceof SecurityTokenProvider) {
                securityToken = ((SecurityTokenProvider)message).getSecurityToken();
                
                if (securityToken == null
                        && KademliaSettings.STORE_REQUIRES_SECURITY_TOKEN.getValue()) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info(node + " sent us a null SecurityToken");
                    }
                    return;
                }
            }
            
            context.store(node, securityToken, valuesToForward);
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
    
    /**
     * 
     */
    private Operation getOperation(Contact node, Contact existing, KUID valueId) {
        // To avoid redundant STORE forward, a node only transfers a value 
        // if it is the closest to the key or if its ID is closer than any 
        // other ID (except the new closest one of course)
        // TODO: maybe relax this a little bit: what if we're not the closest 
        // and the closest is stale?
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        RouteTable routeTable = context.getRouteTable();
        List<Contact> nodes = routeTable.select(valueId, k, SelectMode.ALIVE_WITH_LOCAL);
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
        //    was offline for a short period of time or whatsoever.
        //    (see also pre-condition(s) from where we're calling
        //    this method)
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
                
                return Operation.FORWARD;
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
                && (existing == null || existing.isUnknown())) {
            
            KUID nodeId = node.getNodeID();
            KUID furthestId = furthest.getNodeID();
                
            if (nodeId.isNearerTo(valueId, furthestId)) {
                //System.out.println("CONDITION C");
                //System.out.println(valueId);
                //System.out.println(context.getLocalNode());
                //System.out.println(node);
                //System.out.println(CollectionUtils.toString(nodes));
                //System.out.println();
                
                return Operation.DELETE;
            }
        }
        
        return Operation.NOTHING;
    }
}
