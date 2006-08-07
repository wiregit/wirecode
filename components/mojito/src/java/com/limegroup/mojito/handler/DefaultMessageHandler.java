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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.settings.NetworkSettings;
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
    
    public void addTime(long time) {
    }
    
    public long time() {
        return 0L;
    }
    
    public long timeout() {
        return NetworkSettings.TIMEOUT.getValue();
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
    
    private void addLiveContactInfo(Contact node, DHTMessage message) throws IOException {
        
        if (node.isFirewalled()) {
            return;
        }
        
        RouteTable routeTable = context.getRouteTable();
        
        // Only do store forward if it is a new node in our routing table 
        // (we are (re)connecting to the network) or a node that is reconnecting
        Contact existing = routeTable.get(node.getNodeID());
        
        // add node to the routing table -- update timestamp and info if needed
        routeTable.add(node);
        
        if (existing == null || existing.getInstanceID() != node.getInstanceID()) {
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Node " + node + " is new or has changed his instanceID, will check for store forward!");   
            }
            
            forward(node, message);
        }
    }
    
    private void forward(Contact node, DHTMessage message) throws IOException {
        
        RouteTable routeTable = context.getRouteTable();
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        List<Contact> nearestNodesToId = routeTable.select(node.getNodeID(), k, false);
        
        // Are we one of the K closest nodes to the contact?
        if (containsLocalNode(nearestNodesToId, context.getLocalNodeID())) {
            List<DHTValue> valuesToForward = new ArrayList<DHTValue>();
            
            Database database = context.getDatabase();
            synchronized(database) {
                for(KUID valueId : database.keySet()) {
                    
                    // To avoid redundant STORE forward, a node only transfers a value 
                    // if it is the closest to the key or if its ID is closer than any 
                    // other ID (except the new closest one of course)
                    // TODO: maybe relax this a little bit: what if we're not the closest 
                    // and the closest is stale?
                    
                    List<Contact> nearestNodesToKey = routeTable.select(valueId, k, false);
                    Contact closest = nearestNodesToKey.get(0);
                    
                    //System.out.println(context.getLocalNode());
                    //System.out.println(CollectionUtils.toString(nearestNodesToKey));
                    //System.out.println();
                    
                    if (context.isLocalNode(closest)   
                            || (node.equals(closest)
                                && (nearestNodesToKey.size() > 1)
                                && nearestNodesToKey.get(1).equals(context.getLocalNode()))) {
                        
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Node " + node + " is now close enough to a value and we are responsible for xfer");   
                        }
                        
                        databaseStats.STORE_FORWARD_COUNT.incrementStat();
                        valuesToForward.addAll(database.get(valueId).values());
                        
                    } else if (nearestNodesToKey.size() >= k
                            && !containsLocalNode(nearestNodesToKey, context.getLocalNodeID())) {
                        
                        boolean delete = DatabaseSettings.DELETE_VALUE_IF_FURTHEST_NODE.getValue();
                        
                        int count = 0;
                        for(Iterator<DHTValue> it = database.get(valueId).values().iterator(); it.hasNext(); ) {
                            DHTValue value = it.next();
                            if (!value.isLocalValue()) {
                                // Rather than to delete the DHTValue immediately we're
                                // setting the flag that it's no longer nearby which will 
                                // expire it faster. This way we can serve as a cache for
                                // a while...
                                
                                //System.out.println("\n" + value + "\n");
                                
                                if (delete) {
                                    it.remove();
                                } else {
                                    value.setNearby(false);
                                }
                                
                                count++;
                            }
                        }
                        
                        databaseStats.STORE_FORWARD_REMOVALS.addData(count);
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
    }
    
    /**
     * Returns whether or not the local Node is in the given List
     */
    private boolean containsLocalNode(List<Contact> nodes, KUID id) {
        for (int i = nodes.size()-1; i >= 0; i--) {
            if (id.equals(nodes.get(i).getNodeID())) {
                return true;
            }
        }
        return false;
    }
}
