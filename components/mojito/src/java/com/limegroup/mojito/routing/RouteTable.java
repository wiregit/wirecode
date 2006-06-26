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

import java.util.List;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.PingListener;


/**
 * RouteTable interface that all LimeDHT route table implementations
 * must implement.
 */
public interface RouteTable {
    
    /**
     * Adds a new ContactNode or if it's already known updates its
     * contact information.
     * 
     * @param node the ContactNode we would like to add
     * @param knownToBeAlive wheather or not this ContactNode is known to be alive
     * @return true if ContactNode was added
     */
    public void add(Contact node);
    
    /**
     * Returns a Contact from the local RoutingTable if such Node exists 
     * and null if it doesn't.
     */
    public Contact get(KUID nodeId);
    
    /**
     * Selects the best matching Contact for the provided KUID.
     * This method will gueanteed return a non-null value if the
     * RoutingTable is not empty.
     */
    public Contact select(KUID nodeId);
    
    /**
     * Selects the best matching k ContactNodes for the provided KUID. The returned
     * Contacts are sorted by their closeness to the lookup Key from closest to
     * least closest Contact. Use {@link com.limegroup.mojito.util.BucketUtils#sort(List)}
     * to sort the list from least-recently-seen to most-recently-seen ContactNode.
     * 
     * @param nodeId the lookup KUID
     * @param count the number of Contact (maybe less if RoutingTable has less than k entries!)
     * @param liveNodes wheather or not only live nodes should be in the result set
     * @param willContact wheather or not we'll contact these ContactNodes
     * @return list of ContactNodes sorted by closeness
     */
    public List<? extends Contact> select(KUID nodeId, int count, boolean liveNodes, boolean willContact);
    
    /**
     * Notifies the RoutingTable that the Contact with the provided
     * KUID has failed to a request
     */
    public void handleFailure(KUID nodeId);
    
    /**
     * Returns all Contacts as List
     */
    public List<? extends Contact> getNodes();
    
    /**
     * Returns Contacts that are actively used for routing
     */
    public List<? extends Contact> getLiveNodes();
    
    /**
     * Returns cached Contacts that are in the replacement cache
     */
    public List<? extends Contact> getCachedNodes();
    
    /**
     * 
     */
    public List<KUID> getRefreshIDs(boolean force);
    
    /**
     * Clears all elements from the RoutingTable
     */
    public void clear();
    
    /**
     * 
     */
    public static interface SpoofChecker extends PingListener {
        
    }
}
