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
 
package com.limegroup.mojito.routing;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;

import com.limegroup.mojito.BucketNode;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.handler.BootstrapManager;


/**
 * RouteTable interface that all LimeDHT route table implementations
 * must implement.
 */
public interface RouteTable {
    
    /**
     * Clears all elements from the RoutingTable
     */
    public void clear();
    
    /**
     * Returns true if the RoutingTable is empty (initial state)
     */
    public boolean isEmpty();
    
    /**
     * Returns the number of ContactNodes.
     */
    public int size();
    
    /**
     * Returns the number of Buckets
     */
    public int getBucketCount();
    
    /**
     * Adds a new ContactNode or if it's already known updates its
     * contact information.
     * 
     * @param node the ContactNode we would like to add
     * @param knownToBeAlive wheather or not this ContactNode is known to be alive
     * @return true if ContactNode was added
     */
    public boolean add(ContactNode node, boolean knownToBeAlive);
    
    /**
     * Returns a ContactNode from the local RoutingTable if such Node exists 
     * and null if it doesn't.
     */
    public ContactNode get(KUID nodeId);
    
    /**
     * Returns a ContactNode from the local RoutingTable if such Node exists 
     * and null if it doesn't.
     */
    public ContactNode get(KUID nodeId, boolean checkAndUpdateCache);
    
    /**
     * Selects the best matching ContactNode for the provided KUID.
     * This method will gueanteed return a non-null value if the
     * RoutingTable is not empty.
     */
    public ContactNode select(KUID lookup);
    
    /**
     * Selects the best matching k ContactNodes for the provided KUID. The returned
     * ContactNodes are sorted by their closeness to the lookup Key from closest to
     * least closest ContactNode. Use {@link com.limegroup.mojito.util.BucketUtils#sort(List)}
     * to sort the list from least-recently-seen to most-recently-seen ContactNode.
     * 
     * @param lookup the lookup KUID
     * @param k the number of ContactNodes (maybe less if RoutingTable has less than k entries!)
     * @param onlyLiveNodes wheather or not only live nodes should be in the result set
     * @param willContact wheather or not we'll contact these ContactNodes
     * @return list of ContactNodes sorted by closeness
     */
    public List<ContactNode> select(KUID lookup, int k, boolean onlyLiveNodes, boolean willContact);
    
    /**
     * Returns true if the RoutingTable contains a ContactNode with this KUID.
     */
    public boolean containsNode(KUID nodeId);
    
    /**
     * Notifies the RoutingTable that the ContactNode with the provided
     * KUID has failed to a request
     */
    public void handleFailure(KUID nodeId, SocketAddress address);
    
    /**
     * Returns all ContactNodes as List
     */
    public List<ContactNode> getAllNodes();
    
    /**
     * Returns all ContactNodes ordered by most recently seen first as List
     */
    public List<ContactNode> getAllNodesMRS();
    
    /**
     * Returns ContactNodes ordered by most recently seen first as List
     * 
     * @param numNodes
     */
    public List<ContactNode> getMRSNodes(int numNodes);
    
    /**
     * Returns all BucketNodes as List
     */
    public List<BucketNode> getAllBuckets();
    
    /**
     * Refreshes the routing table's buckets
     * 
     * @param force true to refresh all buckets, false otherwise
     * @param manager the BootstrapManager callback
     * @throws IOException
     */
    public void refreshBuckets(boolean force, BootstrapManager manager) throws IOException;
    
    /**
     * 
     */
    public static interface SpoofChecker extends PingListener {
        
    }
}
