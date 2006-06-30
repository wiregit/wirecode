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

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;


/**
 * RouteTable interface that all LimeDHT route table implementations
 * must implement.
 */
public interface RouteTable {
    
    /**
     * Adds a new Contact or if it's already in the RouteTable updates 
     * its contact information.
     * 
     * @param node the Contact we would like to add
     */
    public void add(Contact node);
    
    /**
     * Returns a Contact from the local RoutingTable if such Contact exists 
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
     * @param count the number of Contact (maybe less if RoutingTable has less than 'count' entries!)
     * @param liveContacts wheather or not only live Contacts should be in the result set
     * @param willContact wheather or not we'll contact these Contacts
     * @return list of Contacts sorted by closeness
     */
    public List<Contact> select(KUID nodeId, int count, boolean liveContacts, boolean willContact);
    
    /**
     * Notifies the RoutingTable that the Contact with the provided
     * KUID has failed to answert to a request.
     */
    public void handleFailure(KUID nodeId, SocketAddress address);
    
    /**
     * Returns all Contacts as List
     */
    public List<Contact> getContacts();
    
    /**
     * Returns Contacts that are actively used for routing
     */
    public List<Contact> getLiveContacts();
    
    /**
     * Returns cached Contacts that are in the replacement cache
     */
    public List<Contact> getCachedContacts();
    
    /**
     * Returns a List of KUIDs that need to be looked up in order
     * to refresh the RouteTable.
     * 
     * @param force whether or not the refresh is forced 
     */
    public List<KUID> getRefreshIDs(boolean force);
    
    /**
     * Returns whether or not the local Contact (i.e. we)
     * are close to the provided KUID. In other words if
     * the provided KUID is (hypothetically) in the same 
     * Bucket in the RouteTable as the local Contact is.
     */
    public boolean isNearToLocal(KUID nodeId);
    
    /**
     * Clears all elements from the RoutingTable
     */
    public void clear();
}
